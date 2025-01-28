package com.arny.mobilebert.data.ai.analyse

import android.content.Context
import android.util.Log
import com.arny.mobilebert.data.ai.attention.BertAttentionExtractor
import com.arny.mobilebert.data.ai.models.ImportantWord
import com.arny.mobilebert.data.ai.models.ModelTensorsInfo
import com.arny.mobilebert.data.ai.models.TensorDetails
import com.arny.mobilebert.data.ai.models.TextAnalysis
import com.arny.mobilebert.data.ai.tokenizers.BertTokenizerMultiLang
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITextAnalyzer
import com.arny.mobilebert.domain.ai.ITokenizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter
import java.nio.channels.FileChannel
import javax.inject.Inject
import kotlin.math.sqrt

class BertAnalyzerMultiLang @Inject constructor(
    private val context: Context,
    private val mobilFileManager: ModelFileManager
) : ITextAnalyzer {
    private lateinit var tokenizer: ITokenizer
    private lateinit var attentionExtractor: BertAttentionExtractor
    private var interpreter: Interpreter? = null
    private val config = ModelConfig.BERT_MULTILINGUAL
    private val interpreterLock = Mutex()
    private var modelTensorsInfo: ModelTensorsInfo = ModelTensorsInfo(emptyList(), emptyList())

    override fun getTensorsInfo(): ModelTensorsInfo = modelTensorsInfo

    // Добавляем флаг для отслеживания повторного использования
    private var _isReused = false
    override val isReused: Boolean
        get() = _isReused

    override fun initialize() {
        if (interpreter != null) {
            _isReused = true
            return
        }
        try {
            val options = Interpreter.Options().apply {
                numThreads = 4
                useXNNPACK = true
            }
            Log.i(BertAnalyzerMultiLang::class.java.simpleName, "initializeModel: ${config.modelName}");
            val openFd = context.assets.openFd(config.modelPath)

            openFd.use { fileDescriptor ->
                fileDescriptor.createInputStream().use { inputStream ->
                    inputStream.channel.use { fileChannel ->
                        val startOffset = fileDescriptor.startOffset
                        val declaredLength = fileDescriptor.declaredLength
                        val modelBuffer = fileChannel.map(
                            FileChannel.MapMode.READ_ONLY,
                            startOffset,
                            declaredLength
                        )
                        interpreter = Interpreter(modelBuffer, options)
                        attentionExtractor = BertAttentionExtractor()
                        // Проверка тензоров
                        logTensorDetails()
                        // загрузка токенов
                        tokenizer = BertTokenizerMultiLang(  config = config, mobilFileManager)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ModelManager", "Error loading model: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun logTensorDetails() {
        val inputTensors = mutableListOf<TensorDetails>()
        val outputTensors = mutableListOf<TensorDetails>()
        interpreter?.let {
            // Входные тензоры
            for (i in 0 until it.inputTensorCount) {
                val tensor = it.getInputTensor(i)
//                println(
//                    "Input tensor name: ${tensor.name()}, shape: ${
//                        tensor.shape().contentToString()
//                    }, dataType: ${tensor.dataType()}"
//                )
                inputTensors.add(TensorDetails(
                    name = tensor.name(),
                    shape = tensor.shape().toList(),
                    dataType = tensor.dataType().toString()
                ))
            }

            // Выходные тензоры
            for (i in 0 until it.outputTensorCount) {
                val tensor = it.getOutputTensor(i)
                println(
                    "Output tensor name: ${tensor.name()}, shape: ${
                        tensor.shape().contentToString()
                    }, dataType: ${tensor.dataType()}"
                )
                outputTensors.add(TensorDetails(
                    name = tensor.name(),
                    shape = tensor.shape().toList(),
                    dataType = tensor.dataType().toString()
                ))
            }
        }
        modelTensorsInfo = ModelTensorsInfo(inputTensors, outputTensors)
    }


    override suspend fun analyzeText(text: String): TextAnalysis = interpreterLock.withLock {
        require(text.isNotBlank())
//        logTensorShapes() // Добавить эту строку
//        Log.d("BertAnalyzerMultiLang", "analyzeText: $text")
        val tokenizeResult = tokenizer.tokenize(text)

        // Обрабатываем каждое слово отдельно
        val wordScores = tokenizeResult.tokens.mapIndexed { index, token ->
            val inputs = mapOf(
                "int64_serving_input_ids" to Array(1) { LongArray(1) { tokenizeResult.inputIds[index] } },
                "int64_serving_attention_mask" to Array(1) { LongArray(1) { 1L } },
                "int64_serving_token_type_ids" to Array(1) { LongArray(1) { 0L } }
            )

            val outputs = runInference(inputs)

            // Получаем sequence output [1, 1, 768]
            val sequenceOutput = outputs[0] as? Array<*>
            // Получаем векторное представление слова
            val wordVector = (sequenceOutput?.get(0) as? Array<*>)?.get(0) as? FloatArray

            // Вычисляем важность слова на основе его вектора
            calculateWordImportance(wordVector, token)
        }

//        Log.d("BertAnalyzerMultiLang", "Raw word scores: ${wordScores.joinToString()}")

        // Нормализуем scores с учетом разброса значений
        val normalizedScores = normalizeScores(wordScores)
//        Log.d("BertAnalyzerMultiLang", "Normalized scores: ${normalizedScores.joinToString()}")

        val importantWords = tokenizeResult.tokens.zip(normalizedScores)
            .map { (token, score) ->
                ImportantWord(token, score)
            }
            .sortedByDescending { it.weight }
            .filter { it.weight > 0.1f }

        return TextAnalysis(
            tokens = tokenizeResult.tokens,
            importantWords = importantWords,
            attentionScore = normalizedScores.average()
        )
    }

    override fun getTokenizer(): ITokenizer = tokenizer

    override fun getModelName(): String = config.modelName

    private fun calculateWordImportance(wordVector: FloatArray?, token: String): Float {
        if (wordVector == null) return 0f

        // Базовые метрики
        val magnitude = sqrt(wordVector.map { it * it }.sum())
        val meanActivation = wordVector.average().toFloat()
        val maxActivation = wordVector.maxOrNull() ?: 0f

        // Факторы важности слова
        val lengthFactor = when {
            token.length <= 2 -> 0.4f  // Сильнее понижаем короткие слова
            token.length <= 3 -> 0.6f  // Умеренно понижаем трехбуквенные слова
            token.length >= 8 -> 1.3f  // Повышаем длинные слова
            token.length >= 6 -> 1.1f  // Слегка повышаем слова средней длины
            else -> 1.0f
        }

        // Фактор типа слова
        val wordTypeFactor = when {
            // Служебные слова
            token.lowercase() in setOf(
                "и",
                "или",
                "но",
                "а",
                "в",
                "на",
                "с",
                "со",
                "за",
                "под",
                "над",
                "к",
                "у"
            ) -> 0.2f
            // Сокращения и единицы измерения
            token.lowercase() in setOf("км", "м", "кг", "см", "мм", "г", "кв", "куб") -> 0.5f
            // Числа
            token.all { it.isDigit() } -> 0.7f
            // Специальные слова (можно дополнить)
            token.lowercase() in setOf("марки", "номер", "модель") -> 0.8f

            else -> 1.0f
        }

        // Комбинируем метрики с разными весами
        return (magnitude * 0.5f +
                meanActivation * 0.3f +
                maxActivation * 0.2f) * lengthFactor * wordTypeFactor
    }

    private fun normalizeScores(scores: List<Float>): List<Float> {
        if (scores.isEmpty()) return emptyList()

        val min = scores.minOrNull() ?: return scores
        val max = scores.maxOrNull() ?: return scores
        val range = max - min

        return if (range > 0.001f) {
            scores.map { score ->
                // Используем нелинейную нормализацию для лучшего распределения весов
                val normalized = ((score - min) / range)
                (normalized * normalized).coerceIn(0f, 1f)
            }
        } else {
            scores.map { 0.5f }
        }
    }

    private fun runInference(inputs: Map<String, Any>): Map<Int, Any> {
        val outputs = mutableMapOf<Int, Any>()

        interpreter?.let {
            // Создаем выходные буферы с правильной размерностью
            outputs[0] = Array(1) { Array(1) { FloatArray(768) } }  // [1, 1, 768]
            outputs[1] = Array(1) { FloatArray(768) }               // [1, 768]

            it.runForMultipleInputsOutputs(
                inputs.values.toTypedArray(),
                outputs
            )
        }

        return outputs
    }

    override fun close() {
        interpreter?.close()
    }
}
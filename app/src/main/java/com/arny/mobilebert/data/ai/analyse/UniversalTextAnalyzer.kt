package com.arny.mobilebert.data.ai.analyse

import android.util.Log
import android.util.LruCache
import com.arny.mobilebert.data.ai.models.ImportantWord
import com.arny.mobilebert.data.ai.models.ModelTensorsInfo
import com.arny.mobilebert.data.ai.models.TensorDetails
import com.arny.mobilebert.data.ai.models.TextAnalysis
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITextAnalyzer
import com.arny.mobilebert.domain.ai.ITokenizer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import javax.inject.Inject
import kotlin.math.sqrt

// Универсальный анализатор текста
class UniversalTextAnalyzer @Inject constructor(
    private val config: ModelConfig,
    private val tokenizer: ITokenizer,
    private val modelFileManager: ModelFileManager,
) : ITextAnalyzer {
    private var interpreter: Interpreter? = null
    private val interpreterLock = Mutex()
    private val resultsCache = LruCache<String, TextAnalysis>(100)
    private var lastMemoryUsage = 0L
    private var modelTensorsInfo: ModelTensorsInfo = ModelTensorsInfo(emptyList(), emptyList())
    // Добавляем флаг для отслеживания повторного использования
    private var _isReused = false
    override val isReused: Boolean
        get() = _isReused

    override fun getTensorsInfo(): ModelTensorsInfo = modelTensorsInfo

    override fun initialize() {
        if (interpreter != null) {
            _isReused = true
            return
        }
        Log.i("UniversalTextAnalyzer", "initialize model: ${config.modelPath}")
        val options = Interpreter.Options().apply {
            numThreads = 4
            useXNNPACK = true
        }

        // Загрузка модели
        interpreter = modelFileManager.getModelFile(config).let { modelFile ->
            FileInputStream(modelFile).channel.use { fileChannel ->
                val modelBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    modelFile.length()
                )
                Interpreter(modelBuffer, options)
            }
        }
        logTensorDetails()
    }

    private fun logTensorDetails() {
        val inputTensors = mutableListOf<TensorDetails>()
        val outputTensors = mutableListOf<TensorDetails>()
        interpreter?.let { interpreter ->
            for (i in 0 until interpreter.inputTensorCount) {
                val tensor = interpreter.getInputTensor(i)
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

            for (i in 0 until interpreter.outputTensorCount) {
                val tensor = interpreter.getOutputTensor(i)
//                println(
//                    "Output tensor name: ${tensor.name()}, shape: ${
//                        tensor.shape().contentToString()
//                    }, dataType: ${tensor.dataType()}"
//                )
                outputTensors.add(TensorDetails(
                    name = tensor.name(),
                    shape = tensor.shape().toList(),
                    dataType = tensor.dataType().toString()
                ))
            }
        }
        modelTensorsInfo = ModelTensorsInfo(inputTensors, outputTensors)
    }

    private fun logProcessingDetails(
        tokenizeResult: TokenizeResult,
        startTime: Long,
        endTime: Long
    ) {
        Log.d(
            "Analyzer", """
        Processing details:
        Text tokens: ${tokenizeResult.tokens.size}
        Processing time: ${endTime - startTime}ms
        Memory delta: ${updateMemoryUsage() / 1024}KB
        Tokens: ${tokenizeResult.tokens.joinToString()}
    """.trimIndent()
        )
    }

    override suspend fun analyzeText(text: String) = runBlocking {
        interpreterLock.withLock {
            // Проверяем кэш
            resultsCache.get(text)?.let { return@runBlocking it }
            val tokenizeResult = tokenizer.tokenize(text)
            val startTime = System.currentTimeMillis()
            val result = processText(tokenizeResult)
            val endTime = System.currentTimeMillis()

            logProcessingDetails(tokenizeResult, startTime, endTime)
            // Кэшируем результат
            resultsCache.put(text, result)
            result
        }
    }

    private fun processText(tokenizeResult: TokenizeResult): TextAnalysis {
        val tokens = tokenizeResult.tokens

        val inputs = createModelInputs(tokenizeResult)
        val outputs = createModelOutputs(tokenizeResult.inputIds.size)

        interpreter?.runForMultipleInputsOutputs(inputs, outputs)

        val attentionScores = (outputs[0] as Array<*>)[0] as Array<*>
        val sequenceEmbeddings = attentionScores.map { it as FloatArray }

        val scores = calculateEnhancedScores(sequenceEmbeddings, tokens)

        val importantWords = tokens.zip(scores)
            .filter { (token, _) -> token !in setOf("[CLS]", "[SEP]") }
            .map { (token, score) -> ImportantWord(token, score) }
            .filter { it.weight > 0.3f }
            .sortedByDescending { it.weight }
            .also { words ->
                Log.d("Analyzer", "Important words found: ${
                    words.joinToString { "${it.token}(${String.format("%.2f", it.weight)})" }
                }")
            }

        return TextAnalysis(
            tokens = tokens,
            importantWords = importantWords,
            attentionScore = scores.average()
        )
    }

    private fun calculateEnhancedScores(
        sequenceEmbeddings: List<FloatArray>,
        tokens: List<String>
    ): List<Float> {
        // Игнорируем специальные токены
        if (tokens.size != sequenceEmbeddings.size) {
            throw IllegalArgumentException("Tokens and embeddings size mismatch")
        }

        // Рассчитываем базовые статистики для нормализации
        val embeddingStats = sequenceEmbeddings
            .filter { tokens[sequenceEmbeddings.indexOf(it)] !in setOf("[CLS]", "[SEP]") }
            .map { embedding ->
                val magnitude = sqrt(embedding.map { it * it }.sum())
                val mean = embedding.average()
                val std = sqrt(embedding.map { (it - mean) * (it - mean) }.average())
                Triple(magnitude, mean, std)
            }

        val (maxMagnitude, maxMean, maxStd) = embeddingStats.fold(
            Triple(0f, 0.0, 0.0)
        ) { acc, (mag, mean, std) ->
            Triple(
                maxOf(acc.first, mag.toFloat()),
                maxOf(acc.second, mean),
                maxOf(acc.third, std)
            )
        }

        return tokens.mapIndexed { index, token ->
            when (token) {
                "[CLS]", "[SEP]" -> 0.1f
                else -> {
                    val embedding = sequenceEmbeddings[index]

                    // Базовые метрики
                    val magnitude = sqrt(embedding.map { it * it }.sum())
                    val mean = embedding.average()
                    val std = sqrt(embedding.map { (it - mean) * (it - mean) }.average())

                    // Нормализованные компоненты
                    val magnitudeScore = (magnitude / maxMagnitude).toFloat()
                    val varianceScore = (std / maxStd).toFloat()

                    // Позиционный фактор
                    val positionFactor = when (index) {
                        1, tokens.size - 2 -> 1.1f  // Первое и последнее значимые слова
                        in 2..3 -> 1.05f           // Начало предложения
                        else -> 1.0f
                    }

                    // Длина слова (предполагаем, что длинные слова более значимы)
                    val lengthFactor = minOf(token.length / 10f, 1f) * 0.1f + 0.9f

                    // Характеристики слова
                    val wordCharacteristics = when {
                        token.all { it.isUpperCase() } -> 1.1f  // Аббревиатуры
                        token.first().isUpperCase() -> 1.05f    // Имена собственные
                        token.contains(Regex("[0-9]")) -> 0.95f // Числа
                        else -> 1.0f
                    }

                    // Комбинируем все факторы
                    val score = (
                            magnitudeScore * 0.4f +
                                    varianceScore * 0.3f +
                                    lengthFactor * 0.3f
                            ) * positionFactor * wordCharacteristics

                    // Нормализуем финальный скор
                    score.coerceIn(0.3f, 0.9f)
                }
            }
        }
    }

    private fun createModelInputs(tokenizeResult: TokenizeResult): Array<Any> {
        val sequenceLength = tokenizeResult.inputIds.size
        return arrayOf(
            Array(1) { tokenizeResult.inputIds },
            Array(1) { LongArray(sequenceLength) { 1L } },
            Array(1) { LongArray(sequenceLength) { 0L } }
        )
    }

    private fun createModelOutputs(sequenceLength: Int): MutableMap<Int, Any> {
        val hiddenSize = config.outputShape[1]
        return mutableMapOf<Int, Any>().apply {
            // [batch_size, sequence_length, hidden_size]
            this[0] = Array(1) { Array(sequenceLength) { FloatArray(hiddenSize) } }
            // [batch_size, hidden_size]
            this[1] = Array(1) { FloatArray(hiddenSize) }
        }
    }

    private fun updateMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val delta = usedMemory - lastMemoryUsage
        lastMemoryUsage = usedMemory
        return delta
    }

    override fun getModelName(): String {
        return config.modelName
    }

    override fun getTokenizer(): ITokenizer {
        return tokenizer
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        resultsCache.evictAll()
    }
}
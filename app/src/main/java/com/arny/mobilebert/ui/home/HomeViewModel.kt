package com.arny.mobilebert.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.ComparisonMetrics
import com.arny.mobilebert.data.ai.models.ComparisonResult
import com.arny.mobilebert.data.ai.models.LanguageMetrics
import com.arny.mobilebert.data.ai.models.MemoryMetrics
import com.arny.mobilebert.data.ai.models.ModelComparison
import com.arny.mobilebert.data.ai.models.ModelInfo
import com.arny.mobilebert.data.ai.models.ModelMetrics
import com.arny.mobilebert.data.ai.models.PerformanceMetrics
import com.arny.mobilebert.data.ai.models.TestCategory
import com.arny.mobilebert.data.ai.models.TestProgress
import com.arny.mobilebert.data.ai.models.TestResultCompare
import com.arny.mobilebert.data.ai.models.TextAnalysis
import com.arny.mobilebert.data.ai.test.ModelComparisonManager
import com.arny.mobilebert.data.search.SearchResult
import com.arny.mobilebert.data.search.SmartSearchManager
import com.arny.mobilebert.data.utils.AndroidAssetManager
import com.arny.mobilebert.domain.ai.IModelFileManager
import com.arny.mobilebert.domain.ai.ITestManager
import com.arny.mobilebert.domain.ai.ITextAnalyzer
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class HomeViewModel @AssistedInject constructor(
    private val smartSearchManager: SmartSearchManager,
    private val textAnalyzer: ITextAnalyzer,
    private val comparisonManager: ModelComparisonManager,
    private val testManager: ITestManager,
    private val fileManager: IModelFileManager,
    private val androidAssetManager: AndroidAssetManager
) : ViewModel() {

    init {
        loadContent()
    }

    private val _state = MutableStateFlow<HomeUIState>(HomeUIState.Idle)
    val state: StateFlow<HomeUIState> = _state.asStateFlow()

    private var modelJob: Job? = null

    private var currentModelConfig: ModelConfig? = null

    private val _resultText = MutableStateFlow("")
    val resultText = _resultText.asStateFlow()

    private val _uiEnabled = MutableStateFlow(true)
    val uiEnabled = _uiEnabled.asStateFlow()

    fun performSearch(query: String) {
        viewModelScope.launch {
            flow { emit(smartSearchManager.search(query, textAnalyzer)) }
                .flowOn(Dispatchers.IO)
                .catch {
                    it.printStackTrace()
                    val resultText = buildString {
                        appendLine("Исходный текст: $query")
                        appendLine("\nОшибка: ${it.message}")
                    }
                    _resultText.value = resultText
                }
                .collectLatest { results ->
                    displayResults(results)
                }
        }
    }

    fun analyzeText(text: String) {
        viewModelScope.launch {
            _uiEnabled.value = false
            flow {
                emit(textAnalyzer.analyzeText(text))
            }
                .flowOn(Dispatchers.IO)
                .catch {
                    it.printStackTrace()
                    val resultText = buildString {
                        appendLine("Исходный текст: $text")
                        appendLine("\nОшибка: ${it.message}")
                    }
                    _resultText.value = resultText
                    _uiEnabled.value = true
                }
                .collectLatest { analysis ->
                    updateUI(analysis, text)
                    _uiEnabled.value = true
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textAnalyzer.close()
        modelJob?.cancel()
    }

    fun startImport(modelConfig: ModelConfig) {
        currentModelConfig = modelConfig
    }

    fun importModelFile(uri: Uri) {
        viewModelScope.launch {
            try {
                currentModelConfig?.let { config ->
                    _state.value = HomeUIState.Importing(false, config)
                    androidAssetManager.importModelFile(uri, config)
                    _state.value = HomeUIState.SelectingVocab
                }
            } catch (e: Exception) {
                _state.value = HomeUIState.Error("Failed to import model file: ${e.message}")
            }
        }
    }

    fun importVocabFile(uri: Uri) {
        viewModelScope.launch {
            try {
                currentModelConfig?.let { config ->
                    _state.value = HomeUIState.Importing(true,config)
                    androidAssetManager.importVocabFile(uri, config)
                    if (fileManager.isModelComplete(config)) {
                        val modelInfo = getModelInfo(config)
                        _state.value = HomeUIState.Imported(modelInfo)
                    } else {
                        _state.value = HomeUIState.Error("Import incomplete")
                    }
                }
            } catch (e: Exception) {
                _state.value = HomeUIState.Error("Failed to import vocab file: ${e.message}")
            }
        }
    }

    private fun getModelInfo(modelConfig: ModelConfig): ModelInfo? {
        if (!fileManager.isModelDownloaded(modelConfig)) return null

        val (modelSize, vocabSize) = fileManager.getModelSizeFull(modelConfig)
        return ModelInfo(
            name = modelConfig.modelName,
            modelSize = modelSize,
            vocabSize = vocabSize,
            outputShape = modelConfig.outputShape
        )
    }

    fun testModel(modelConfig: ModelConfig) {
        modelJob?.cancel()
        modelJob = viewModelScope.launch {
            _uiEnabled.value = false
            testManager.testTokenization(modelConfig)
                .flowOn(Dispatchers.IO)
                .catch {
                    it.printStackTrace()
                    val resultText = buildString {
                        appendLine("Ошибка: ${it.message}")
                    }
                    _resultText.value = resultText
                    _uiEnabled.value = true
                }
                .collectLatest { progress ->
                    when (progress) {
                        is TestProgress.Started -> {
                            val text = "TestState.Started, ${progress.model}, ${progress.totalTests}"
                            _resultText.value = text
//                            Log.i(HomeViewModel::class.java.simpleName, "testModel:$text ")
                        }

                        is TestProgress.Progress -> {
                            val text =
                                "TestState.Progress, ${progress.phase}, ${progress.current}, ${progress.total}, ${progress.description}"
                            _resultText.value = text
//                            Log.i(HomeViewModel::class.java.simpleName, "testModel:$text ")
                        }

                        is TestProgress.PhaseComplete -> {
                            val text = "TestState.PhaseCompleted, ${progress.phaseName}, ${progress.results}"
                            _resultText.value = text
//                            Log.i(HomeViewModel::class.java.simpleName, "testModel:$text ")
                        }

                        is TestProgress.Completed -> {
                            val text = "TestState.Completed, ${progress.model}, ${progress.finalReport},"
                            _resultText.value = text
                            _uiEnabled.value = true
                            Log.i(HomeViewModel::class.java.simpleName, "testModel:$text ")
                        }

                        is TestProgress.Error -> {
                            val text = "TestState.Error, ${progress.message}"
                            _resultText.value = text
                            _uiEnabled.value = true
//                            Log.i(HomeViewModel::class.java.simpleName, "testModel:$text ")
                        }
                    }
                }
        }
    }

    fun startComparison(selectedModels: List<ModelConfig> = getDefaultModels()) {
        if (selectedModels.isEmpty()) {
            _state.value = HomeUIState.Error("Не выбрано ни одной модели")
            return
        }
        modelJob?.cancel()
        modelJob = viewModelScope.launch {
            try {
                _state.value = HomeUIState.Loading
                comparisonManager.compareModels(
                    models = selectedModels,
                    progressCallback = { progress ->
                        _state.value = HomeUIState.Progress(progress)
                    }
                ).collect { results ->
                    val comparison = generateComparison(results)
                    _state.value = HomeUIState.Results(
                        text = formatResults(results, comparison)
                    )
                }
            } catch (e: CancellationException) {
                _state.value = HomeUIState.Idle
                throw e
            } catch (e: Exception) {
                _state.value = HomeUIState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun cancelWork() {
        modelJob?.cancel()
        _state.value = HomeUIState.Idle
    }

    private fun loadContent() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                smartSearchManager.loadContent()
            }
        }
    }

    private fun displayResults(results: List<SearchResult>) {
        val resultText = buildString {
            appendLine("Результаты поиска:")
            appendLine()

            if (results.isEmpty()) {
                appendLine("Ничего не найдено")
            } else {
                results.forEachIndexed { index, result ->
                    appendLine("${index + 1}. Релевантность: ${result.relevance}")
                    // Показываем часть текста с подсветкой найденных слов
                    appendLine(result.block.text.take(200) + "...")
                    appendLine()
                }
            }
        }

        _resultText.value = resultText
    }

    private fun updateUI(analysis: TextAnalysis, text: String) {
        // Выводим важные слова с весами
        val resultText = buildString {
            appendLine("Исходный текст: $text")
            appendLine("Токены: ${analysis.tokens.joinToString(" ")}")
            appendLine("\nВажные слова (по убыванию важности):")
            analysis.importantWords.forEach { word ->
                val percentage = word.weight * 100
                appendLine("${word.token}: ${String.format("%.1f%%", percentage)}")
            }

            // Добавим статистику
            appendLine("\nСтатистика:")
            appendLine("Всего слов: ${analysis.tokens.size}")
            appendLine("Значимых слов: ${analysis.importantWords.size}")
            appendLine("Средний вес: ${String.format("%.1f%%", analysis.attentionScore * 100)}")

            // Добавим распределение весов
            val weights = analysis.importantWords.map { it.weight }
            if (weights.isNotEmpty()) {
                appendLine("Максимальный вес: ${String.format("%.1f%%", (weights.maxOrNull() ?: 0f) * 100)}")
                appendLine("Минимальный вес: ${String.format("%.1f%%", (weights.minOrNull() ?: 0f) * 100)}")
            }
        }

        Log.d(HomeViewModel::class.java.simpleName, "updateUI: $resultText");

        // Выводим результаты
        _resultText.value = resultText

    }

    private fun formatResults(
        results: List<ComparisonResult>,
        comparison: ModelComparison
    ): String = buildString {
        appendLine("=== Результаты сравнения моделей ===\n")

        appendLine("Лучшие модели по категориям:")
        appendLine("- Общий результат: ${comparison.bestOverall}")
        appendLine("- Технические тексты: ${comparison.bestForTechnicalTexts}")
        appendLine("- Смешанные языки: ${comparison.bestForMixedLanguage}")
        appendLine("- Поиск: ${comparison.bestForSearch}")
        appendLine()

        results.forEach { result ->
            appendLine("=== ${result.modelName} ===")
            appendLine("Производительность:")
            appendLine("- Среднее время: ${result.metrics.performance.averageProcessingTime}ms")
            appendLine("- Пиковая память: ${result.metrics.memory.peakMemoryUsage / 1024}KB")
            appendLine()

            appendLine("Точность:")
            appendLine("- Общая: ${(result.metrics.accuracy.averageAccuracy * 100).roundToInt()}%")
            appendLine("- Технические термины: ${(result.metrics.accuracy.technicalTermsAccuracy * 100).roundToInt()}%")
            appendLine("- Смешанный текст: ${(result.metrics.accuracy.mixedLanguageAccuracy * 100).roundToInt()}%")
            appendLine()

            appendLine("Сильные стороны:")
            result.strengths.forEach { strength ->
                appendLine("- $strength")
            }
            appendLine()

            appendLine("Слабые стороны:")
            result.weaknesses.forEach { weakness ->
                appendLine("- $weakness")
            }
            appendLine()
        }

        appendLine("Рекомендации:")
        comparison.recommendations.forEach { recommendation ->
            appendLine("- $recommendation")
        }
    }

    private fun generateComparison(results: List<ComparisonResult>): ModelComparison {
        // Подготовка метрик для оценки моделей
        val scores = calculateScores(results)

        // Определение лучших моделей по различным категориям
        val bestOverall = findBestOverall(scores)
        val bestForTechnicalTexts = findBestForCategory(scores, TestCategory.TECHNICAL)
        val bestForMixedLanguage = findBestForCategory(scores, TestCategory.MIXED_LANGUAGE)
        val bestForSearch = findBestForCategory(scores, TestCategory.SEARCH)

        // Генерация рекомендаций на основе результатов
        val recommendations = generateRecommendations(results)

        return ModelComparison(
            bestOverall = bestOverall,
            bestForTechnicalTexts = bestForTechnicalTexts,
            bestForMixedLanguage = bestForMixedLanguage,
            bestForSearch = bestForSearch,
            recommendations = recommendations,
            detailedComparison = scores
        )
    }

    private fun calculateScores(
        results: List<ComparisonResult>
    ): Map<String, ComparisonMetrics> {
        // Оценка каждой модели по набору метрик
        return results.associate { result ->
            val metrics = result.metrics
            result.modelName to ComparisonMetrics(
                overallScore = calculateOverallScore(metrics),
                performanceScore = calculatePerformanceScore(metrics.performance),
                accuracyScore = metrics.accuracy.averageAccuracy,
                memoryScore = calculateMemoryScore(metrics.memory),
                languageScore = calculateLanguageScore(metrics.languageSupport),
                categoryScores = calculateCategoryScores(result.testResults)
            )
        }
    }

    private fun calculateOverallScore(metrics: ModelMetrics): Float {
        // Общий балл, основанный на весах различных метрик
        return (
                metrics.accuracy.averageAccuracy * 0.4f +
                        calculatePerformanceScore(metrics.performance) * 0.3f +
                        calculateMemoryScore(metrics.memory) * 0.2f +
                        calculateLanguageScore(metrics.languageSupport) * 0.1f
                )
    }

    private fun calculatePerformanceScore(metrics: PerformanceMetrics): Float {
        // Оценка производительности на основе среднего времени обработки
        val timeScore = 1f - (metrics.averageProcessingTime / 1000f).coerceIn(0f, 1f)
        return timeScore
    }

    private fun calculateMemoryScore(metrics: MemoryMetrics): Float {
        // Оценка эффективности использования памяти
        return metrics.memoryEfficiency.coerceIn(0f, 1f)
    }

    private fun calculateLanguageScore(metrics: LanguageMetrics): Float {
        // Оценка поддержки языков
        return (
                metrics.russianAccuracy * 0.4f +
                        metrics.englishAccuracy * 0.4f +
                        metrics.mixedTextAccuracy * 0.2f
                )
    }

    private fun calculateCategoryScores(
        results: Map<TestCategory, List<TestResultCompare>>
    ): Map<TestCategory, Float> {
        // Оценка точности для каждой категории тестов
        return results.mapValues { (_, results) ->
            results.map { result ->
                val expectedTokens = result.case.expectedTokens
                val foundTokens = result.foundTokens
                val intersection = expectedTokens.intersect(foundTokens)

                if (expectedTokens.isEmpty()) 1f
                else intersection.size.toFloat() / expectedTokens.size
            }.average().toFloat()
        }
    }

    private fun findBestOverall(
        scores: Map<String, ComparisonMetrics>
    ): String {
        // Находим модель с лучшим общим баллом
        return scores.maxByOrNull { it.value.overallScore }?.key ?: ""
    }

    private fun findBestForCategory(
        scores: Map<String, ComparisonMetrics>,
        category: TestCategory
    ): String {
        // Находим модель с лучшим баллом для заданной категории
        return scores.maxByOrNull { it.value.categoryScores[category] ?: 0f }?.key ?: ""
    }

    private fun generateRecommendations(
        results: List<ComparisonResult>
    ): List<String> {
        // Генерация рекомендаций на основе оценок моделей
        val recommendations = mutableListOf<String>()

        results.forEach { result ->
            val metrics = result.metrics

            when {
                metrics.accuracy.averageAccuracy > 0.8f &&
                        metrics.performance.averageProcessingTime < 100 -> {
                    recommendations.add(
                        "${result.modelName} рекомендуется для общего использования"
                    )
                }

                metrics.accuracy.technicalTermsAccuracy > 0.85f -> {
                    recommendations.add(
                        "${result.modelName} лучше всего подходит для технических текстов"
                    )
                }

                metrics.languageSupport.mixedTextAccuracy > 0.85f -> {
                    recommendations.add(
                        "${result.modelName} оптимальна для смешанных текстов"
                    )
                }
            }
        }

        return recommendations
    }

    private fun getDefaultModels(): List<ModelConfig> {
        return ModelConfig.values().toList()
    }
}
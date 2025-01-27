package com.arny.mobilebert.data.ai.test

import com.arny.mobilebert.data.ai.analyse.AIModelFactory
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.AccuracyMetrics
import com.arny.mobilebert.data.ai.models.ComparisonProgress
import com.arny.mobilebert.data.ai.models.ComparisonResult
import com.arny.mobilebert.data.ai.models.Language
import com.arny.mobilebert.data.ai.models.LanguageMetrics
import com.arny.mobilebert.data.ai.models.MemoryMetrics
import com.arny.mobilebert.data.ai.models.ModelMetrics
import com.arny.mobilebert.data.ai.models.PerformanceMetrics
import com.arny.mobilebert.data.ai.models.TestCategory
import com.arny.mobilebert.data.ai.models.TestResultCompare
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ModelComparisonManager @Inject constructor(
    private val modelFactory: AIModelFactory
) {
    private val testSuite = ComparisonTestSuite()

    suspend fun compareModels(
        models: List<ModelConfig>,
        progressCallback: (ComparisonProgress) -> Unit
    ): Flow<List<ComparisonResult>> = flow {
        val results = mutableListOf<ComparisonResult>()

        models.forEachIndexed { index, model ->
            progressCallback(
                ComparisonProgress.ModelStarted(
                    current = index + 1,
                    total = models.size,
                    modelName = model.modelName
                )
            )

            val analyzer = modelFactory.createAnalyzer(model)
            analyzer.initialize()

            val testResults = testSuite.runTests(
                analyzer = analyzer,
                onProgress = { phase, progress, currentTest ->
                    progressCallback(
                        ComparisonProgress.TestProgress(
                            modelName = model.modelName,
                            phase = phase,
                            progress = progress,
                            currentTest = currentTest
                        )
                    )
                }
            )

            val metrics = calculateMetrics(testResults)

            progressCallback(
                ComparisonProgress.ModelCompleted(
                    modelName = model.modelName,
                    metrics = metrics
                )
            )

            val result = ComparisonResult(
                modelName = model.modelName,
                metrics = metrics,
                strengths = findStrengths(metrics),
                weaknesses = findWeaknesses(metrics, testResults),
                testResults = testResults
            )

            results.add(result)
            emit(results.toList())

            analyzer.close()
        }
    }

    private fun calculateMetrics(
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): ModelMetrics {
        return ModelMetrics(
            performance = calculatePerformanceMetrics(testResults),
            accuracy = calculateAccuracyMetrics(testResults),
            memory = calculateMemoryMetrics(testResults),
            languageSupport = calculateLanguageMetrics(testResults)
        )
    }

    private fun calculatePerformanceMetrics(
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): PerformanceMetrics {
        val allResults = testResults.values.flatten()

        return PerformanceMetrics(
            averageProcessingTime = allResults.map { it.processingTime }.average().toFloat(),
            maxProcessingTime = allResults.maxOf { it.processingTime }.toFloat(),
            minProcessingTime = allResults.minOf { it.processingTime }.toFloat(),
            processingTimeByCategory = testResults.mapValues { (_, results) ->
                results.map { it.processingTime }.average().toFloat()
            },
            totalProcessingTime = allResults.sumOf { it.processingTime }
        )
    }

    private fun calculateAccuracyMetrics(
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): AccuracyMetrics {
        val accuracyByCategory = testResults.mapValues { (_, results) ->
            calculateCategoryAccuracy(results)
        }

        return AccuracyMetrics(
            technicalTermsAccuracy = accuracyByCategory[TestCategory.TECHNICAL] ?: 0f,
            mixedLanguageAccuracy = accuracyByCategory[TestCategory.MIXED_LANGUAGE] ?: 0f,
            searchAccuracy = accuracyByCategory[TestCategory.SEARCH] ?: 0f,
            errorAnalysisAccuracy = accuracyByCategory[TestCategory.ERROR_HANDLING] ?: 0f,
            importantWordsAccuracy = calculateImportantWordsAccuracy(testResults),
            averageAccuracy = accuracyByCategory.values.average().toFloat(),
            accuracyByCategory = accuracyByCategory
        )
    }

    private fun calculateCategoryAccuracy(results: List<TestResultCompare>): Float {
        return results.map { result ->
            val foundRelevant = result.foundTokens.intersect(result.case.expectedTokens)
            val precision = if (result.foundTokens.isNotEmpty()) {
                foundRelevant.size.toFloat() / result.foundTokens.size
            } else 0f
            val recall = if (result.case.expectedTokens.isNotEmpty()) {
                foundRelevant.size.toFloat() / result.case.expectedTokens.size
            } else 0f

            if (precision + recall > 0) {
                2 * (precision * recall) / (precision + recall) // F1 score
            } else 0f
        }.average().toFloat()
    }

    private fun calculateMemoryMetrics(
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): MemoryMetrics {
        val allResults = testResults.values.flatten()

        return MemoryMetrics(
            peakMemoryUsage = allResults.maxOf { it.memoryUsed },
            averageMemoryPerToken = allResults.map {
                it.memoryUsed.toFloat() / it.foundTokens.size
            }.average().toFloat(),
            memoryEfficiency = calculateMemoryEfficiency(allResults),
            memoryByCategory = testResults.mapValues { (_, results) ->
                results.sumOf { it.memoryUsed }
            }
        )
    }

    private fun calculateLanguageMetrics(
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): LanguageMetrics {
        val resultsByLanguage = testResults.values.flatten()
            .groupBy { it.case.language }

        return LanguageMetrics(
            russianAccuracy = calculateLanguageAccuracy(resultsByLanguage[Language.RUSSIAN]),
            englishAccuracy = calculateLanguageAccuracy(resultsByLanguage[Language.ENGLISH]),
            mixedTextAccuracy = calculateLanguageAccuracy(resultsByLanguage[Language.MIXED]),
            languageDetectionAccuracy = calculateLanguageDetectionAccuracy(testResults)
        )
    }

    private fun findStrengths(
        metrics: ModelMetrics
    ): List<String> {
        val strengths = mutableListOf<String>()

        with(metrics) {
            if (performance.averageProcessingTime < 100) {
                strengths.add("Высокая скорость обработки")
            }
            if (accuracy.averageAccuracy > 0.8f) {
                strengths.add("Высокая точность")
            }
            if (memory.memoryEfficiency > 0.7f) {
                strengths.add("Эффективное использование памяти")
            }
            if (languageSupport.mixedTextAccuracy > 0.8f) {
                strengths.add("Отличная поддержка смешанных языков")
            }
        }

        return strengths
    }

    private fun findWeaknesses(
        metrics: ModelMetrics,
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): List<String> {
        val weaknesses = mutableListOf<String>()

        with(metrics) {
            if (performance.averageProcessingTime > 300) {
                weaknesses.add("Медленная обработка")
            }
            if (accuracy.averageAccuracy < 0.5f) {
                weaknesses.add("Низкая точность")
            }
            if (memory.memoryEfficiency < 0.3f) {
                weaknesses.add("Неэффективное использование памяти")
            }
        }

        return weaknesses
    }

    private fun calculateMemoryEfficiency(results: List<TestResultCompare>): Float {
        val totalMemory = results.sumOf { it.memoryUsed }
        val totalTokens = results.sumOf { it.foundTokens.size }
        return if (totalTokens > 0) {
            1 - (totalMemory.toFloat() / (totalTokens * 1024 * 1024))
        } else 0f
    }

    private fun calculateLanguageAccuracy(results: List<TestResultCompare>?): Float {
        return results?.let { calculateCategoryAccuracy(it) } ?: 0f
    }

    private fun calculateLanguageDetectionAccuracy(
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): Float {
        // Implement language detection accuracy calculation
        return 0f
    }

    private fun calculateImportantWordsAccuracy(
        testResults: Map<TestCategory, List<TestResultCompare>>
    ): Float {
        return testResults.values.flatten()
            .map { result ->
                val expectedTokens = result.case.expectedTokens
                val foundImportantTokens = result.tokenWeights
                    .filter { it.value > 0.5f }
                    .keys

                val intersection = expectedTokens.intersect(foundImportantTokens)
                if (expectedTokens.isEmpty()) 1f
                else intersection.size.toFloat() / expectedTokens.size
            }
            .average()
            .toFloat()
    }
}
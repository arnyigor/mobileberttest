package com.arny.mobilebert.data.ai.test

import android.content.Context
import android.os.Debug
import android.util.Log
import com.arny.mobilebert.data.ai.analyse.AIModelFactory
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.InitializationResult
import com.arny.mobilebert.data.ai.models.MemoryInfo
import com.arny.mobilebert.data.ai.models.MemoryResult
import com.arny.mobilebert.data.ai.models.MemoryStatistics
import com.arny.mobilebert.data.ai.models.ModelInfo
import com.arny.mobilebert.data.ai.models.TestCase
import com.arny.mobilebert.data.ai.models.TestProgress
import com.arny.mobilebert.data.ai.models.TestResult
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.data.utils.formatFileSize
import com.arny.mobilebert.data.utils.getModelSize
import com.arny.mobilebert.domain.ai.ITestManager
import com.arny.mobilebert.domain.ai.ITextAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ModelTestManager @Inject constructor(
    private val context: Context,
    private val modelFactory: AIModelFactory,
) : ITestManager {
    private var currentAnalyzer: ITextAnalyzer? = null
    private val memoryTracker = MemoryTracker()

    override suspend fun testMultipleInitializations(
        modelConfig: ModelConfig,
        iterations: Int
    ): Flow<TestProgress> = flow {
        emit(TestProgress.Started(modelConfig.modelName, iterations))

        try {
            val modelSize = getModelSize(context, modelConfig.modelPath)
            val vocabSize = getModelSize(context, modelConfig.vocabPath)

            prepareForTest(modelConfig)

            val initResults = mutableListOf<InitializationResult>()
            var currentStatistics = MemoryStatistics(
                totalTime = 0L,
                totalNativeHeap = 0L,
                totalJavaHeap = 0L,
                minTime = Long.MAX_VALUE,
                maxTime = 0L,
                minMemory = Long.MAX_VALUE,
                maxMemory = 0L
            )

            repeat(iterations) { iteration ->
                emit(
                    TestProgress.Progress(
                        phase = "Multiple Initializations",
                        current = iteration + 1,
                        total = iterations,
                        description = "Running initialization ${iteration + 1}/$iterations"
                    )
                )

                cleanupBeforeIteration()

                val startTime = System.nanoTime()
                val startMemory = getDetailedMemoryInfo()

                currentAnalyzer?.close()
                currentAnalyzer = null
                currentAnalyzer = modelFactory.createAnalyzer(modelConfig)
                currentAnalyzer!!.initialize()

                val endTime = System.nanoTime()
                val endMemory = getDetailedMemoryInfo()

                if (validateMemoryMetrics(endMemory)) {
                    val initTime = (endTime - startTime) / 1_000_000
                    val memoryResult = calculateMemoryResult(startMemory, endMemory)

                    initResults.add(
                        InitializationResult(
                            iteration = iteration + 1,
                            time = initTime,
                            memoryInfo = memoryResult,
                            rawMemoryInfo = endMemory
                        )
                    )

                    currentStatistics = updateStatistics(
                        initTime,
                        memoryResult,
                        currentStatistics.totalTime,
                        currentStatistics.totalNativeHeap,
                        currentStatistics.totalJavaHeap,
                        currentStatistics.minTime,
                        currentStatistics.maxTime,
                        currentStatistics.minMemory,
                        currentStatistics.maxMemory
                    )
                }

                delay(50)
            }

            val summaryReport = generateReport(
                modelConfig = modelConfig,
                modelSize = modelSize,
                vocabSize = vocabSize,
                initResults = initResults,
                statistics = currentStatistics,
                iterations = iterations
            )

            emit(TestProgress.Completed(modelConfig.modelName, summaryReport))

        } catch (e: Exception) {
            Log.e(TAG, "Multiple initializations test failed", e)
            emit(TestProgress.Error("Multiple initializations failed: ${e.message}"))
        }
    }

    private fun updateStatistics(
        initTime: Long,
        memoryResult: MemoryResult,
        totalTime: Long,
        totalNativeHeap: Long,
        totalJavaHeap: Long,
        minTime: Long,
        maxTime: Long,
        minMemory: Long,
        maxMemory: Long
    ): MemoryStatistics {
        return MemoryStatistics(
            totalTime = totalTime + initTime,
            totalNativeHeap = totalNativeHeap + memoryResult.nativeHeap,
            totalJavaHeap = totalJavaHeap + memoryResult.javaHeap,
            minTime = minOf(minTime, initTime),
            maxTime = maxOf(maxTime, initTime),
            minMemory = minOf(minMemory, memoryResult.totalMemory),
            maxMemory = maxOf(maxMemory, memoryResult.totalMemory)
        )
    }

    private fun calculateMemoryResult(
        startMemory: MemoryInfo,
        endMemory: MemoryInfo
    ): MemoryResult {
        val nativeHeapDelta = endMemory.nativeHeap - startMemory.nativeHeap
        val javaHeapDelta = endMemory.javaHeap - startMemory.javaHeap
        val totalMemoryDelta = endMemory.total - startMemory.total

        val memoryUsage = (endMemory.allocated.toFloat() / endMemory.maxMemory) * 100
        val nativeUsage = if (endMemory.nativeHeapSize > 0) {
            (endMemory.nativeHeapAllocatedSize.toFloat() / endMemory.nativeHeapSize) * 100
        } else {
            0f
        }

        return MemoryResult(
            nativeHeap = nativeHeapDelta,
            javaHeap = javaHeapDelta,
            totalMemory = totalMemoryDelta,
            memoryUsage = memoryUsage,
            nativeUsage = nativeUsage
        )
    }

    private suspend fun prepareForTest(modelConfig: ModelConfig) {
        System.gc()
        delay(100)

        // Прогрев системы
        repeat(3) {
            currentAnalyzer?.close()
            currentAnalyzer = null
            currentAnalyzer = modelFactory.createAnalyzer(modelConfig)
            currentAnalyzer?.initialize()
            delay(50)
        }

        System.gc()
        delay(100)
    }

    private suspend fun cleanupBeforeIteration() {
        System.gc()
        delay(50)
    }

    private fun validateMemoryMetrics(info: MemoryInfo): Boolean {
        return info.nativeHeapSize >= info.nativeHeapAllocatedSize &&
                info.totalPss > 0 &&
                info.nativeHeapSize > 0
    }

    private fun calculateStdDev(values: List<Number>): Double {
        val mean = values.map { it.toDouble() }.average()
        val variance = values.map { (it.toDouble() - mean) * (it.toDouble() - mean) }.average()
        return sqrt(variance)
    }

    private fun analyzeTimeTrend(results: List<InitializationResult>): String {
        val times = results.map { it.time }
        val firstHalf = times.take(times.size / 2).average()
        val secondHalf = times.takeLast(times.size / 2).average()

        val change = (secondHalf - firstHalf) / firstHalf * 100

        return when {
            change < -5.0 -> "Улучшение (${String.format("%.1f", -change)}%)"
            change > 5.0 -> "Ухудшение (${String.format("%.1f", change)}%)"
            else -> "Стабильно (${String.format("%.1f", change)}%)"
        }
    }

    private fun analyzeMemoryTrend(results: List<InitializationResult>): String {
        val memory = results.map { it.memoryInfo.totalMemory }
        val firstHalf = memory.take(memory.size / 2).average()
        val secondHalf = memory.takeLast(memory.size / 2).average()

        val change = if (firstHalf != 0.0) {
            (secondHalf - firstHalf) / firstHalf * 100
        } else {
            0.0
        }

        return when {
            change < -5.0 -> "Оптимизация (${String.format("%.1f", -change)}%)"
            change > 5.0 -> "Рост потребления (${String.format("%.1f", change)}%)"
            else -> "Стабильно (${String.format("%.1f", change)}%)"
        }
    }

    private fun getDetailedMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val allocatedMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()

        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)

        return MemoryInfo(
            nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024,
            javaHeap = (allocatedMemory - freeMemory) / 1024,
            total = (Debug.getNativeHeapAllocatedSize() + (allocatedMemory - freeMemory)) / 1024,
            nativeHeapSize = Debug.getNativeHeapSize() / 1024,
            nativeHeapFreeSize = Debug.getNativeHeapFreeSize() / 1024,
            nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize() / 1024,
            totalPss = memoryInfo.totalPss.toLong(),
            nativePss = memoryInfo.nativePss.toLong(),
            dalvikPss = memoryInfo.dalvikPss.toLong(),
            allocated = allocatedMemory / 1024,
            free = freeMemory / 1024,
            maxMemory = maxMemory / 1024
        )
    }

    private fun generateReport(
        modelConfig: ModelConfig,
        modelSize: Long,
        vocabSize: Long,
        initResults: List<InitializationResult>,
        statistics: MemoryStatistics,
        iterations: Int
    ): String = buildString {
        appendLine("=== Multiple Initializations Report ===")
        appendLine("Model: ${modelConfig.modelName}")

        appendLine("\nModel Information:")
        appendLine("- Model file size: ${formatFileSize(modelSize)}")
        appendLine("- Vocab file size: ${formatFileSize(vocabSize)}")
        appendLine("- Total size: ${formatFileSize(modelSize + vocabSize)}")
        appendLine("- Output shape: ${modelConfig.outputShape}")
        if (vocabSize == 0L) {
            appendLine("WARNING: Vocab file not found or empty!")
        }

        appendLine("\nIterations: $iterations")

        // Анализ выбросов
        val outlierAnalysis = analyzeOutliers(initResults)
        if (outlierAnalysis.isNotEmpty()) {
            appendLine("\nOutlier Analysis:")
            appendLine(outlierAnalysis)
        }

        appendLine("\nTime Statistics:")
        appendLine("- Average time: ${statistics.totalTime / iterations}ms")
        appendLine("- Min time: ${statistics.minTime}ms")
        appendLine("- Max time: ${statistics.maxTime}ms")
        appendLine("- Standard deviation: ${calculateStdDev(initResults.map { it.time })}")
        appendLine("- Median time: ${calculateMedian(initResults.map { it.time })}")

        appendLine("\nDetailed Memory Statistics:")
        appendLine("Basic Memory:")
        val avgNativeHeap = statistics.totalNativeHeap / iterations
        val avgJavaHeap = statistics.totalJavaHeap / iterations
        appendLine("- Average Native Heap: ${maxOf(0, avgNativeHeap)}KB")
        appendLine("- Average Java Heap: ${maxOf(0, avgJavaHeap)}KB")
        appendLine("- Min total memory: ${maxOf(0, statistics.minMemory)}KB")
        appendLine("- Max total memory: ${statistics.maxMemory}KB")
        appendLine("- Memory std dev: ${
            calculateStdDev(initResults.map {
                maxOf(0.0, (it.memoryInfo.nativeHeap + it.memoryInfo.javaHeap).toDouble())
            })
        }")

        appendLine("\nPSS Statistics:")
        val avgTotalPss = initResults.map { it.rawMemoryInfo.totalPss }.average()
        val avgNativePss = initResults.map { it.rawMemoryInfo.nativePss }.average()
        val avgDalvikPss = initResults.map { it.rawMemoryInfo.dalvikPss }.average()
        appendLine("- Average Total PSS: ${avgTotalPss.roundToInt()}KB")
        appendLine("- Average Native PSS: ${avgNativePss.roundToInt()}KB")
        appendLine("- Average Dalvik PSS: ${avgDalvikPss.roundToInt()}KB")
        appendLine("- PSS std dev: ${calculateStdDev(initResults.map { it.rawMemoryInfo.totalPss })}")

        appendLine("\nNative Heap Details:")
        val avgNativeHeapSize = initResults.map { it.rawMemoryInfo.nativeHeapSize }.average()
        val avgNativeHeapFree = initResults.map { it.rawMemoryInfo.nativeHeapFreeSize }.average()
        val avgNativeHeapAllocated = initResults.map { it.rawMemoryInfo.nativeHeapAllocatedSize }.average()
        appendLine("- Average Size: ${avgNativeHeapSize.roundToInt()}KB")
        appendLine("- Average Free: ${avgNativeHeapFree.roundToInt()}KB")
        appendLine("- Average Allocated: ${avgNativeHeapAllocated.roundToInt()}KB")
        appendLine("- Average Usage: ${
            (avgNativeHeapAllocated / (avgNativeHeapSize + 1) * 100).roundToInt()
        }%")

        appendLine("\nMemory Usage Statistics:")
        appendLine("- Average Memory Usage: ${
            initResults.map { it.memoryInfo.memoryUsage }.average().roundToInt()
        }%")
        appendLine("- Average Native Usage: ${
            initResults.map { it.memoryInfo.nativeUsage }.average().roundToInt()
        }%")
        appendLine("- Memory Usage std dev: ${
            calculateStdDev(initResults.map { it.memoryInfo.memoryUsage })
        }")

        appendLine("\nPerformance Analysis:")
        appendLine("Time Distribution:")
        val timeDistribution = analyzeTimeDistribution(initResults)
        timeDistribution.forEach { (range, count) ->
            appendLine("  $range ms: $count iterations (${
                String.format("%.1f", count.toFloat() / iterations * 100)
            }%)")
        }

        appendLine("\nPerformance Trend:")
        val timeTrend = analyzeTimeTrend(initResults)
        val memoryTrend = analyzeMemoryTrend(initResults)
        appendLine("Time trend: $timeTrend")
        appendLine("Memory trend: $memoryTrend")

        if (iterations >= 10) {
            appendLine("\nStability Analysis:")
            val firstTenAvg = initResults.take(10).map { it.time }.average()
            val lastTenAvg = initResults.takeLast(10).map { it.time }.average()
            val timeChange = ((lastTenAvg - firstTenAvg) / firstTenAvg * 100)
            appendLine("- First 10 iterations avg: ${String.format("%.2f", firstTenAvg)}ms")
            appendLine("- Last 10 iterations avg: ${String.format("%.2f", lastTenAvg)}ms")
            appendLine("- Performance change: ${String.format("%.1f", timeChange)}%")

            // Анализ стабильности
            val timeStability = analyzeStability(initResults.map { it.time })
            appendLine("- Time stability score: ${String.format("%.2f", timeStability)} " +
                    "(${getStabilityDescription(timeStability)})")
        }
    }

    private fun analyzeOutliers(results: List<InitializationResult>): String {
        val times = results.map { it.time }
        val q1 = calculatePercentile(times, 0.25)
        val q3 = calculatePercentile(times, 0.75)
        val iqr = q3 - q1
        val lowerBound = q1 - 1.5 * iqr
        val upperBound = q3 + 1.5 * iqr

        val outliers = times.filter { it < lowerBound || it > upperBound }

        return if (outliers.isNotEmpty()) {
            buildString {
                appendLine("Found ${outliers.size} outliers:")
                appendLine("- IQR range: $q1-$q3 ms")
                appendLine("- Outlier bounds: $lowerBound-$upperBound ms")
                appendLine("- Outlier values: ${outliers.joinToString()}")
            }
        } else ""
    }

    private fun calculatePercentile(values: List<Long>, percentile: Double): Double {
        val sorted = values.sorted()
        val index = (percentile * (sorted.size - 1)).toInt()
        return sorted[index].toDouble()
    }

    private fun calculateMedian(values: List<Long>): Double {
        return calculatePercentile(values, 0.5)
    }

    private fun analyzeStability(values: List<Long>): Double {
        val mean = values.average()
        val std = calculateStdDev(values)
        return (1.0 - (std / mean)).coerceIn(0.0, 1.0)
    }

    private fun getStabilityDescription(score: Double): String = when {
        score >= 0.95 -> "Excellent"
        score >= 0.90 -> "Very Good"
        score >= 0.85 -> "Good"
        score >= 0.80 -> "Fair"
        else -> "Poor"
    }

    private fun analyzeTimeDistribution(results: List<InitializationResult>): Map<String, Int> {
        val times = results.map { it.time }
        val minTime = times.min()
        val maxTime = times.max()
        val range = maxTime - minTime

        // Определяем оптимальное количество интервалов
        val numBuckets = sqrt(times.size.toDouble()).toInt().coerceIn(5, 10)
        val bucketSize = maxOf(1L, range / numBuckets)

        return results.groupBy { result ->
            val bucket = ((result.time - minTime) / bucketSize)
            val start = minTime + bucket * bucketSize
            val end = minTime + (bucket + 1) * bucketSize
            "$start-$end"
        }.mapValues { it.value.size }
            .toSortedMap()
    }

    // Добавляем метод тестирования инициализации
    override suspend fun testModelInitialization(
        modelConfig: ModelConfig
    ): Flow<TestProgress> = flow {
        emit(TestProgress.Started(modelConfig.modelName, 1))

        try {
            // Принудительная очистка памяти перед тестом
            System.gc()
            Thread.sleep(100)

            val startTime = System.nanoTime()
            val startMemory = getDetailedMemoryInfo()
            memoryTracker.trackMemory(startMemory)

            // Создаем новый анализатор
            currentAnalyzer?.close()
            currentAnalyzer = null

            currentAnalyzer = modelFactory.createAnalyzer(modelConfig)

            emit(
                TestProgress.Progress(
                    phase = "Initialization",
                    current = 0,
                    total = 1,
                    description = "Initializing model..."
                )
            )

            currentAnalyzer!!.initialize()
//            val tensorInfo = currentAnalyzer!!.getTensorsInfo()

            val endTime = System.nanoTime()
            val memoryInfo = getDetailedMemoryInfo()
            memoryTracker.trackMemory(memoryInfo)

            val initReport = buildString {
                appendLine("=== Model Initialization Report ===")
                appendLine("Model: ${modelConfig.modelName}")

               /* appendLine("\nTensor Information:")
                appendLine("Input Tensors:")
                tensorInfo.inputTensors.forEach { tensor ->
                    appendLine("- Name: ${tensor.name}")
                    appendLine("  Shape: ${tensor.shape.joinToString()}")
                    appendLine("  Type: ${tensor.dataType}")
                }

                appendLine("\nOutput Tensors:")
                tensorInfo.outputTensors.forEach { tensor ->
                    appendLine("- Name: ${tensor.name}")
                    appendLine("  Shape: ${tensor.shape.joinToString()}")
                    appendLine("  Type: ${tensor.dataType}")
                }*/

                appendLine("\nDetailed Memory Usage:")
                appendLine("Basic Memory:")
                appendLine("- Native Heap: ${memoryInfo.nativeHeap - startMemory.nativeHeap}KB")
                appendLine("- Java Heap: ${memoryInfo.javaHeap - startMemory.javaHeap}KB")
                appendLine("- Total Used: ${memoryInfo.total - startMemory.total}KB")

                appendLine("\nHeap Statistics:")
                appendLine("- Allocated Memory: ${memoryInfo.allocated}KB")
                appendLine("- Free Memory: ${memoryInfo.free}KB")
                appendLine("- Max Memory Available: ${memoryInfo.maxMemory}KB")
                appendLine("- Memory Usage: ${(memoryInfo.allocated.toFloat() / memoryInfo.maxMemory * 100).roundToInt()}%")

                appendLine("\nNative Memory:")
                appendLine("- Native Allocated: ${memoryInfo.nativeHeapAllocatedSize}KB")
                appendLine("- Native Free: ${memoryInfo.nativeHeapFreeSize}KB")
                appendLine("- Native Usage: ${(memoryInfo.nativeHeapAllocatedSize.toFloat() / (memoryInfo.nativeHeapAllocatedSize + memoryInfo
                    .nativeHeapFreeSize) * 100).roundToInt()}%")

                appendLine("\nProcess Memory (PSS):")
                appendLine("- Native PSS: ${memoryInfo.nativePss}KB")
                appendLine("- Dalvik PSS: ${memoryInfo.dalvikPss}KB")
                appendLine("- Total PSS: ${memoryInfo.totalPss}KB")

                appendLine("\nMemory Changes:")
                appendLine("- Native Heap Change: ${memoryInfo.nativeHeap - startMemory.nativeHeap}KB")
                appendLine("- Java Heap Change: ${memoryInfo.javaHeap - startMemory.javaHeap}KB")
                appendLine("- Total PSS Change: ${memoryInfo.totalPss - startMemory.totalPss}KB")

                appendLine("\nMemory Efficiency:")
                val memoryPerOutput = (memoryInfo.total - startMemory.total).toFloat() / modelConfig.outputShape[1]
                appendLine("- Memory per output dimension: ${String.format("%.2f", memoryPerOutput)}KB")

                appendLine("\nMemory Usage Trend:")
                appendLine(memoryTracker.getMemoryTrend())

                appendLine("\nInitialization Details:")
                appendLine("- Raw time: ${(endTime - startTime) / 1_000_000.0}ms")
                appendLine("- First run: ${!currentAnalyzer!!.isReused}")
            }

            emit(TestProgress.Completed(modelConfig.modelName, initReport))

        } catch (e: Exception) {
            Log.e(TAG, "Initialization test failed", e)
            emit(TestProgress.Error("Initialization failed: ${e.message}"))
        }
    }

    private fun validateTokenization(result: TokenizeResult, testCase: TestCase): List<String> {
        val errors = mutableListOf<String>()

        // Проверка количества токенов
        if (testCase.expectedTokens != null && result.tokens.size != testCase.expectedTokens) {
            errors.add("Token count mismatch: expected ${testCase.expectedTokens}, got ${result.tokens.size}")
        }

        // Проверка последовательности токенов
        if (testCase.expectedTokenSequence != null && result.tokens != testCase.expectedTokenSequence) {
            errors.add("Token sequence mismatch:\nexpected: ${testCase.expectedTokenSequence}\ngot: ${result.tokens}")
        }

        // Проверка inputIds
        if (testCase.expectedInputIds != null && !result.inputIds.contentEquals(testCase.expectedInputIds.toLongArray())) {
            errors.add("InputIds mismatch:\nexpected: ${testCase.expectedInputIds}\ngot: ${result.inputIds.asList()}")
        }

        // Проверка maskIds
        if (testCase.expectedMaskIds != null && !result.maskIds.contentEquals(testCase.expectedMaskIds.toLongArray())) {
            errors.add("MaskIds mismatch:\nexpected: ${testCase.expectedMaskIds}\ngot: ${result.maskIds.asList()}")
        }

        // Проверка typeIds
        if (testCase.expectedTypeIds != null && !result.typeIds.contentEquals(testCase.expectedTypeIds.toLongArray())) {
            errors.add("TypeIds mismatch:\nexpected: ${testCase.expectedTypeIds}\ngot: ${result.typeIds.asList()}")
        }

        return errors
    }

    override suspend fun testTokenization(modelConfig: ModelConfig): Flow<TestProgress> = flow {
        val tokenizer = modelFactory.createTokenizer(modelConfig)
        val testSuite = ModelTestSuite()

        val tokenizationTests = testSuite.extendedTokenizationTests
        emit(TestProgress.Started(modelConfig.modelName, tokenizationTests.size))

        var completedTests = 0
        val errors = mutableListOf<String>()

        tokenizationTests.forEach { testCase ->
            val result = tokenizer.tokenize(testCase.text)
            val testErrors = validateTokenization(result, testCase)

            if (testErrors.isNotEmpty()) {
                errors.addAll(testErrors)
            }

            completedTests++
            emit(
                TestProgress.Progress(
                    phase = "Tokenization",
                    current = completedTests,
                    total = tokenizationTests.size,
                    description = "Testing: ${testCase.text.take(50)}..."
                )
            )
        }

        val report = buildString {
            appendLine("=== Tokenization Test Report ===")
            appendLine("Model: ${modelConfig.modelName}")
            appendLine("Test category: ${tokenizationTests[0].category}")
            appendLine("Total tests: ${tokenizationTests.size}")
            appendLine("Passed: ${tokenizationTests.size - errors.size}")
            if (errors.isNotEmpty()) {
                appendLine("\nErrors:")
                errors.forEach { error ->
                    appendLine("- $error")
                }
            }
        }

        emit(TestProgress.Completed(modelConfig.modelName, report))
    }

    override suspend fun testModel(
        modelConfig: ModelConfig,
    ): Flow<TestProgress> = flow {
        currentAnalyzer?.close()
        val testSuite = ModelTestSuite()

        currentAnalyzer = modelFactory.createAnalyzer(modelConfig).also {
            it.initialize()
        }
        Log.i(ModelTestManager::class.java.simpleName, "Starting test for ${modelConfig.modelName}")

        try {
            val testCategories = testSuite.getAllTests()
            val totalTests = testCategories.sumOf { it.second.size }

            emit(TestProgress.Started(modelConfig.modelName, totalTests))

            var completedTests = 0
            val results = mutableMapOf<String, TestResult>()

            // Выполняем тесты для каждой категории
            testCategories.forEach { (category, tests) ->
                emit(
                    TestProgress.Progress(
                        phase = category,
                        current = completedTests,
                        total = totalTests,
                        description = "Starting $category tests..."
                    )
                )

                val categoryResults = runTestPhase(
                    category,
                    tests,
                    currentAnalyzer!!
                ) { current, _, text ->
                    emit(
                        TestProgress.Progress(
                            phase = category,
                            current = completedTests + current,
                            total = totalTests,
                            description = "Processing: ${text.take(50)}..."
                        )
                    )
                }

                results[category] = categoryResults
                emit(TestProgress.PhaseComplete(category, categoryResults))
                completedTests += tests.size
            }

            // Формируем итоговый отчет
            val finalReport = createDetailedReport(modelConfig.modelName, results)
            emit(TestProgress.Completed(modelConfig.modelName, finalReport))

        } catch (e: Exception) {
            Log.e(TAG, "Test failed", e)
            emit(TestProgress.Error("Test failed: ${e.message}"))
        }
    }

    private suspend fun runTestPhase(
        phase: String,
        tests: List<TestCase>,
        analyzer: ITextAnalyzer,
        onProgress: suspend (current: Int, total: Int, text: String) -> Unit
    ): TestResult {
        var totalTokens = 0
        var totalTime = 0L
        var totalMemory = 0L
        val errors = mutableListOf<String>()
        Log.i(ModelTestManager::class.java.simpleName, "runTestPhase: $phase");

        tests.forEachIndexed { index, testCase ->
            val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val startTime = System.currentTimeMillis()

            try {
                // Выполняем анализ текста
                val analysis = analyzer.analyzeText(testCase.text)

                // Проверяем ожидаемые результаты
                testCase.expectedTokens?.let { expected ->
                    if (analysis.tokens.size != expected) {
                        errors.add("Token count mismatch for '${testCase.text.take(50)}': expected $expected, got ${analysis.tokens.size}")
                    }
                }

                testCase.expectedImportantWords?.let { expected ->
                    val actual = analysis.importantWords.map { it.token }.toSet()
                    val missing = expected - actual
                    if (missing.isNotEmpty()) {
                        errors.add("Missing important words: $missing")
                    }
                }

                totalTokens += analysis.tokens.size

                onProgress(index + 1, tests.size, testCase.text)

            } catch (e: Exception) {
                errors.add("Error processing '${testCase.text.take(50)}': ${e.message}")
            }

            val endTime = System.currentTimeMillis()
            val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

            totalTime += (endTime - startTime)
            totalMemory += (endMemory - startMemory)
        }

        return TestResult(
            averageProcessingTime = totalTime.toFloat() / tests.size,
            tokensCount = totalTokens,
            memoryUsage = totalMemory / 1024, // Convert to KB
            analysisTime = totalTime,
            errors = errors,
            averageTokens = totalTokens.toFloat() / tests.size,
        )
    }

    private fun createDetailedReport(
        model: String,
        results: Map<String, TestResult>
    ): String = buildString {
        appendLine("=== Detailed Analysis Report for $model ===\n")

        // Общая статистика
        val totalTime = results.values.sumOf { it.analysisTime }
        val totalTokens = results.values.sumOf { it.tokensCount }
        val avgProcessingTime = results.values.map { it.averageProcessingTime }.average()

        appendLine("Overall Statistics:")
        appendLine("- Total processing time: ${totalTime}ms")
        appendLine("- Total tokens processed: $totalTokens")
        appendLine("- Average processing time: ${avgProcessingTime.roundToInt()}ms")
        appendLine()

        // Результаты по категориям
        results.forEach { (category, result) ->
            appendLine("$category Results:")
            appendLine("- Average processing time: ${result.averageProcessingTime}ms")
            appendLine("- Tokens processed: ${result.tokensCount}")
            appendLine("- Memory usage: ${result.memoryUsage}KB")
            if (result.errors.isNotEmpty()) {
                appendLine("- Errors found: ${result.errors.size}")
                result.errors.forEach { error ->
                    appendLine("  * $error")
                }
            }
            appendLine()
        }

        // Анализ производительности
        appendLine("Performance Analysis:")
        appendLine("- Best performing category: ${results.minByOrNull { it.value.averageProcessingTime }?.key}")
        appendLine("- Most challenging category: ${results.maxByOrNull { it.value.averageProcessingTime }?.key}")
        appendLine("- Memory efficiency: ${results.values.sumOf { it.memoryUsage / it.tokensCount }}KB per token")
        appendLine()

        // Рекомендации
        appendLine("Recommendations:")
        if (avgProcessingTime > 100) {
            appendLine("- Consider batch processing for better performance")
        }
        if (results.values.any { it.memoryUsage > 1000 * 1024 }) {
            appendLine("- Memory usage is high, consider implementing text chunking")
        }
        results.forEach { (category, result) ->
            if (result.averageProcessingTime > avgProcessingTime * 1.5) {
                appendLine("- Optimize processing for $category category")
            }
        }
    }

    companion object {
        private const val TAG = "ModelTestManager"
    }
}
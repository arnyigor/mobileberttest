package com.arny.mobilebert.data.ai.test

import com.arny.mobilebert.data.ai.models.Language
import com.arny.mobilebert.data.ai.models.TestCaseCompare
import com.arny.mobilebert.data.ai.models.TestCategory
import com.arny.mobilebert.data.ai.models.TestResultCompare
import com.arny.mobilebert.domain.ai.ITextAnalyzer

class ComparisonTestSuite {
    private val technicalTests = listOf(
        TestCaseCompare(
            text = "MVVM архитектура приложения",
            expectedTokens = setOf("mvvm", "архитектура", "приложения"),
            category = TestCategory.TECHNICAL
        ),
        TestCaseCompare(
            text = """
            Clean Architecture в Android приложении:
            - Presentation layer (MVVM)
            - Domain layer (Use Cases)
            - Data layer (Repository)
            """.trimIndent(),
            expectedTokens = setOf(
                "clean", "architecture", "mvvm", "repository",
                "presentation", "domain", "data", "layer"
            ),
            category = TestCategory.TECHNICAL
        ),
        TestCaseCompare(
            text = """
            Корутины в Kotlin:
            suspend fun loadData() = coroutineScope {
                val result = async { api.getData() }
                result.await()
            }
            """.trimIndent(),
            expectedTokens = setOf(
                "корутины", "kotlin", "suspend", "coroutinescope",
                "async", "await"
            ),
            category = TestCategory.TECHNICAL
        ),
        TestCaseCompare(
            text = """
            Lifecycle-aware ViewModels используют StateFlow:
            private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
            val uiState: StateFlow<UiState> = _uiState.asStateFlow()
            """.trimIndent(),
            expectedTokens = setOf(
                "lifecycle", "viewmodels", "stateflow", "uistate"
            ),
            category = TestCategory.TECHNICAL,
            language = Language.MIXED
        )
    )

    private val searchTests = listOf(
        TestCaseCompare(
            text = "как использовать MVVM в Android",
            expectedTokens = setOf("mvvm", "android", "использовать"),
            category = TestCategory.SEARCH,
            language = Language.RUSSIAN
        ),
        TestCaseCompare(
            text = "проблемы с отображением списка в RecyclerView",
            expectedTokens = setOf("recyclerview", "список", "отображение", "проблемы"),
            category = TestCategory.SEARCH,
            language = Language.MIXED
        ),
        TestCaseCompare(
            text = "утечки памяти при загрузке изображений",
            expectedTokens = setOf("утечки", "память", "изображения", "загрузка"),
            category = TestCategory.SEARCH,
            language = Language.RUSSIAN
        )
    )

    private val mixedLanguageTests = listOf(
        TestCaseCompare(
            text = """
            @Composable
            fun MainScreen() {
                // Основной экран приложения
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(title = "Главная")
                    ContentList(items = uiState.items)
                }
            }
            """.trimIndent(),
            expectedTokens = setOf(
                "composable", "mainscreen", "column", "modifier",
                "основной", "экран", "приложения"
            ),
            category = TestCategory.MIXED_LANGUAGE,
            language = Language.MIXED
        ),
        TestCaseCompare(
            text = "git commit -m 'Fixed crash при повороте экрана'",
            expectedTokens = setOf("git", "commit", "fixed", "crash", "поворот", "экран"),
            category = TestCategory.MIXED_LANGUAGE,
            language = Language.MIXED
        )
    )

    private val errorTests = listOf(
        TestCaseCompare(
            text = """
            E/AndroidRuntime: FATAL EXCEPTION: main
                Process: com.example.app, PID: 1234
                java.lang.IllegalStateException: Fragment UserProfileFragment not attached to an activity
                at androidx.fragment.app.Fragment.requireActivity(Fragment.java:805)
            """.trimIndent(),
            expectedTokens = setOf(
                "fatal", "exception", "fragment", "illegalstateexception",
                "requireactivity"
            ),
            category = TestCategory.ERROR_HANDLING,
            language = Language.ENGLISH
        ),
        TestCaseCompare(
            text = "Network error: Unable to resolve host 'api.example.com'",
            expectedTokens = setOf("network", "error", "host", "resolve"),
            category = TestCategory.ERROR_HANDLING,
            language = Language.ENGLISH
        )
    )

    private val specialTests = listOf(
        TestCaseCompare(
            text = "API level 34 released in 2023",
            expectedTokens = setOf("api", "level", "released"),
            category = TestCategory.SPECIAL_CASES,
            language = Language.ENGLISH
        ),
        TestCaseCompare(
            text = "implementation 'androidx.compose.ui:ui:1.5.0'",
            expectedTokens = setOf("implementation", "androidx", "compose", "ui"),
            category = TestCategory.SPECIAL_CASES,
            language = Language.ENGLISH
        )
    )

    private val performanceTests = generatePerformanceTests()

    private fun generatePerformanceTests(): List<TestCaseCompare> {
        val baseTexts = listOf(
            "Android Development",
            "Kotlin Programming",
            "Mobile Architecture",
            "UI Implementation",
            "Data Processing"
        )

        return baseTexts.map { baseText ->
            TestCaseCompare(
                text = generateRepeatingText(baseText, 40),
                expectedTokens = baseText.lowercase().split(" ").toSet(),
                category = TestCategory.PERFORMANCE,
                expectedProcessingTime = 500, // ms
                language = Language.ENGLISH
            )
        }
    }

    private fun generateRepeatingText(base: String, repetitions: Int): String {
        return buildString {
            repeat(repetitions) {
                append(base)
                append(" ")
            }
        }.trim()
    }

    suspend fun runTests(
        analyzer: ITextAnalyzer,
        onProgress: suspend (String, Float, String) -> Unit
    ): Map<TestCategory, List<TestResultCompare>> {
        val allTests = mapOf(
            TestCategory.TECHNICAL to technicalTests,
            TestCategory.SEARCH to searchTests,
            TestCategory.MIXED_LANGUAGE to mixedLanguageTests,
            TestCategory.ERROR_HANDLING to errorTests,
            TestCategory.SPECIAL_CASES to specialTests,
            TestCategory.PERFORMANCE to performanceTests
        )

        return allTests.mapValues { (category, tests) ->
            runTestCategory(
                analyzer = analyzer,
                tests = tests,
                onProgress = { progress, currentTest ->
                    onProgress(category.name, progress, currentTest)
                }
            )
        }
    }

    private suspend fun runTestCategory(
        analyzer: ITextAnalyzer,
        tests: List<TestCaseCompare>,
        onProgress: suspend (Float, String) -> Unit
    ): List<TestResultCompare> {
        return tests.mapIndexed { index, test ->
            val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val startTime = System.nanoTime()

            val analysis = analyzer.analyzeText(test.text)

            val endTime = System.nanoTime()
            val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

            onProgress(
                index.toFloat() / tests.size,
                test.text.take(50) + if (test.text.length > 50) "..." else ""
            )

            TestResultCompare(
                case = test,
                processingTime = (endTime - startTime) / 1_000_000, // to ms
                memoryUsed = endMemory - startMemory,
                foundTokens = analysis.importantWords.map { it.token }.toSet(),
                tokenWeights = analysis.importantWords.associate { it.token to it.weight }
            )
        }
    }
}
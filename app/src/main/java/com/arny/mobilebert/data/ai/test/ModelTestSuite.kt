package com.arny.mobilebert.data.ai.test

import com.arny.mobilebert.data.ai.models.TestCase
import com.arny.mobilebert.data.ai.models.TestCategory

class ModelTestSuite {
    // 1. Технические тесты
    val technicalTests = listOf(
        // Архитектурные паттерны
        TestCase(
            text = "MVVM архитектура приложения",
            expectedTokens = 3,
            expectedImportantWords = setOf("mvvm", "архитектура"),
            category = TestCategory.TECHNICAL
        ),
        TestCase(
            text = """
            Clean Architecture в Android приложении:
            - Presentation layer (MVVM)
            - Domain layer (Use Cases)
            - Data layer (Repository)
            """.trimIndent(),
            expectedTokens = 12,
            expectedImportantWords = setOf("clean", "architecture", "mvvm", "repository"),
            category = TestCategory.TECHNICAL
        ),

        // Kotlin-специфичные
        TestCase(
            text = """
            Корутины в Kotlin:
            suspend fun loadData() = coroutineScope {
                val result = async { api.getData() }
                result.await()
            }
            """.trimIndent(),
            expectedTokens = 15,
            expectedImportantWords = setOf("корутины", "suspend", "coroutinescope", "async"),
            category = TestCategory.TECHNICAL
        ),

        // Android компоненты
        TestCase(
            text = """
            Lifecycle-aware ViewModels используют StateFlow:
            private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
            val uiState: StateFlow<UiState> = _uiState.asStateFlow()
            """.trimIndent(),
            expectedTokens = 18,
            expectedImportantWords = setOf("lifecycle", "viewmodels", "stateflow"),
            category = TestCategory.TECHNICAL
        )
    )

    // 2. Поисковые тесты
    val searchTests = listOf(
        // Простой поиск
        TestCase(
            text = "как реализовать MVVM в Android приложении",
            expectedImportantWords = setOf("mvvm", "android", "реализовать"),
            category = TestCategory.SEARCH
        ),

        // Сложный поиск
        TestCase(
            text = "медленная прокрутка списка с картинками в RecyclerView",
            expectedImportantWords = setOf("медленная", "recyclerview", "список", "картинки"),
            category = TestCategory.SEARCH
        ),

        // Контекстный поиск
        TestCase(
            text = "утечки памяти при загрузке изображений",
            expectedImportantWords = setOf("утечки", "память", "изображения"),
            category = TestCategory.SEARCH
        )
    )

    // 3. Смешанные языковые тесты
    val mixedLanguageTests = listOf(
        TestCase(
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
            expectedImportantWords = setOf("composable", "mainscreen", "column", "topbar"),
            category = TestCategory.MIXED_LANGUAGE
        ),

        TestCase(
            text = "git commit -m 'Исправлен краш при deep linking'",
            expectedImportantWords = setOf("crash", "deep", "linking"),
            category = TestCategory.MIXED_LANGUAGE
        )
    )

    // 4. Обработка ошибок
    val errorTests = listOf(
        TestCase(
            text = """
            E/AndroidRuntime: FATAL EXCEPTION: main
                Process: com.example.app, PID: 1234
                java.lang.IllegalStateException: Fragment UserProfileFragment not attached to an activity
                at androidx.fragment.app.Fragment.requireActivity(Fragment.java:805)
            """.trimIndent(),
            expectedImportantWords = setOf("fatal", "exception", "fragment", "illegalstateexception"),
            category = TestCategory.ERROR_HANDLING
        ),

        TestCase(
            text = "Network error: Unable to resolve host 'api.example.com'",
            expectedImportantWords = setOf("network", "error", "host"),
            category = TestCategory.ERROR_HANDLING
        )
    )

    // 5. Специальные случаи
    val specialTests = listOf(
        TestCase(
            text = "compileSdk = 34",
            expectedTokens = 3,
            category = TestCategory.SPECIAL_CASES
        ),

        TestCase(
            text = "implementation 'androidx.compose.ui:ui:1.5.0'",
            expectedImportantWords = setOf("implementation", "compose", "ui"),
            category = TestCategory.SPECIAL_CASES
        ),

        TestCase(
            text = "🚀 New Release v2.0.0 🎉",
            expectedImportantWords = setOf("release"),
            category = TestCategory.SPECIAL_CASES
        )
    )

    // 6. Тесты производительности
    val performanceTests = listOf(
        TestCase(
            text = generateLongText(1000),
            expectedProcessingTime = 500,
            category = TestCategory.PERFORMANCE
        ),
        TestCase(
            text = generateRepeatingText("Android Development", 100),
            expectedTokens = 200,
        )
    )

    // 7. Тесты словаря
    val vocabTests = listOf(
        TestCase(
            expectedTokenSequence = listOf("[CLS]", "[SEP]", "[PAD]", "[UNK]"),
            category = TestCategory.VOCAB_TOKEN,
        ),
        TestCase(
            expectedTokenSequence = listOf("текст", "с", "пробелами", "preprocessing", "embeddings", "unknownword"),
            category = TestCategory.VOCAB_COMMON,
        ),
        TestCase(
            expectedTokenSequence = listOf("##pre", "##cess", "##ing", "##pro", "##ces", "##sing"),
            category = TestCategory.VOCAB_SUBWORD,
        ),
    )

    // 7. Тесты токенизации
    val tokenizationTests = listOf(
        TestCase(
            text = "текст с пробелами",
            expectedTokens = 7, // 5 + 2 (CLS and SEP)
            expectedTokenSequence = listOf("[CLS]", "текст", "с", "пробелами", "[SEP]"),
            expectedInputIds = listOf(101L, 1023L, 1033L, 1024L, 1025L, 0L, 0L),
            expectedMaskIds = listOf(1L, 1L, 1L, 1L, 1L, 0L, 0L),
            expectedTypeIds = listOf(0L, 0L, 0L, 0L, 0L, 0L, 0L),
            category = TestCategory.TOKENIZATION,
            description = "Basic tokenization with spaces and padding"
        ),
        TestCase(
            text = "preprocessing",
            expectedTokens = 6,
            expectedTokenSequence = listOf("[CLS]", "pre", "##pre", "##cess", "##ing", "[SEP]"),
            expectedInputIds = listOf(101L, 2345L, 3456L, 4567L, 5678L, 1025L),
            expectedMaskIds = listOf(1L, 1L, 1L, 1L, 1L, 1L),
            expectedTypeIds = listOf(0L, 0L, 0L, 0L, 0L, 0L),
            category = TestCategory.TOKENIZATION,
            description = "Tokenization of a single word with subwords"
        ),
    )

    fun getAllTests(): List<Pair<String, List<TestCase>>> = listOf(
//        "Technical Analysis" to technicalTests,
//        "Search Capabilities" to searchTests,
//        "Mixed Language Processing" to mixedLanguageTests,
//        "Error Handling" to errorTests,
//        "Special Cases" to specialTests,
//        "Performance" to performanceTests,
        "Tokenization" to tokenizationTests
    )

    private fun generateLongText(words: Int): String {
        val vocabulary = listOf(
            "Android", "Kotlin", "development", "application", "architecture",
            "testing", "performance", "optimization", "implementation", "component"
        )
        return buildString {
            repeat(words) {
                append(vocabulary.random())
                append(" ")
            }
        }.trim()
    }

    private fun generateRepeatingText(base: String, times: Int): String {
        return buildString {
            repeat(times) {
                append(base)
                append(" ")
            }
        }.trim()
    }
}
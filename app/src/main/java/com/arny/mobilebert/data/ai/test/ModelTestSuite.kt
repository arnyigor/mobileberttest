package com.arny.mobilebert.data.ai.test

import com.arny.mobilebert.data.ai.models.TestCase
import com.arny.mobilebert.data.ai.models.TestCategory
import com.arny.mobilebert.data.ai.models.TokenizeResult

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

    // 7. Тесты токенизации
    val tokenizationTests = listOf(
        TestCase(
            text = "текст с пробелами",
            expectedTokens = 5,
            expectedTokenSequence = listOf(
                "[CLS]", "текст", "с", "пробелами", "[SEP]"
            ),
            category = TestCategory.BASE_TOKENIZATION
        ),
        TestCase(
            text = "preprocessing",
            expectedTokens = 3,
            expectedTokenSequence = listOf(
                "[CLS]", "preprocessing", "[SEP]"
            ),
            category = TestCategory.BASE_TOKENIZATION
        ),
        TestCase(
            text = "embeddings",
            expectedTokens = 3,
            expectedTokenSequence = listOf(
                "[CLS]", "embeddings", "[SEP]"
            ),
            category = TestCategory.BASE_TOKENIZATION
        )
    )

    // Новые тестовые случаи
    val extendedTokenizationTests = listOf(
        TestCase(
            text = "Фильм \"Начало\" с Леонардо Ди Каприо",
            expectedTokens = 9,
            expectedTokenSequence = listOf(
                "[CLS]", "фильм", "\"", "начало", "\"", "с", "леонардо", "ди", "каприо", "[SEP]"
            ),
            expectedInputIds = listOf(2L, 5L, 6L, 7L, 6L, 8L, 9L, 10L, 11L, 3L),
            expectedMaskIds = listOf(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L),
            expectedTypeIds = listOf(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L),
            expectedImportantWords = setOf("начало", "леонардо", "ди", "каприо"),
            category = TestCategory.EXTENDED_TOKENIZATION,
            description = "Тестирование запроса с названием фильма и именем актера"
        ),
        TestCase(
            text = "Комедия с Адамом Сэндлером",
            expectedTokens = 6,
            expectedTokenSequence = listOf(
                "[CLS]", "комедия", "с", "адамом", "сэндлером", "[SEP]"
            ),
            expectedInputIds = listOf(2L, 12L, 8L, 13L, 14L, 3L),
            expectedMaskIds = listOf(1L, 1L, 1L, 1L, 1L, 1L),
            expectedTypeIds = listOf(0L, 0L, 0L, 0L, 0L, 0L),
            expectedImportantWords = setOf("адамом", "сэндлером"),
            category = TestCategory.EXTENDED_TOKENIZATION,
            description = "Тестирование запроса с именем актера"
        ),
        TestCase(
            text = "Режиссер Мартин Скорсезе",
            expectedTokens = 5,
            expectedTokenSequence = listOf(
                "[CLS]", "режиссер", "мартин", "скорсезе", "[SEP]"
            ),
            expectedImportantWords = setOf("мартин", "скорсезе"),
            category = TestCategory.EXTENDED_TOKENIZATION,
            description = "Тестирование запроса с именем режиссера"
        ),
        TestCase(
            text = "Драма о любви и предательстве",
            expectedTokens = 7,
            expectedTokenSequence = listOf(
                "[CLS]", "драма", "о", "любви", "и", "предательстве", "[SEP]"
            ),
            expectedImportantWords = setOf("драма", "любви", "предательстве"),
            category = TestCategory.EXTENDED_TOKENIZATION,
            description = "Тестирование запроса с описанием фильма"
        ),
        TestCase(
            text = "Фильм с элементами фантастики и экшена",
            expectedTokens = 8,
            expectedTokenSequence = listOf(
                "[CLS]", "фильм", "с", "элементами", "фантастики", "и", "экшена", "[SEP]"
            ),
            expectedImportantWords = setOf("фантастики", "экшена"),
            category = TestCategory.EXTENDED_TOKENIZATION,
            description = "Тестирование запроса с жанрами фильма"
        ),
        TestCase(
            text = "Семейный фильм с Джуди Денч",
            expectedTokens = 6,
            expectedTokenSequence = listOf(
                "[CLS]", "семейный", "фильм", "с", "джуди", "денч", "[SEP]"
            ),
            expectedImportantWords = setOf("джуди", "денч"),
            category = TestCategory.EXTENDED_TOKENIZATION,
            description = "Тестирование запроса с именем актрисы"
        )
    )

    fun getAllTests(): List<Pair<String, List<TestCase>>> = listOf(
        "Technical Analysis" to technicalTests,
        "Search Capabilities" to searchTests,
        "Mixed Language Processing" to mixedLanguageTests,
        "Error Handling" to errorTests,
        "Special Cases" to specialTests,
        "Performance" to performanceTests,
        "Tokenization" to tokenizationTests
    )

    // Вспомогательная функция для проверки токенизации
    fun validateTokenization(
        result: TokenizeResult,
        testCase: TestCase
    ): List<String> {
        val errors = mutableListOf<String>()

        // Проверка количества токенов
        testCase.expectedTokens?.let { expected ->
            if (result.tokens.size != expected) {
                errors.add(
                    "Token count mismatch: expected $expected, got ${result.tokens.size}"
                )
            }
        }

        // Проверка последовательности токенов
        testCase.expectedTokenSequence?.let { expected ->
            if (result.tokens != expected) {
                errors.add(
                    "Token sequence mismatch:\nexpected: $expected\ngot: ${result.tokens}"
                )
            }
        }

        // Проверка соответствия массивов
        if (result.inputIds.size != result.tokens.size ||
            result.maskIds.size != result.tokens.size ||
            result.typeIds.size != result.tokens.size
        ) {
            errors.add("Arrays size mismatch")
        }

        return errors
    }

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
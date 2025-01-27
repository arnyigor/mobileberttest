package com.arny.mobilebert.data.search

import android.content.Context
import android.util.Log
import com.arny.mobilebert.data.ai.models.ImportantWord
import com.arny.mobilebert.domain.ai.ITextAnalyzer
import javax.inject.Inject

class SmartSearchManager @Inject constructor(
    private val context: Context
) {
    private var textBlocks: List<TextBlock> = emptyList()
    private val keywordIndex = mutableMapOf<String, MutableList<Int>>() // слово -> индексы блоков

    fun loadContent() {
        val processor = TextFileProcessor(context)
        textBlocks = processor.loadTextFile("android_best_practice.txt")
        buildIndex()
    }

    private fun buildIndex() {
        textBlocks.forEachIndexed { index, block ->
            // Разбиваем текст на слова и создаем индекс
            block.text.lowercase()
                .split(Regex("[\\s,.!?()\\[\\]{}]+"))
                .filter { it.length > 2 }
                .forEach { word ->
                    keywordIndex.getOrPut(word) { mutableListOf() }.add(index)
                }
        }
    }

    fun search(query: String, bertAnalyzer: ITextAnalyzer): List<SearchResult> {
        val searchStart = System.currentTimeMillis()
        Log.d("SmartSearchManager", "search: query:$query")
        Log.d("SmartSearchManager", "textBlocks: size:${textBlocks.size}")


        val queryWords = query.lowercase()
            .split(Regex("[\\s,.!?()\\[\\]{}]+"))
            .filter { it.length > 2 }
        // Ищем блоки, содержащие слова запроса
        val matchingBlocks = mutableMapOf<Int, Int>() // blockIndex -> matches count

        queryWords.forEach { word ->
            // Прямой поиск
            keywordIndex[word]?.forEach { blockIndex ->
                matchingBlocks[blockIndex] = (matchingBlocks[blockIndex] ?: 0) + 1
            }

            // Поиск по основе слова
            val wordRoot = word.take(word.length - 2)
            keywordIndex.filter { (key, _) ->
                key.startsWith(wordRoot)
            }.forEach { (_, blockIndices) ->
                blockIndices.forEach { blockIndex ->
                    matchingBlocks[blockIndex] = (matchingBlocks[blockIndex] ?: 0) + 1
                }
            }

            // Поиск по синонимам
            getSynonyms(word).forEach { synonym ->
                keywordIndex[synonym]?.forEach { blockIndex ->
                    matchingBlocks[blockIndex] = (matchingBlocks[blockIndex] ?: 0) + 1
                }
            }
        }

        val results = matchingBlocks.map { (blockIndex, matches) ->
            val score = matches.toFloat() / queryWords.size
            SearchResult(
                block = textBlocks[blockIndex],
                score = score,
                relevance = String.format("%.1f%%", score * 100)
            )
        }
            .filter { it.score > 0.3f }
            .sortedByDescending { it.score }
            .take(5)

        val searchTime = System.currentTimeMillis() - searchStart
        Log.d("Search", "Search completed in $searchTime ms")

        return results
    }

    private fun getSynonyms(word: String): Set<String> {
        return when (word) {
            "mvp" -> setOf("model", "view", "presenter", "паттерн", "архитектура")
            "mvvm" -> setOf("model", "view", "viewmodel", "паттерн", "архитектура")
            "архитектура" -> setOf("паттерн", "шаблон", "структура")
            "особенности" -> setOf("преимущества", "характеристики", "возможности")
            // Добавьте другие синонимы
            else -> emptySet()
        }
    }

    private fun calculateRelevance(
        queryWords: List<ImportantWord>,
        blockWords: List<ImportantWord>
    ): Float {
        // Создаем мапы слово → вес
        val queryWordMap = queryWords.associate {
            it.token.lowercase() to it.weight.coerceIn(0f, 1f)
        }

        var totalScore = 0f
        var totalWords = queryWordMap.size

        queryWordMap.forEach { (queryWord, queryWeight) ->
            // Ищем похожие слова в блоке
            val bestMatch = blockWords.maxOfOrNull { blockWord ->
                if (areWordsSimilar(queryWord, blockWord.token)) {
                    queryWeight * blockWord.weight
                } else {
                    0f
                }
            } ?: 0f

            totalScore += bestMatch
        }

        return if (totalWords > 0) {
            (totalScore / totalWords).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    private fun areWordsSimilar(word1: String, word2: String): Boolean {
        val w1 = word1.lowercase()
        val w2 = word2.lowercase()

        // Прямое совпадение
        if (w1 == w2) return true

        // Проверка на части слов
        if (w1.length > 3 && w2.length > 3) {
            if (w1.contains(w2) || w2.contains(w1)) return true
        }

        // Связанные слова и синонимы
        val relatedWords = mapOf(
            "особенности" to setOf("преимущества", "характеристики", "возможности", "специфика"),
            "архитектура" to setOf("mvvm", "mvc", "mvp", "паттерн", "шаблон", "подход"),
            "паттерн" to setOf("pattern", "шаблон", "архитектура", "подход"),
            "mvp" to setOf("model", "view", "presenter", "паттерн", "архитектура", "шаблон"),
            "mvvm" to setOf("model", "view", "viewmodel", "паттерн", "архитектура"),
            "данные" to setOf("data", "информация", "содержимое"),
            "приложение" to setOf("app", "программа", "система")
        )

        // Проверяем связанные слова в обоих направлениях
        relatedWords.forEach { (key, values) ->
            if ((w1.contains(key) && values.any { w2.contains(it) }) ||
                (w2.contains(key) && values.any { w1.contains(it) })) {
                return true
            }
        }

        return false
    }

}
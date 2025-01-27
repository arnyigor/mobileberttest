package com.arny.mobilebert.data.ai.tokenizers

import android.content.Context
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITokenizer

class BertTokenizer(
    context: Context,
    config: ModelConfig,
    modelFileManager: ModelFileManager
) : BaseTokenizer(context, config, modelFileManager), ITokenizer {

    override fun tokenize(text: String): TokenizeResult {
        // Разбиваем на слова, сохраняя регистр для английских слов
        val words = text.split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { word ->
                // Приводим к нижнему регистру только русские слова
                if (word.any { it in 'а'..'я' || it in 'А'..'Я' }) {
                    word.lowercase()
                } else {
                    word
                }
            }

        val wordIds = mutableListOf<Long>()
        val tokens = mutableListOf<String>()

        // Добавляем [CLS]
        wordIds.add(specialTokens["[CLS]"]!!.toLong())
        tokens.add("[CLS]")

        // Обрабатываем каждое слово
        words.forEach { word ->
            // Пробуем найти слово целиком
            val id = vocab[word] ?: run {
                // Если не нашли, пробуем в нижнем регистре
                vocab[word.lowercase()] ?: run {
                    // Если все еще не нашли, разбиваем на подтокены
                    val subTokens = tokenizeWord(word)
                    if (subTokens.isNotEmpty()) {
                        subTokens.first()
                    } else {
                        specialTokens["[UNK]"]!!
                    }
                }
            }
            wordIds.add(id.toLong())
            tokens.add(word)
        }

        // Добавляем [SEP]
        wordIds.add(specialTokens["[SEP]"]!!.toLong())
        tokens.add("[SEP]")

        return TokenizeResult(
            inputIds = wordIds.toLongArray(),
            maskIds = LongArray(wordIds.size) { 1L },
            typeIds = LongArray(wordIds.size) { 0L },
            tokens = tokens
        )
    }

    private fun tokenizeWord(word: String): List<Int> {
        // Разбиваем слово на подтокены
        var remainingWord = word
        val tokens = mutableListOf<Int>()

        while (remainingWord.isNotEmpty()) {
            var longestMatch: String? = null
            var longestMatchId: Int? = null

            // Ищем самый длинный подтокен
            for (i in remainingWord.length downTo 0) {
                val subWord = remainingWord.substring(0, i)
                val id = vocab[subWord] ?: vocab["##$subWord"]
                if (id != null) {
                    longestMatch = subWord
                    longestMatchId = id
                    break
                }
            }

            if (longestMatch != null && longestMatchId != null) {
                tokens.add(longestMatchId)
                remainingWord = remainingWord.substring(longestMatch.length)
                if (remainingWord.isNotEmpty()) {
                    remainingWord = "##$remainingWord"
                }
            } else {
                break
            }
        }

        return tokens
    }
}
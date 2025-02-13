package com.arny.mobilebert.data.ai.tokenizers

import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITokenizer

class LabseEnRuTokenizer(
    config: ModelConfig,
    modelFileManager: ModelFileManager
) : BaseTokenizer(   config, modelFileManager), ITokenizer {
    private val unkToken = "[UNK]"

    override fun tokenize(text: String): TokenizeResult {
        val tokens = text.lowercase()
            .replace("[^a-zа-яё0-9 ]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }

//        Log.d("BertTokenizer", "tokens: $tokens")

        // Получаем ID для всех слов
        val wordIds = tokens.map { word ->
            (vocab[word] ?: vocab[unkToken]!!).toLong()
        }

//        Log.d("BertTokenizer", "Word IDs: ${wordIds.joinToString()}")

        return TokenizeResult(
            inputIds = wordIds.toLongArray(),
            maskIds = LongArray(wordIds.size) { 1L },
            typeIds = LongArray(wordIds.size) { 0L },
            tokens = tokens
        )
    }
}
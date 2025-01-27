package com.arny.mobilebert.data.ai.tokenizers

import android.content.Context
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITokenizer

class RuBertTiny2Tokenizer(
    context: Context,
    config: ModelConfig,
    modelFileManager: ModelFileManager
) : BaseTokenizer(context, config, modelFileManager), ITokenizer {

    override fun tokenize(text: String): TokenizeResult {
        // Предобработка текста
        val preprocessedTokens = preprocessText(text)

        // Добавление специальных токенов
        val tokens = listOf("[CLS]") + preprocessedTokens + listOf("[SEP]")

        // Получение ID токенов
        val wordIds = tokens.map { token ->
            getTokenId(token)
        }

        // Создание маски и типов
        val maskIds = LongArray(wordIds.size) { 1L }
        val typeIds = LongArray(wordIds.size) { 0L }

        return TokenizeResult(
            inputIds = wordIds.map { it.toLong() }.toLongArray(),
            maskIds = maskIds,
            typeIds = typeIds,
            tokens = tokens
        )
    }

    private fun preprocessText(text: String): List<String> {
        return text.lowercase()
            .replace("[^a-zа-яё0-9 \"\']".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
    }
}
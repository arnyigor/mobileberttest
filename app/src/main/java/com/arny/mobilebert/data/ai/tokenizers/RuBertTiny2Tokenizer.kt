package com.arny.mobilebert.data.ai.tokenizers

import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITokenizer

class RuBertTiny2Tokenizer(
    config: ModelConfig,
    private val modelFileManager: ModelFileManager
) : BaseTokenizer(config, modelFileManager), ITokenizer {

    override fun tokenize(text: String): TokenizeResult {
        val tokens = sentenceTokenizer.tokenize(text)

        val wordIds = tokens.map { word ->
            (vocab[word] ?: vocab["[UNK]"]!!).toLong()
        }

        return TokenizeResult(
            inputIds = wordIds.toLongArray(),
            maskIds = LongArray(wordIds.size) { 1L },
            typeIds = LongArray(wordIds.size),
            tokens = tokens
        )
    }
}
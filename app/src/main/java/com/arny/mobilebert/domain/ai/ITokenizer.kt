package com.arny.mobilebert.domain.ai

import com.arny.mobilebert.data.ai.models.TokenizeResult

interface ITokenizer {
    fun tokenize(text: String): TokenizeResult
    fun getVocabMap(): Map<String, Int> = emptyMap()
}

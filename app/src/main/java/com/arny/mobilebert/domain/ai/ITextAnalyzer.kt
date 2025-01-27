package com.arny.mobilebert.domain.ai

import com.arny.mobilebert.data.ai.models.TextAnalysis
import com.arny.mobilebert.data.ai.models.ModelTensorsInfo

interface ITextAnalyzer {
    val isReused: Boolean
    suspend fun analyzeText(text: String): TextAnalysis
    fun getModelName(): String
    fun getTokenizer(): ITokenizer
    fun initialize()
    fun close()
    fun getTensorsInfo(): ModelTensorsInfo
}

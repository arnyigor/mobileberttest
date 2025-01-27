package com.arny.mobilebert.data.ai.models

data class TextAnalysis(
    val tokens: List<String>,
    val importantWords: List<ImportantWord>,
    val attentionScore: Double
)

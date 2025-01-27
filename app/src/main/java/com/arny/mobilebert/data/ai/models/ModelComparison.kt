package com.arny.mobilebert.data.ai.models

data class ModelComparison(
    val bestOverall: String,
    val bestForTechnicalTexts: String,
    val bestForMixedLanguage: String,
    val bestForSearch: String,
    val recommendations: List<String>,
    val detailedComparison: Map<String, ComparisonMetrics>
)
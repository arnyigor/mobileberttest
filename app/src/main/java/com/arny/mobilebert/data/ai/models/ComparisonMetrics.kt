package com.arny.mobilebert.data.ai.models

data class ComparisonMetrics(
    val overallScore: Float,
    val performanceScore: Float,
    val accuracyScore: Float,
    val memoryScore: Float,
    val languageScore: Float,
    val categoryScores: Map<TestCategory, Float>
)
package com.arny.mobilebert.data.ai.models

data class ComparisonResult(
    val modelName: String,
    val metrics: ModelMetrics,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val testResults: Map<TestCategory, List<TestResultCompare>>
)
package com.arny.mobilebert.data.ai.models

data class TestResult(
    val averageProcessingTime: Float,
    val averageTokens: Float,
    val tokensCount: Int,
    val memoryUsage: Long,
    val analysisTime: Long,
    val errors: List<String> = emptyList()
)
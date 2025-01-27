package com.arny.mobilebert.data.ai.models

data class TestResultCompare(
    val case: TestCaseCompare,
    val processingTime: Long,
    val memoryUsed: Long,
    val foundTokens: Set<String>,
    val tokenWeights: Map<String, Float>
)

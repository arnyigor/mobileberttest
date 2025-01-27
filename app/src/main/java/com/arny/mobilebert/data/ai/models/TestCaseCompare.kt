package com.arny.mobilebert.data.ai.models

data class TestCaseCompare(
    val text: String,
    val expectedTokens: Set<String>,
    val category: TestCategory,
    val expectedProcessingTime: Long? = null,
    val expectedMemoryUsage: Long? = null,
    val language: Language = Language.MIXED
)
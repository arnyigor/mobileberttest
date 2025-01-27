package com.arny.mobilebert.data.ai.models

data class TestCase(
    val text: String,
    val expectedTokens: Int? = null,
    val expectedImportantWords: Set<String>? = null,
    val expectedProcessingTime: Long? = null,
    val expectedMemoryUsage: Long? = null,
    val category: TestCategory = TestCategory.GENERAL,
    val expectedTokenSequence: List<String>? = null,
    val description: String? = null,
    val expectedInputIds: List<Long>? = null,
    val expectedMaskIds: List<Long>? = null,
    val expectedTypeIds: List<Long>? = null,
)
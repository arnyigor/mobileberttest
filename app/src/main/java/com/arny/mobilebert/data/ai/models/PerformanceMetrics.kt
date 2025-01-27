package com.arny.mobilebert.data.ai.models

data class PerformanceMetrics(
    val averageProcessingTime: Float,
    val maxProcessingTime: Float,
    val minProcessingTime: Float,
    val processingTimeByCategory: Map<TestCategory, Float>,
    val totalProcessingTime: Long
)
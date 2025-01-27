package com.arny.mobilebert.data.ai.models

data class MemoryMetrics(
    val peakMemoryUsage: Long,
    val averageMemoryPerToken: Float,
    val memoryEfficiency: Float,
    val memoryByCategory: Map<TestCategory, Long>
)
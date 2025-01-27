package com.arny.mobilebert.data.ai.models

data class MemoryStatistics(
    val totalTime: Long,
    val totalNativeHeap: Long,
    val totalJavaHeap: Long,
    val minTime: Long,
    val maxTime: Long,
    val minMemory: Long,
    val maxMemory: Long
)
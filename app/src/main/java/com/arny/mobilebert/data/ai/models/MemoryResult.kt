package com.arny.mobilebert.data.ai.models

data class MemoryResult(
    val nativeHeap: Long,
    val javaHeap: Long,
    val totalMemory: Long,
    val memoryUsage: Float, // процент использования памяти
    val nativeUsage: Float  // процент использования нативной памяти
)
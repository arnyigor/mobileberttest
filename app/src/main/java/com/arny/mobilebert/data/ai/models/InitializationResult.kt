package com.arny.mobilebert.data.ai.models

data class InitializationResult(
    val iteration: Int,
    val time: Long,
    val memoryInfo: MemoryResult,
    val rawMemoryInfo: MemoryInfo
)


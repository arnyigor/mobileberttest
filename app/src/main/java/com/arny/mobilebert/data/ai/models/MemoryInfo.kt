package com.arny.mobilebert.data.ai.models

data class MemoryInfo(
    val nativeHeap: Long,
    val javaHeap: Long,
    val total: Long,
    val nativeHeapSize: Long,
    val nativeHeapFreeSize: Long,
    val nativeHeapAllocatedSize: Long,
    val totalPss: Long,
    val nativePss: Long,
    val dalvikPss: Long,
    val allocated: Long,
    val free: Long,
    val maxMemory: Long
)
package com.arny.mobilebert.data.ai.test

import com.arny.mobilebert.data.ai.models.MemoryInfo
import kotlin.math.roundToInt

class MemoryTracker {
    private val memoryHistory = mutableListOf<MemoryInfo>()

    fun trackMemory(info: MemoryInfo) {
        memoryHistory.add(info)
    }

    fun getMemoryTrend(): String = buildString {
        memoryHistory.forEachIndexed { index, info ->
            appendLine("Run ${index + 1}:")
            appendLine("  Native: ${info.nativeHeap}KB")
            appendLine("  Java: ${info.javaHeap}KB")
            appendLine("  PSS: ${info.totalPss}KB")
            appendLine("  Memory Usage: ${(info.allocated.toFloat() / info.maxMemory * 100).roundToInt()}%")
        }
    }
}
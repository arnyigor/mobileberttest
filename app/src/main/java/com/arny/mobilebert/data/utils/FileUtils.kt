package com.arny.mobilebert.data.utils

import android.content.Context
import android.util.Log

fun getModelSize(context: Context, modelPath: String): Long = try {
    context.assets.openFd(modelPath).use { fd -> fd.declaredLength }
} catch (e: Exception) {
    Log.e("FileUtils", "Error getting model size: ${e.message}")
    0L
}

// Форматирование размера в читаемый вид
fun formatFileSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> String.format("%.1f KB", size / 1024f)
    size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024f * 1024f))
    else -> String.format("%.1f GB", size / (1024f * 1024f * 1024f))
}
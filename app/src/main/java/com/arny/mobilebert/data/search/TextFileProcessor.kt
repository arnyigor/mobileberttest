package com.arny.mobilebert.data.search

import android.content.Context
import javax.inject.Inject

class TextFileProcessor @Inject constructor(private val context: Context) {
    fun loadTextFile(fileName: String): List<TextBlock> {
        val text = context.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }

        // Разбиваем текст на блоки (например, по абзацам)
        return text.split("\n\n")
            .filter { it.isNotEmpty() }
            .mapIndexed { index, block ->
                TextBlock(
                    text = block.trim(),
                    startIndex = index * 1000, // Условный индекс для демонстрации
                    endIndex = (index + 1) * 1000 - 1
                )
            }
    }
}
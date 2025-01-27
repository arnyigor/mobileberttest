package com.arny.mobilebert.data.ai.attention

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import com.arny.mobilebert.data.ai.models.ImportantWord
import javax.inject.Inject

class AttentionVisualizer @Inject constructor() {

    fun visualizeAttention(
        words: List<String>,
        importantWords: List<ImportantWord>
    ): SpannableString {
        // Визуализация с подсветкой
        val originalText = words.joinToString(" ")
        val spannableString = SpannableString(originalText)

        // Создаем карту слов и их весов для быстрого доступа
        val wordWeights = importantWords.associate { it.token to it.weight }

        // Проходим по каждому слову в тексте
        var currentPosition = 0
        words.forEach { word ->
            val weight = wordWeights[word] ?: 0f
            val start = currentPosition
            val end = start + word.length

            // Создаем цвет с нужной прозрачностью
            val alpha = (weight * 255).toInt().coerceIn(0, 255)
            val color = Color.argb(alpha, 255, 0, 0) // Красный цвет с прозрачностью

            // Применяем форматирование
            spannableString.setSpan(
                BackgroundColorSpan(color),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Добавляем жирный шрифт для важных слов
            if (weight > 0.5f) {
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Обновляем позицию с учетом пробела
            currentPosition = end + 1
        }

        return spannableString
    }
}
package com.arny.mobilebert.data.ai.attention

import android.util.Log
import com.arny.mobilebert.data.ai.models.ImportantWord
import com.arny.mobilebert.data.ai.models.TextAnalysis

class BertAttentionExtractor {
    fun extract(outputs: Map<Int, Any>, words: List<String>): TextAnalysis {
        // Собираем только значения между 0 и 1
        val validScores = mutableListOf<Pair<Int, Float>>()

        outputs.forEach { (key, value) ->
            if (value is Array<*> && value.size == 1) {
                val firstLevel = value[0]
                if (firstLevel is Array<*> && firstLevel.size == 1) {
                    val score = (firstLevel[0] as? FloatArray)?.firstOrNull()
                    if (score != null && score in 0f..1f) {
                        validScores.add(key to score)
                        Log.d("BertAnalyzer", "Output $key: valid score = $score")
                    }
                }
            }
        }

        // Сортируем по значению score
        val sortedScores = validScores.sortedBy { it.second }

        // Берем только первые N scores, где N - количество слов
        val wordScores = sortedScores
            .take(words.size)
            .map { it.second }

        Log.d("BertAnalyzer", "Word scores before normalization: ${wordScores.joinToString()}")

        // Нормализуем только если есть существенная разница между значениями
        val normalizedScores = if (wordScores.isNotEmpty()) {
            val max = wordScores.maxOrNull() ?: 1f
            val min = wordScores.minOrNull() ?: 0f
            val range = max - min

            if (range > 0.1f) { // Существенная разница
                wordScores.map { score ->
                    ((score - min) / range).coerceIn(0f, 1f)
                }
            } else {
                // Если разница несущественная, оставляем исходные значения
                wordScores
            }
        } else {
            List(words.size) { 0.5f }
        }

        Log.d("BertAnalyzer", "Normalized scores: ${normalizedScores.joinToString()}")

        // Создаем пары слово-вес
        val importantWords = words.zip(normalizedScores)
            .map { (word, score) -> ImportantWord(word, score) }
            .filter { it.weight > 0.1f } // Фильтруем слишком низкие веса
            .sortedByDescending { it.weight }

        return TextAnalysis(
            tokens = words,
            importantWords = importantWords,
            attentionScore = normalizedScores.average()
        )
    }

}
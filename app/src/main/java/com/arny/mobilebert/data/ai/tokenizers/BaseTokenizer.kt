package com.arny.mobilebert.data.ai.tokenizers

import android.content.Context
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITokenizer

abstract class BaseTokenizer(
    protected val context: Context,
    protected val config: ModelConfig,
    private val modelFileManager: ModelFileManager
) : ITokenizer {

    protected val vocab: Map<String, Int> by lazy { loadVocab() }
    protected val specialTokens = mapOf(
        "[PAD]" to 0,
        "[UNK]" to 1,
        "[CLS]" to 2,
        "[SEP]" to 3,
        "[MASK]" to 4
    )

    private fun loadVocab() = modelFileManager.getVocabFile(config).useLines { lines ->
        buildMap {
            lines.forEachIndexed { index, line ->
                if (line.isNotBlank()) {
                    put(line.trim(), index)
                }
            }
        }
    }

    // Общие вспомогательные методы
    protected fun getTokenId(token: String): Int {
        return vocab[token] ?: vocab["[UNK]"] ?: 0
    }

    protected fun checkWord(word: String): Boolean = word in vocab

    // Абстрактный метод, который должны реализовать конкретные токенизаторы
    abstract override fun tokenize(text: String): TokenizeResult
}
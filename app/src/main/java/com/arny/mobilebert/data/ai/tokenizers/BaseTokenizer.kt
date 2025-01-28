package com.arny.mobilebert.data.ai.tokenizers

import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.TokenizeResult
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITokenizer

abstract class BaseTokenizer(
    protected val config: ModelConfig,
    private val modelFileManager: ModelFileManager
) : ITokenizer {
    protected val sentenceTokenizer: SentencePieceTokenizer =
        SentencePieceTokenizer(modelFileManager, config)

    override fun getVocabMap(): Map<String, Int> = vocab

    protected val vocab: Map<String, Int> by lazy { loadVocab() }
    protected val specialTokens by lazy {
        mapOf(
            "[PAD]" to (vocab["[PAD]"] ?: -1),
            "[UNK]" to (vocab["[UNK]"] ?: -1),
            "[CLS]" to (vocab["[CLS]"] ?: -1),
            "[SEP]" to (vocab["[SEP]"] ?: -1),
            "[MASK]" to (vocab["[MASK]"] ?: -1),
        ).filterValues { it != -1 }
    }

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
    protected open fun getTokenId(token: String): Int {
        return vocab[token] ?: vocab["[UNK]"] ?: 0
    }

    protected fun checkWord(word: String): Boolean = word in vocab

    // Абстрактный метод, который должны реализовать конкретные токенизаторы
    abstract override fun tokenize(text: String): TokenizeResult
}
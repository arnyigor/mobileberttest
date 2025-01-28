package com.arny.mobilebert.data.ai.tokenizers

import ai.djl.sentencepiece.SpTokenizer
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.utils.ModelFileManager
import java.io.FileInputStream
import java.io.IOException

class SentencePieceTokenizer(modelFileManager: ModelFileManager, config: ModelConfig) {
    private val tokenizer: SpTokenizer
    init {
        try {
            // Загрузка модели
            modelFileManager.getModelFile(config).let { modelFile ->
                tokenizer = SpTokenizer(FileInputStream(modelFile))
            }
        } catch (e: IOException) {
            // Обработка ошибки загрузки модели
            e.printStackTrace()
            throw RuntimeException("Failed to load SentencePiece model", e)
        }
    }

    fun tokenize(text: String): List<String> {
        return try {
            tokenizer.tokenize(text)
        } catch (e: IOException) {
            // Обработка ошибки токенизации
            e.printStackTrace()
            listOf("[UNK]")
        }
    }
}
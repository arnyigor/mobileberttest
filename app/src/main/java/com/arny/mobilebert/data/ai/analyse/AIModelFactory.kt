package com.arny.mobilebert.data.ai.analyse

import com.arny.mobilebert.data.ai.tokenizers.BertTokenizerMultiLang
import com.arny.mobilebert.data.ai.tokenizers.LabseEnRuTokenizer
import com.arny.mobilebert.data.ai.tokenizers.RuBertTiny2Tokenizer
import com.arny.mobilebert.data.utils.ModelFileManager
import com.arny.mobilebert.domain.ai.ITextAnalyzer
import com.arny.mobilebert.domain.ai.ITokenizer
import javax.inject.Inject

// Фабрика для создания анализаторов и токенизаторов
class AIModelFactory @Inject constructor(
    private val modelFileManager: ModelFileManager
) {
    fun createAnalyzer(config: ModelConfig): ITextAnalyzer {
        return UniversalTextAnalyzer(
            config = config,
            tokenizer = createTokenizer(config),
            modelFileManager = modelFileManager
        )
    }

    fun createTokenizer(config: ModelConfig): ITokenizer {
        return when (config) {
            ModelConfig.BERT_MULTILINGUAL -> BertTokenizerMultiLang(config, modelFileManager)
            ModelConfig.RUBERT_TINY2 -> RuBertTiny2Tokenizer(config, modelFileManager)
            ModelConfig.RUBERT_BASE_CASED -> RuBertTiny2Tokenizer(config, modelFileManager)
            ModelConfig.TINY_BERT -> RuBertTiny2Tokenizer(config, modelFileManager)
            ModelConfig.LABSE_ENRU -> LabseEnRuTokenizer(config, modelFileManager)
        }
    }
}
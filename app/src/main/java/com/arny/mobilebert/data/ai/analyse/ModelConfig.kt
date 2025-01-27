package com.arny.mobilebert.data.ai.analyse

enum class ModelConfig(
    val modelName: String,
    val modelPath: String,
    val vocabPath: String,
    val outputShape: List<Int>,
) {
    BERT_MULTILINGUAL(
        modelName = "bert-base-multilingual-cased",
        modelPath = "bert-base-multilingual-cased.tflite",
        vocabPath = "bert-base-multilingual-cased_vocab.txt",
        outputShape = listOf(1, 768),
    ),
    LABSE_ENRU(
        modelName = "labse-en-ru",
        modelPath = "labse-en-ru.tflite",
        vocabPath = "labse-en-ru_vocab.txt",
        outputShape = listOf(1, 768),
    ),
    RUBERT_BASE_CASED(
        modelName = "rubert-base-cased",
        modelPath = "rubert-base-cased.tflite",
        vocabPath = "rubert-base-cased_vocab.txt",
        outputShape = listOf(1, 768),
    ),
    TINY_BERT(
        modelName = "tinybert_general_4l_312d",
        modelPath = "tinybert_general_4l_312d.tflite",
        vocabPath = "tinybert_general_4l_312d_vocab.txt",
        outputShape = listOf(1, 312),
    ),

    RUBERT_TINY2(
        modelName = "rubert-tiny2",
        modelPath = "rubert-tiny2.tflite",
        vocabPath = "rubert-tiny2_vocab.txt",
        outputShape = listOf(1, 312),
    ),
}
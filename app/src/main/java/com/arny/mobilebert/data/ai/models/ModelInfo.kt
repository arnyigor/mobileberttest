package com.arny.mobilebert.data.ai.models

data class ModelInfo(
    val name: String,
    val modelSize: Long,
    val vocabSize: Long,
    val outputShape: List<Int>
)
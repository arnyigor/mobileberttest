package com.arny.mobilebert.data.ai.models

data class ModelTensorsInfo(
    val inputTensors: List<TensorDetails>,
    val outputTensors: List<TensorDetails>
)
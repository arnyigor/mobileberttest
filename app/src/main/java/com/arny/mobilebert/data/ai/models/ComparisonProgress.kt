package com.arny.mobilebert.data.ai.models

sealed class ComparisonProgress {
    data class ModelStarted(
        val current: Int,
        val total: Int,
        val modelName: String
    ) : ComparisonProgress()

    data class TestProgress(
        val modelName: String,
        val phase: String,
        val progress: Float,
        val currentTest: String
    ) : ComparisonProgress()

    data class ModelCompleted(
        val modelName: String,
        val metrics: ModelMetrics
    ) : ComparisonProgress()
}
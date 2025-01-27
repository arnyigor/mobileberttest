package com.arny.mobilebert.data.ai.models

data class ModelMetrics(
    val performance: PerformanceMetrics,
    val accuracy: AccuracyMetrics,
    val memory: MemoryMetrics,
    val languageSupport: LanguageMetrics
)
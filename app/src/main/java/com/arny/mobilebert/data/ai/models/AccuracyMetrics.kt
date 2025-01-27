package com.arny.mobilebert.data.ai.models

data class AccuracyMetrics(
    val technicalTermsAccuracy: Float,
    val mixedLanguageAccuracy: Float,
    val searchAccuracy: Float,
    val errorAnalysisAccuracy: Float,
    val importantWordsAccuracy: Float,
    val averageAccuracy: Float,
    val accuracyByCategory: Map<TestCategory, Float>
)
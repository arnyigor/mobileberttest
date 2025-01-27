package com.arny.mobilebert.data.ai.models

sealed class TestProgress {
    data class Started(
        val model: String,
        val totalTests: Int
    ) : TestProgress()

    data class Progress(
        val phase: String,
        val current: Int,
        val total: Int,
        val description: String
    ) : TestProgress()

    data class PhaseComplete(
        val phaseName: String,
        val results: TestResult
    ) : TestProgress()

    data class Completed(
        val model: String,
        val finalReport: String
    ) : TestProgress()

    data class Error(val message: String) : TestProgress()
}
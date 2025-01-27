package com.arny.mobilebert.ui.home

import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.ComparisonProgress
import com.arny.mobilebert.data.ai.models.ModelInfo

sealed class HomeUIState {
    object Idle : HomeUIState()
    object SelectingModel : HomeUIState()
    object SelectingVocab : HomeUIState()
    data class Importing(
        val isVocab: Boolean,
        val modelConfig: ModelConfig
    ) : HomeUIState()

    data class Imported(
        val modelInfo: ModelInfo?
    ) : HomeUIState()

    object Loading : HomeUIState()
    data class Progress(
        val progress: ComparisonProgress
    ) : HomeUIState()

    data class Results(
        val text: String
    ) : HomeUIState()

    data class Error(val message: String) : HomeUIState()
}
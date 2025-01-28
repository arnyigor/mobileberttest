package com.arny.mobilebert.domain.ai

import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.data.ai.models.TestCase
import com.arny.mobilebert.data.ai.models.TestProgress
import kotlinx.coroutines.flow.Flow

interface ITestManager {
    suspend fun testModel(modelConfig: ModelConfig): Flow<TestProgress>
    suspend fun testModelInitialization(modelConfig: ModelConfig): Flow<TestProgress>
    suspend fun testMultipleInitializations(modelConfig: ModelConfig, iterations: Int): Flow<TestProgress>
    suspend fun testTokenization(modelConfig: ModelConfig): Flow<TestProgress>
    suspend fun testVocab(modelConfig: ModelConfig): Flow<TestProgress>
}

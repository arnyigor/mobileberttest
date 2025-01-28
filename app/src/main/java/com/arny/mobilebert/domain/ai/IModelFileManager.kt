package com.arny.mobilebert.domain.ai

import com.arny.mobilebert.data.ai.analyse.ModelConfig

interface IModelFileManager {
    fun getModelSize(path: String): Long
    fun isModelComplete(config: ModelConfig): Boolean
    fun isModelDownloaded(config: ModelConfig): Boolean
    fun getModelSizeFull(modelConfig: ModelConfig): Pair<Long, Long>
}
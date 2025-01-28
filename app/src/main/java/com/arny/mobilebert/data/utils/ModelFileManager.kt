package com.arny.mobilebert.data.utils

import com.arny.mobilebert.data.ai.analyse.ModelConfig
import com.arny.mobilebert.domain.ai.IModelFileManager
import java.io.File
import javax.inject.Inject

class ModelFileManager @Inject constructor(
    private val androidAssetManager: AndroidAssetManager
) : IModelFileManager {
    private val modelsDir = androidAssetManager.getModelsDir()

    init {
        modelsDir.mkdirs()
    }

    override fun getModelSize(path: String): Long {
        return androidAssetManager.getFileSize(path)
    }

    override fun isModelComplete(modelConfig: ModelConfig): Boolean {
        val modelDir = File(modelsDir, modelConfig.modelName)
        val modelFile = File(modelDir, modelConfig.modelPath)
        val vocabFile = File(modelDir, modelConfig.vocabPath)

        return modelFile.exists() &&
                vocabFile.exists() &&
                modelFile.length() > 0 &&
                vocabFile.length() > 0
    }

    fun getModelFile(modelConfig: ModelConfig): File {
        return File(File(modelsDir, modelConfig.modelName), modelConfig.modelPath)
    }

    fun getVocabFile(modelConfig: ModelConfig): File {
        return File(File(modelsDir, modelConfig.modelName), modelConfig.vocabPath)
    }

    override fun isModelDownloaded(modelConfig: ModelConfig): Boolean {
        val modelFile = getModelFile(modelConfig)
        val vocabFile = getVocabFile(modelConfig)
        return modelFile.exists() &&
                vocabFile.exists() &&
                modelFile.length() > 0 &&
                vocabFile.length() > 0
    }

   override fun getModelSizeFull(modelConfig: ModelConfig): Pair<Long, Long> {
        val modelFile = getModelFile(modelConfig)
        val vocabFile = getVocabFile(modelConfig)
        return Pair(
            modelFile.length(),
            vocabFile.length()
        )
    }
}
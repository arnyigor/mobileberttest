package com.arny.mobilebert.data.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.arny.mobilebert.data.ai.analyse.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ModelFileManager @Inject constructor(
   private val context: Context
) {
    private val modelsDir = File(context.filesDir, "models")

    init {
        modelsDir.mkdirs()
    }

    // Путь к Downloads
    private val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    suspend fun importModelFile(uri: Uri, modelConfig: ModelConfig) {
        withContext(Dispatchers.IO) {
            val modelDir = File(modelsDir, modelConfig.modelName).apply { mkdirs() }
            val destFile = File(modelDir, modelConfig.modelPath)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    suspend fun importVocabFile(uri: Uri, modelConfig: ModelConfig) {
        withContext(Dispatchers.IO) {
            val modelDir = File(modelsDir, modelConfig.modelName).apply { mkdirs() }
            val destFile = File(modelDir, modelConfig.vocabPath)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun isModelComplete(modelConfig: ModelConfig): Boolean {
        val modelDir = File(modelsDir, modelConfig.modelName)
        val modelFile = File(modelDir, modelConfig.modelPath)
        val vocabFile = File(modelDir, modelConfig.vocabPath)

        return modelFile.exists() &&
                vocabFile.exists() &&
                modelFile.length() > 0 &&
                vocabFile.length() > 0
    }

    fun importModelFromDownloads(modelConfig: ModelConfig): Boolean {
        return try {
            // Создаем директорию для модели если её нет
            val modelDir = File(modelsDir, modelConfig.modelName).apply { mkdirs() }

            // Копируем файл модели
            val sourceModelFile = File(downloadDir, modelConfig.modelPath)
            val destModelFile = File(modelDir, modelConfig.modelPath)
            if (sourceModelFile.exists()) {
                sourceModelFile.copyTo(destModelFile, overwrite = true)
            } else {
                Log.e("ModelFileManager", "Model file not found in Downloads: ${modelConfig.modelPath}")
                return false
            }

            // Копируем файл словаря
            val sourceVocabFile = File(downloadDir, modelConfig.vocabPath)
            val destVocabFile = File(modelDir, modelConfig.vocabPath)
            if (sourceVocabFile.exists()) {
                sourceVocabFile.copyTo(destVocabFile, overwrite = true)
            } else {
                Log.e("ModelFileManager", "Vocab file not found in Downloads: ${modelConfig.vocabPath}")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e("ModelFileManager", "Error importing model files", e)
            false
        }
    }

    fun getModelFile(modelConfig: ModelConfig): File {
        return File(File(modelsDir, modelConfig.modelName), modelConfig.modelPath)
    }

    fun getVocabFile(modelConfig: ModelConfig): File {
        return File(File(modelsDir, modelConfig.modelName), modelConfig.vocabPath)
    }

    fun isModelDownloaded(modelConfig: ModelConfig): Boolean {
        val modelFile = getModelFile(modelConfig)
        val vocabFile = getVocabFile(modelConfig)
        return modelFile.exists() &&
                vocabFile.exists() &&
                modelFile.length() > 0 &&
                vocabFile.length() > 0
    }

    fun getModelSize(modelConfig: ModelConfig): Pair<Long, Long> {
        val modelFile = getModelFile(modelConfig)
        val vocabFile = getVocabFile(modelConfig)
        return Pair(
            modelFile.length(),
            vocabFile.length()
        )
    }

    fun deleteModel(modelConfig: ModelConfig) {
        File(modelsDir, modelConfig.modelName).deleteRecursively()
    }

    fun listDownloadedModels(): List<ModelConfig> {
        return modelsDir.listFiles()?.mapNotNull { modelDir ->
            ModelConfig.values().find { it.modelName == modelDir.name }
        } ?: emptyList()
    }
}
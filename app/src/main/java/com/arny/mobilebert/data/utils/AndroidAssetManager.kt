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

class AndroidAssetManager @Inject constructor(
    private val context: Context
) {
    // Путь к Downloads
    private val downloadDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun getFileSize(modelPath: String): Long = try {
        context.assets.openFd(modelPath).use { fd -> fd.declaredLength }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error getting model size: ${e.message}")
        0L
    }

    fun getModelsDir(): File {
        return File(context.filesDir, "models")
    }

    suspend fun importModelFile(uri: Uri, modelConfig: ModelConfig) {
        withContext(Dispatchers.IO) {
            val modelDir = File(getModelsDir(), modelConfig.modelName).apply { mkdirs() }
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
            val modelDir = File(getModelsDir(), modelConfig.modelName).apply { mkdirs() }
            val destFile = File(modelDir, modelConfig.vocabPath)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }


    fun importModelFromDownloads(modelConfig: ModelConfig): Boolean {
        return try {
            // Создаем директорию для модели если её нет
            val modelDir = File(getModelsDir(), modelConfig.modelName).apply { mkdirs() }

            // Копируем файл модели
            val sourceModelFile = File(downloadDir, modelConfig.modelPath)
            val destModelFile = File(modelDir, modelConfig.modelPath)
            if (sourceModelFile.exists()) {
                sourceModelFile.copyTo(destModelFile, overwrite = true)
            } else {
                Log.e(
                    "ModelFileManager",
                    "Model file not found in Downloads: ${modelConfig.modelPath}"
                )
                return false
            }

            // Копируем файл словаря
            val sourceVocabFile = File(downloadDir, modelConfig.vocabPath)
            val destVocabFile = File(modelDir, modelConfig.vocabPath)
            if (sourceVocabFile.exists()) {
                sourceVocabFile.copyTo(destVocabFile, overwrite = true)
            } else {
                Log.e(
                    "ModelFileManager",
                    "Vocab file not found in Downloads: ${modelConfig.vocabPath}"
                )
                return false
            }

            true
        } catch (e: Exception) {
            Log.e("ModelFileManager", "Error importing model files", e)
            false
        }
    }

}
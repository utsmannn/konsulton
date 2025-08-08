package com.utsman.konsulton.chat.data

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    object Starting : DownloadState()
    data class Downloading(val progress: Int, val downloadedMB: Int, val totalMB: Int) :
        DownloadState()

    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloadService {

    private fun getModelsDirectory(context: Context): File {
        val externalFilesDir = context.getExternalFilesDir(null)
        val modelsDir = if (externalFilesDir != null) {
            File(externalFilesDir, "models")
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "OfflineStoryMaker/models"
            )
        }

        Log.d("ModelDownloadService", "Models directory: ${modelsDir.absolutePath}")
        return modelsDir
    }

    fun downloadModel(
        context: Context,
        model: ModelData
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Starting)

        try {
            val modelsDir = getModelsDirectory(context)
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val requiredSpaceBytes = model.sizeInMB * 1024 * 1024L
            val availableSpaceBytes = modelsDir.freeSpace

            if (availableSpaceBytes < requiredSpaceBytes) {
                emit(DownloadState.Error("Tidak cukup ruang penyimpanan. Diperlukan ${model.sizeInMB} MB, tersedia ${availableSpaceBytes / (1024 * 1024)} MB."))
                return@flow
            }

            val outputFile = File(modelsDir, model.fileName)

            val url = URL(model.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                emit(DownloadState.Error("Server returned HTTP ${connection.responseCode}"))
                return@flow
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(outputFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)

                val progress = if (fileLength > 0) {
                    (total * 100 / fileLength).toInt()
                } else 0

                val downloadedMB = (total / (1024 * 1024)).toInt()
                val totalMB = if (fileLength > 0) {
                    (fileLength / (1024 * 1024))
                } else model.sizeInMB

                emit(DownloadState.Downloading(progress, downloadedMB, totalMB))
            }

            output.flush()
            output.close()
            input.close()
            connection.disconnect()

            emit(DownloadState.Success)

        } catch (e: Exception) {
            val modelsDir = getModelsDirectory(context)
            val outputFile = File(modelsDir, model.fileName)
            if (outputFile.exists()) {
                try {
                    outputFile.delete()
                    Log.d(
                        "ModelDownloadService",
                        "Cleaned up partial download: ${outputFile.absolutePath}"
                    )
                } catch (cleanupException: Exception) {
                    Log.e(
                        "ModelDownloadService",
                        "Failed to cleanup partial download",
                        cleanupException
                    )
                }
            }

            val errorMessage = when {
                e.message?.contains("space", ignoreCase = true) == true ->
                    "Tidak cukup ruang penyimpanan. Hapus beberapa file dan coba lagi."

                e.message?.contains("network", ignoreCase = true) == true ->
                    "Koneksi internet bermasalah. Periksa koneksi dan coba lagi."

                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Download timeout. Periksa koneksi internet dan coba lagi."

                else -> e.message ?: "Download gagal. Coba lagi nanti."
            }

            emit(DownloadState.Error(errorMessage))
        }
    }.flowOn(Dispatchers.IO)

    fun isModelExists(context: Context, fileName: String): Boolean {
        val modelsDir = getModelsDirectory(context)
        val modelFile = File(modelsDir, fileName)
        return modelFile.exists() && modelFile.length() > 0
    }

    fun getModelFile(context: Context, fileName: String): File {
        val modelsDir = getModelsDirectory(context)
        return File(modelsDir, fileName)
    }

    fun deleteModel(context: Context, fileName: String): Boolean {
        val modelsDir = getModelsDirectory(context)
        val modelFile = File(modelsDir, fileName)
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }

    fun getInstalledModels(context: Context): List<ModelData> {
        val modelsDir = getModelsDirectory(context)
        if (!modelsDir.exists()) return emptyList()

        val availableModels = ModelRepository.availableModels
        return availableModels.filter { model ->
            isModelExists(context, model.fileName)
        }
    }
}
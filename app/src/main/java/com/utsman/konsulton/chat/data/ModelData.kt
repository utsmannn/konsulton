package com.utsman.konsulton.chat.data

data class ModelData(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeInMB: Int
)

object ModelRepository {
    val availableModels = listOf(
        ModelData(
            id = "qwen2.5-0.5B-Instruct",
            name = "Qwen2.5-0.5B-Instruct",
            fileName = "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task?download=true",
            sizeInMB = 547
        ),
        ModelData(
            id = "hammer2.1-0.5b",
            name = "Hammer2.1-0.5b",
            fileName = "hammer2p1_05b_.task",
            downloadUrl = "https://huggingface.co/litert-community/Hammer2.1-0.5b/resolve/main/hammer2p1_05b_.task?download=true",
            sizeInMB = 502
        ),
        ModelData(
            id = "DeepSeek-R1-Distill-Qwen-1.5B",
            name = "DeepSeek-R1-Distill-Qwen-1.5B",
            fileName = "deepseek_q8_ekv1280.task",
            downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/deepseek_q8_ekv1280.task?download=true",
            sizeInMB = 2048
        ),
        ModelData(
            id = "SmolVLM-256M-Instruct",
            name = "SmolVLM-256M-Instruct",
            fileName = "smalvlm-256m-instruct_q8_ekv2048_single_image.tflite",
            downloadUrl = "https://huggingface.co/litert-community/SmolVLM-256M-Instruct/resolve/main/smalvlm-256m-instruct_q8_ekv2048_single_image.tflite?download=true",
            sizeInMB = 2048
        )

    )

    fun getModelByFileName(fileName: String): ModelData? {
        return availableModels.find { it.fileName == fileName }
    }
}
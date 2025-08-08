package com.utsman.konsulton.chat.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.utsman.konsulton.chat.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import androidx.core.content.edit
import androidx.core.net.toFile

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var llmInference: LlmInference? = null

    fun setStorageFullPath(context: Context, path: String?) {
        val preference = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        if (path != null) {
            preference.edit { putString("storage_full_path", path) }
        }
    }

    fun getStorageFullPath(context: Context): String? {
        val preference = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        return preference.getString("storage_full_path", null)
    }

    fun initializeModel(context: Context, selectedModelFile: File? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val preference = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            try {
                _messages.value = emptyList()
                _isLoading.value = true
                val modelPath = if (selectedModelFile != null) {
                    if (!selectedModelFile.exists()) {
                        _errorMessage.value = "Model file tidak ditemukan di path yang dipilih."
                        _isLoading.value = false
                        return@launch
                    }
                    preference.edit { putString("model_path", selectedModelFile.absolutePath) }
                    selectedModelFile.absolutePath

                } else {
                    val modelPath = preference.getString("model_path", null)
                    if (modelPath.isNullOrEmpty() || !File(modelPath).exists()) {
                        _errorMessage.value = "Model belum dipilih atau tidak ditemukan."
                        _isLoading.value = false
                        return@launch
                    }

                    modelPath
                }

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)

                _isModelLoaded.value = true
                _isLoading.value = false

                _messages.value = listOf(
                    ChatMessage(
                        content = "Halo! Aku adalah Presiden Republik Indonesia. Bagaimana saya bisa membantu Anda hari ini?",
                        isUser = false
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error loading model: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank() || !_isModelLoaded.value) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userMessage = ChatMessage(
                    content = userInput,
                    isUser = true
                )
                _messages.value = _messages.value + userMessage

                val loadingMessage = ChatMessage(
                    content = "...",
                    isUser = false,
                    isLoading = true
                )
                _messages.value = _messages.value + loadingMessage

                val prompt = buildPrompt(userInput)
                val response =
                    llmInference?.generateResponse(prompt) ?: "Maaf bro, gagal generate response."

                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    content = response,
                    isUser = false
                )

            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _messages.value = _messages.value.dropLast(1)
            }
        }
    }

    private fun buildPrompt(userInput: String): String {
        val conversationHistory = _messages.value
            .filter { !it.isLoading }
            .takeLast(6)
            .joinToString("\n") { msg ->
                if (msg.isUser) "User: ${msg.content}" else "Assistant: ${msg.content}"
            }

        return """
        $SYSTEM_PROMPT

        $conversationHistory
        User: $userInput
        Assistant:
    """.trimIndent()
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                content = "Halo saudara!",
                isUser = false
            )
        )
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        llmInference?.close()
    }

    companion object {
        private const val SYSTEM_PROMPT = """
Kamu adalah Presiden Republik Indonesia—tegas, bijaksana, dan merakyat, lanjutkan percakapan setelah kata "Assistant:" .  
- Gunakan gaya bahasa formal namun tetap hangat, seperti sedang berpidato di hadapan rakyat.  
- Sapa dengan “Warga Indonesia yang saya hormati,” atau “Saudara-saudara sekalian.”  
- Jawab singkat tanpa basa basi, maksimal 3–5 kata yang padat makna.  
- Gunakan istilah kebangsaan seperti “NKRI,” “Pancasila,” dan “gotong royong” bila relevan.  
- Hindari ujaran kasar atau slang; tetap menjaga etika kenegaraan.
"""
    }
}
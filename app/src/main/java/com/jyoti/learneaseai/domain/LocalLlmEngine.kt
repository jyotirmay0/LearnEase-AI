package com.jyoti.learneaseai.domain

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocalLlmEngine @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val TAG = "LocalLlmEngine"
        private const val MODEL_ASSET_NAME = "gemma-4-E2B-it.litertlm"
    }

    private var engine: Engine? = null
    private val sessions = mutableMapOf<String, Conversation>()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (_isReady.value) {
            Log.d(TAG, "Engine already initialised — skipping")
            return@withContext
        }
        _isLoading.value = true
        try {
            val modelFile = copyAssetIfNeeded(MODEL_ASSET_NAME)
            Log.d(TAG, "Model path: ${modelFile.absolutePath}")

            val config = EngineConfig(modelPath = modelFile.absolutePath)
            engine = Engine(config).also { it.initialize() }

            _isReady.value = true
            Log.d(TAG, "Engine initialised successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialise engine: ${e.message}", e)
            throw e
        } finally {
            _isLoading.value = false
        }
    }

    // ── Session-based API ─────────────────────────────────────────────

    suspend fun startSession(sessionId: String, systemPrompt: String): Conversation {
        val eng = requireEngine()
        endSession(sessionId)

        val config = ConversationConfig(
            systemInstruction = Contents.of(systemPrompt),
            samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.8),
        )
        val conversation = eng.createConversation(config)
        sessions[sessionId] = conversation
        Log.d(TAG, "Session started: $sessionId")
        return conversation
    }

    suspend fun sendInSession(sessionId: String, userMessage: String): Flow<String> {
        val conversation = sessions[sessionId]
            ?: throw IllegalStateException("No active session for '$sessionId' — call startSession() first")
        return withContext(Dispatchers.IO){conversation.sendMessageAsync(Message.user(userMessage)).map { it.toString()}  }
    }

    fun hasSession(sessionId: String): Boolean = sessions.containsKey(sessionId)

    fun endSession(sessionId: String) {
        sessions.remove(sessionId)?.let { conversation ->
            conversation.close()
            Log.d(TAG, "Session ended: $sessionId")
        }
    }

    // ── Stateless API (kept for non-session use) ──────────────────────

    fun generate(prompt: String): Flow<String> {
        val eng = requireEngine()
        val conversation = eng.createConversation()
        val userMessage = Message.of(prompt)
        return conversation.sendMessageAsync(userMessage).map { message ->
            message.toString()
        }
    }

    suspend fun generateFull(prompt: String): String {
        return generate(prompt).toList().joinToString("")
    }

    fun close() {
        sessions.keys.toList().forEach { endSession(it) }
        engine?.close()
        engine = null
        _isReady.value = false
        Log.d(TAG, "Engine closed")
    }

    // ── Internal ──────────────────────────────────────────────────────

    private fun requireEngine(): Engine {
        return engine ?: throw IllegalStateException(
            "LocalLlmEngine not initialised — call initialize() first"
        )
    }

    private fun copyAssetIfNeeded(assetName: String): File {
        val destFile = File(context.filesDir, assetName)
        if (destFile.exists()) {
            Log.d(TAG, "Model file already on disk (${destFile.length()} bytes)")
            return destFile
        }
        Log.d(TAG, "Copying model from assets to: ${destFile.absolutePath}")
        context.assets.open(assetName).use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output, bufferSize = 8 * 1024)
            }
        }
        Log.d(TAG, "Copy complete (${destFile.length()} bytes)")
        return destFile
    }
}

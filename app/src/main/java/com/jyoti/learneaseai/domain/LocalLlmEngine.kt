package com.jyoti.learneaseai.domain

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper around LiteRT-LM [Engine] for on-device inference
 * using the Gemma 4 E2B model.
 *
 * Lifecycle:
 *  1. Call [initialize] once (from a coroutine — it's heavy).
 *  2. Use [generate] (streaming) or [generateFull] (blocking).
 *  3. Call [close] when the engine is no longer needed.
 */
class LocalLlmEngine(private val context: Context) {

    companion object {
        private const val TAG = "LocalLlmEngine"
        /** Name of the .litertlm file placed in the assets/ folder */
        private const val MODEL_ASSET_NAME = "gemma-4-E2B-it.litertlm"
    }

    private var engine: Engine? = null

    private val _isReady = MutableStateFlow(false)
    /** True once the model is loaded and ready for inference. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** True while the model is being loaded from assets. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Copy the model from assets to internal storage (if not already there)
     * and initialise the LiteRT-LM engine.
     *
     * Call this on a background thread — model loading can take 5-15 seconds.
     */
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

    /**
     * Generate a streaming response for the given [prompt].
     *
     * @return A [Flow] that emits text tokens as they are generated.
     * @throws IllegalStateException if the engine hasn't been initialised.
     */
    fun generate(prompt: String): Flow<String> {
        val eng = engine ?: throw IllegalStateException(
            "LocalLlmEngine not initialised — call initialize() first"
        )
        val conversation = eng.createConversation()
        val userMessage = Message.of(prompt)
        return conversation.sendMessageAsync(userMessage).map { message ->
            message.toString()
        }
    }

    /**
     * Generate a complete (non-streaming) response for the given [prompt].
     *
     * Collects all tokens from the streaming API and joins them.
     */
    suspend fun generateFull(prompt: String): String {
        return generate(prompt).toList().joinToString("")
    }

    /**
     * Release all native resources held by the engine.
     */
    fun close() {
        engine?.close()
        engine = null
        _isReady.value = false
        Log.d(TAG, "Engine closed")
    }

    // ── Internal ──────────────────────────────────────────────────────

    /**
     * Copy [assetName] from the APK's assets/ directory to the app's
     * internal files directory. Skips the copy if the file already exists
     * with the same size.
     */
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

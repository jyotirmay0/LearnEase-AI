package com.jyoti.learneaseai.data.local.embed

import android.content.Context
import android.util.Log
import com.jyoti.learneaseai.domain.embedding.EmbeddingMath.l2Normalize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalEmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val interpreter: Interpreter
    private val tokenizer = BertTokenizer(context)
    private val inputCount: Int

    init {
        val modelBuffer = context.assets.openFd("all-MiniLM-L6-v2-quant.tflite").let { fd ->
            FileInputStream(fd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
        }
        interpreter = Interpreter(modelBuffer)
        inputCount = interpreter.inputTensorCount
        Log.d("LocalEmbeddingEngine", "Model has $inputCount input tensor(s)")
    }

    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val (inputIds, attentionMask, _) = tokenizer.tokenize(text)

        // Output shape is [1, 384] — the model already performs mean pooling internally
        val output = Array(1) { FloatArray(384) }

        // The quantized model only has 2 inputs (input_ids + attention_mask);
        // token_type_ids was optimized away during TFLite conversion.
        val inputs = arrayOf(arrayOf(inputIds), arrayOf(attentionMask))
        interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to output))

        l2Normalize(output[0])
    }

    fun close() = interpreter.close()
}
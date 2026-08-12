package com.jyoti.learneaseai.data.local.embed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BertTokenizer @Inject constructor(
    @ApplicationContext private val  context: Context,
) {
    private val vocabFileName: String = "vocab.txt"
    private val vocab: Map<String, Int> = context.assets.open(vocabFileName)
        .bufferedReader().readLines()
        .mapIndexed { index, token -> token to index }
        .toMap()

    suspend fun tokenize(text: String, maxLength: Int = 256): Triple<IntArray, IntArray, IntArray>
    =withContext(Dispatchers.IO){

        val tokens = mutableListOf("[CLS]")
        text.lowercase().split(Regex("\\s+")).forEach { word ->
            tokens.addAll(wordPieceTokenize(word))
        }
        tokens.add("[SEP]")

        val ids = tokens.map { vocab[it] ?: vocab["[UNK]"]!! }.take(maxLength).toMutableList()
        while (ids.size < maxLength) ids.add(0)  // pad

        val inputIds = ids.toIntArray()
        val attentionMask = IntArray(maxLength) { if (it < tokens.size) 1 else 0 }
        val tokenTypeIds = IntArray(maxLength) { 0 }  // single sentence, all zeros

        Triple(inputIds, attentionMask, tokenTypeIds)
    }

    private fun wordPieceTokenize(word: String): List<String> {
        if (vocab.containsKey(word)) return listOf(word)
        val pieces = mutableListOf<String>()
        var remaining = word
        while (remaining.isNotEmpty()) {
            var end = remaining.length
            var found = false
            while (end > 0) {
                val sub = (if (pieces.isNotEmpty()) "##" else "") + remaining.substring(0, end)
                if (vocab.containsKey(sub)) {
                    pieces.add(sub)
                    remaining = remaining.substring(end)
                    found = true
                    break
                }
                end--
            }
            if (!found) return listOf("[UNK]")
        }
        return pieces
    }
}
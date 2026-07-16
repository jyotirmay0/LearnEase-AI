package com.jyoti.learneaseai.ui.upload

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jyoti.learneaseai.BuildConfig
import com.jyoti.learneaseai.data.local.AppDatabase
import com.jyoti.learneaseai.data.local.EmbeddingEntity
import com.jyoti.learneaseai.data.remote.NetworkModule
import com.jyoti.learneaseai.data.repository.DocumentRepositoryImpl
import com.jyoti.learneaseai.domain.Chunker
import com.jyoti.learneaseai.pdf.PdfExatractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UploadVM(application: Application) : AndroidViewModel(application) {
    private val _selectedPdfUri = MutableStateFlow<Uri?>(null)
    val selectedPdfUri = _selectedPdfUri.asStateFlow()
    private val _pdfText = MutableStateFlow<String>("")
    val pdfText = _pdfText.asStateFlow()
    private val _chunks = MutableStateFlow<List<String>>(emptyList())
    val chunks = _chunks.asStateFlow()

    private val _embedding = MutableStateFlow<List<FloatArray>>(emptyList())
    val embedding = _embedding.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    private val _documentName = MutableStateFlow("")
    val documentName = _documentName.asStateFlow()

    val api = NetworkModule.api
    val repo: DocumentRepositoryImpl = DocumentRepositoryImpl()
    private val db = AppDatabase.getInstance(application)
    private val embeddingDao = db.embeddingDao()

    //select the pdf pick
    fun onPdfSelected(uri: Uri) {
        _selectedPdfUri.value = uri
        _documentName.value = uri.lastPathSegment ?: "document.pdf"
    }

    //extract the pdf -> text
    fun pdfExtract(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val extractor = PdfExatractor(context = getApplication())
            val text = extractor.extractText(uri)
            _pdfText.value = text
            // Auto-chunk after extraction
            _chunks.value = Chunker.chunk(text)
            Log.d("Chunks", "Created ${_chunks.value.size} chunks")
        }
    }

    fun chunking(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chunks.value = Chunker.chunk(text)
            Log.d("Chunks", chunks.value.toString())
        }
    }

    fun embedding() {
        viewModelScope.launch {
            try {
                // Wait until chunks are available
                _chunks.first { it.isNotEmpty() }.let { chunks ->
                    Log.d("embed", "embedding viewmodel called")
                    _embedding.value = repo.embedChunksThrottled(
                        chunks, api, apiKey = BuildConfig.GEMINI_API_KEY
                    )
                }

                // Save to Room database
                saveEmbeddingsToDb()
            } catch (e: Exception) {
                Log.e("embed", "Embedding failed: ${e.message}", e)
            }
        }
    }

    private suspend fun saveEmbeddingsToDb() {
        val currentChunks = _chunks.value
        val currentEmbeddings = _embedding.value
        val docName = _documentName.value

        if (currentChunks.size != currentEmbeddings.size) {
            Log.e("embed", "Chunks and embeddings size mismatch!")
            return
        }

        val entities = currentChunks.mapIndexed { index, chunkText ->
            EmbeddingEntity(
                documentName = docName,
                chunkIndex = index,
                chunkText = chunkText,
                embedding = currentEmbeddings[index]
            )
        }

        embeddingDao.deleteByDocument(docName)
        embeddingDao.insertAll(entities)
        _isSaved.value = true
        Log.d("embed", "Saved ${entities.size} embeddings to database for: $docName")
    }
}

package com.jyoti.learneaseai.ui.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyoti.learneaseai.data.repository.ChatRepository
import com.jyoti.learneaseai.data.repository.DocumentRepositoryImpl
import com.jyoti.learneaseai.domain.Chunker
import com.jyoti.learneaseai.domain.LocalLlmEngine
import com.jyoti.learneaseai.domain.models.Document
import com.jyoti.learneaseai.pdf.PdfExatractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.emptyList



@HiltViewModel
class UploadVM@Inject constructor(private val docRepo: DocumentRepositoryImpl,
                                  private val localLlm: LocalLlmEngine,
                                  @ApplicationContext private val context: Context,
                                  private val chatRepository :ChatRepository) : ViewModel () {
    private val _selectedPdfUri = MutableStateFlow<Uri?>(null)
    val selectedPdfUri = _selectedPdfUri.asStateFlow()
    private val _pdfText = MutableStateFlow<String>("")
    private val _chunks = MutableStateFlow<List<String>>(emptyList())
    val chunks = _chunks.asStateFlow()

    private val _embedding = MutableStateFlow<List<FloatArray>>(emptyList())
    val embedding = _embedding.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    private val _documentName = MutableStateFlow("")
    val documentName = _documentName.asStateFlow()

    private val _listOfPdf=MutableStateFlow<List<Document>>(emptyList())
    val listOfPdf = _listOfPdf.asStateFlow()

    // ── Q&A state ─────────────────────────────────────────────────
    private val _answer = MutableStateFlow("")
    val answer = _answer.asStateFlow()

    private val _isAnswering = MutableStateFlow(false)
    val isAnswering = _isAnswering.asStateFlow()

    private val _answerError = MutableStateFlow<String?>(null)
    val answerError = _answerError.asStateFlow()




    init {
        // Start loading the local model in the background
        getAllPdf()
        viewModelScope.launch {


            withContext(Dispatchers.IO) {
                try {
                    localLlm.initialize()
                } catch (e: Exception) {
                    Log.e("UploadVM", "Failed to load local LLM: ${e.message}", e)
                }
            }

        }
    }

    //select the pdf pick
    fun onPdfSelected(uri: Uri,fileName: String) {
        _selectedPdfUri.value = uri
        _documentName.value = fileName
        viewModelScope.launch(Dispatchers.IO){
            docRepo.processDocument(documentName.value, selectedPdfUri.value!!)
        }
    }

    //extract the pdf -> text
//    fun pdfExtract(uri: Uri) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val extractor = PdfExatractor(context)
//            val text = extractor.extractText(uri)
//            _pdfText.value = text
//            // Auto-chunk after extraction
//            _chunks.value = Chunker.chunk(text)
//            Log.d("Chunks", "Created ${_chunks.value.size} chunks")
//        }
//    }


    fun embedding() {
        viewModelScope.launch {
            try {
                // Wait until chunks are available
                _chunks.first { it.isNotEmpty() }.let { chunks ->
                    Log.d("embed", "embedding viewmodel called")
                    _embedding.value = docRepo.getEmbedChunks(
                        chunks
                    )
                }

                // Save to Room database
               docRepo.saveEmbeddingsToDb(_chunks.value,_embedding.value, _documentName.value)
            } catch (e: Exception) {
                Log.e("embed", "Embedding failed: ${e.message}", e)
            }
        }
    }

    /**
     * Ask a question against the uploaded documents.
     * Runs the full RAG pipeline: embed query → similarity → prompt → local LLM.
     */
//    fun askQuestion(question: String) {
//        if (question.isBlank()) return
//        viewModelScope.launch {
//            _isAnswering.value = true
//            _answer.value = ""
//            _answerError.value = null
//            try {
//                _answer.value = chatRepository.answer(question)
//            } catch (e: Exception) {
//                Log.e("ask", "Answer failed: ${e.message}", e)
//                _answerError.value = e.message ?: "Something went wrong"
//            } finally {
//                _isAnswering.value = false
//            }
//        }
//    }


    override fun onCleared() {
        super.onCleared()
        localLlm.close()
    }

    fun getAllPdf(){
        viewModelScope.launch {
            docRepo.getAllDoc().collect { documents ->
                documents.forEach {
                    Log.d("Documents", it.name)
                }
                _listOfPdf.value=documents

        }



        }


    }
}

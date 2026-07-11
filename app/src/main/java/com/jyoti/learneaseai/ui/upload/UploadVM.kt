package com.jyoti.learneaseai.ui.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyoti.learneaseai.domain.Chunker
import com.jyoti.learneaseai.pdf.PdfExatractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class UploadVM: ViewModel(){
    private val _selectedPdfUri = MutableStateFlow<Uri?>(null)
    val selectedPdfUri = _selectedPdfUri.asStateFlow()
    private val _pdfText = MutableStateFlow<String>("")
    val pdfText = _pdfText.asStateFlow()
    private val _chunks = MutableStateFlow<List<String>>(emptyList())
    val chunks = _chunks.asStateFlow()


//select the pdf pick

    fun onPdfSelected(uri: Uri) {

        _selectedPdfUri.value = uri
    }


    //extract the pdf -> text
    fun pdfExtract(uri:Uri,context: Context)
    {

        viewModelScope.launch (Dispatchers.IO){
            val extractor= PdfExatractor(context = context)
            _pdfText.value = extractor.extractText(uri)
        }
    }
    fun chunking (text: String){
        viewModelScope.launch (Dispatchers.IO){
            _chunks.value=Chunker.chunk(text)
            Log.d("Chunks", chunks.value.toString())


        }
    }

}
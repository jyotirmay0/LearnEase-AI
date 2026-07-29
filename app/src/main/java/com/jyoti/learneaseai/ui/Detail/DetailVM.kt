package com.jyoti.learneaseai.ui.Detail

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jyoti.learneaseai.data.repository.DocumentRepositoryImpl
import com.jyoti.learneaseai.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log


@HiltViewModel
class DetailVM  @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val docRepo: DocumentRepositoryImpl
) : ViewModel() {


    private val args = savedStateHandle.toRoute<Routes.Detail>()
    val docId=args.docId

    private val _docName= MutableStateFlow("")
     val docName= _docName.asStateFlow()
    private val _chunks= MutableStateFlow<List<String>>(emptyList())
    val chunks=_chunks.asStateFlow()

    init {

        getDocumentName(docId)
        getChunks(docId)
    }
    fun getDocumentName(docID:String)
    {
        viewModelScope.launch {

            val document=docRepo.getDocumentbyId(docID)
            if(document==null)
            {
                Log.e("DocumentRepository", "Document with id $docID not found")
            }else
            {
                _docName.value=document.name
            }
        }

    }

    fun getChunks(docID: String)
    {
        viewModelScope.launch {
            val chunks=docRepo.getChunksbyDocumentId(docId)
            _chunks.value=chunks

        }
    }

}
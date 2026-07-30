package com.jyoti.learneaseai.ui.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jyoti.learneaseai.data.repository.ChatRepository
import com.jyoti.learneaseai.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ChatVM @Inject constructor(
    private val chatRepo: ChatRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val args = savedStateHandle.toRoute<Routes.Chat>()
    val docId=args.docId
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    fun startChat(question: String){
        viewModelScope.launch {
            _messages.update {
                it + ChatMessage(question, true)
            }
            _messages.update {
                it + ChatMessage("",false)

            }
            chatRepo.startChat(docId,question).collect { token ->

                _messages.update { current ->

                    val list = current.toMutableList()

                    val last = list.last()

                    list[list.lastIndex] =
                        last.copy(
                            text = last.text + token
                        )

                    list
                }


            }
        }

    }

    fun sendMessege(question: String){
        Log.d("vm", "sendMessege: {$question}")
        viewModelScope.launch {
            _messages.update {
                it + ChatMessage(question, true)
            }
            _messages.update {
                it + ChatMessage("",false)

            }
            chatRepo.sendMessege(docId, question).collect { token ->

                _messages.update { current ->

                    val list = current.toMutableList()

                    val last = list.last()

                    list[list.lastIndex] =
                        last.copy(
                            text = last.text + token
                        )

                    list
                }


            }
        }

    }

    override fun onCleared() {
        chatRepo.endSession(docId)
        super.onCleared()
    }
}
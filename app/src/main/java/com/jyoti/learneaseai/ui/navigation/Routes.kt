package com.jyoti.learneaseai.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Routes{

    @Serializable
    data object Upload: Routes
    @Serializable
    data class Detail(val docId: String): Routes
    @Serializable
    data class Chat(val docId: String): Routes

}
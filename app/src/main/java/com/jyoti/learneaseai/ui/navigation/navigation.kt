package com.jyoti.learneaseai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jyoti.learneaseai.ui.Detail.DetailScreen
import com.jyoti.learneaseai.ui.chat.ChatScreen
import com.jyoti.learneaseai.ui.upload.UploadScreen


@Composable
fun navGraph(navController: NavHostController= rememberNavController()){

    NavHost(navController = navController,
        startDestination = Routes.Upload) {

        composable<Routes.Upload> { backstackEntry ->
            UploadScreen(
                onDocumentClick = {docId ->
                    navController.navigate(Routes.Detail(docId))
                }
            )
        }

        composable<Routes.Detail> { backstackEntry ->
            val args = backstackEntry.toRoute<Routes.Detail>()
            DetailScreen(
                title = args.docId,
                onAskAiClick = {docId->

                    navController.navigate(Routes.Chat(docId))
                },
                onBackClick = {
                    navController.popBackStack()

                }


            )
        }

        composable<Routes.Chat> { backstackEntry ->
            val args = backstackEntry.toRoute<Routes.Detail>()
            ChatScreen (
                title = args.docId,
                onSendMessage = {docId->

                },
                onCloseClick = {
                    navController.popBackStack()

                }


            )
        }





    }

}
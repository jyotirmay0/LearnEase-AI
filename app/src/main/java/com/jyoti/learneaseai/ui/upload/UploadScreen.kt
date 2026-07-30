package com.jyoti.learneaseai.ui.upload

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.Layout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.TableInfo
import com.jyoti.learneaseai.pdf.getFileName
import kotlin.io.encoding.Base64


@Composable
fun UploadScreen(
    onDocumentClick: (String) -> Unit
) {

    val context = LocalContext.current
    val vm: UploadVM = hiltViewModel()

    val pdfUri by vm.selectedPdfUri.collectAsState()
    val embed by vm.embedding.collectAsState()
    val answer by vm.answer.collectAsState()
    val isAnswering by vm.isAnswering.collectAsState()
    val answerError by vm.answerError.collectAsState()
    val documents by vm.listOfPdf.collectAsState()
    val docName by vm.documentName.collectAsState()

    var questionText by remember { mutableStateOf("") }

    val result = remember { mutableStateOf<Uri?>(null) }
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { it: Uri ->
            val fileName = getFileName(context, it)
            result.value = it
            vm.onPdfSelected(it,fileName)

        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        val modifier = Modifier
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Upload Documents",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Import PDFs or add notes to build your knowledge base.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Upload buttons
            item {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val uploadState = null
                        Button(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.weight(1f),

                            ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload PDF")
                        }
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Notes")
                        }

                    }
                    pdfUri?.path.let { image ->
                        Text(text = "Document Path: " + image.toString() + docName)
                    }
                }


            }




            items(
                items = documents,
                key = { document -> document.id }
            ) { document ->

                DocumentCard(
                    id = document.id,
                    name = document.name,
                    onClick = {
                        onDocumentClick(document.id)
                    },
                    onMenuClick = {
                        // Show menu
                    }
                )
            }



        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FloatingActionButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
        }
    }
}



@Composable
fun EmbeddingsBox(
    embeddings: List<FloatArray>,
    modifier: Modifier = Modifier
) {

    val clipboard = LocalClipboardManager.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp),
        shape = RoundedCornerShape(16.dp)
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            IconButton(
                onClick = {

                    val text = buildString {

                        embeddings.forEachIndexed { index, vector ->

                            append("Chunk $index\n")
                            append(vector.joinToString(", "))
                            append("\n\n")
                        }
                    }

                    clipboard.setText(
                        AnnotatedString(text)
                    )
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy"
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = 48.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    embeddings.size
                ) { index ->

                    val vector = embeddings[index]

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Text(
                                text = "Chunk $index",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = vector.joinToString(
                                    separator = ", "
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun DocumentCard(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {

    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "PDF",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ID : $id",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onMenuClick
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More"
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview(){

    Column(modifier = Modifier.fillMaxSize()) {


    }

}
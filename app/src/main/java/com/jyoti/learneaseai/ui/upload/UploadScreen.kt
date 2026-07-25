package com.jyoti.learneaseai.ui.upload

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jyoti.learneaseai.pdf.getFileName

@Preview(showBackground = true)
@Composable
fun UploadScreen() {

    val context = LocalContext.current
    val vm: UploadVM = viewModel()

    val pdfUri by vm.selectedPdfUri.collectAsState()
    val pdfText by vm.pdfText.collectAsState()
    val embed by vm.embedding.collectAsState()
    val answer by vm.answer.collectAsState()
    val isAnswering by vm.isAnswering.collectAsState()
    val answerError by vm.answerError.collectAsState()

    var questionText by remember { mutableStateOf("") }

    val result = remember { mutableStateOf<Uri?>(null) }
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { it: Uri ->
            val fileName = getFileName(context, it)
            result.value = it
            vm.onPdfSelected(it)
            vm.pdfExtract(uri)
        }
    }


    Scaffold(

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
                        Text(text = "Document Path: "+image.toString())
                    }
                }


            }
            item {


                ExtractedTextBox(
                    text = pdfText,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item {

                Button(
                    onClick = {
                        vm.embedding()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Generate Embeddings")
                }
            }
            item {
                EmbeddingsBox(embed)
            }

            // ── Ask a Question ────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ask a Question",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type your question here...") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                    IconButton(
                        onClick = {
                            vm.askQuestion(questionText)
                        },
                        enabled = questionText.isNotBlank() && !isAnswering
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Ask"
                        )
                    }
                }
            }

            // Answer / Processing
            item {
                if (isAnswering) {
                    // Processing indicator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Processing your question...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (answerError != null) {
                    // Error display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Error: $answerError",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (answer.isNotEmpty()) {
                    // Answer display
                    AnswerBox(answer = answer)
                }
            }

}}}



@Composable
fun ExtractedTextBox(
    text: String,
    modifier: Modifier = Modifier
) {

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp)
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            IconButton(
                onClick = {
                    clipboardManager.setText(
                        AnnotatedString(text)
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 48.dp,
                        bottom = 16.dp
                    )
                    .verticalScroll(scrollState)
            ) {

                Text(
                    text = text.ifEmpty {
                        "No text extracted yet..."
                    },
                    style = MaterialTheme.typography.bodyMedium
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
fun AnswerBox(
    answer: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(answer))
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy answer"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 48.dp,
                        bottom = 16.dp
                    )
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Answer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
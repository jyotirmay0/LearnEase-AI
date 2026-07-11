package com.jyoti.learneaseai.pdf



    import android.content.Context
    import android.net.Uri
    import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
    import com.tom_roush.pdfbox.pdmodel.PDDocument
    import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfExatractor (
        private val context: Context
    ) {

        init {
            PDFBoxResourceLoader.init(context)
        }

        suspend fun extractText(uri: Uri): String {

            context.contentResolver
                .openInputStream(uri)
                ?.use { inputStream ->

                    PDDocument.load(inputStream).use { document ->

                        return PDFTextStripper()
                            .getText(document)
                    }
                }

            return ""
        }
    }

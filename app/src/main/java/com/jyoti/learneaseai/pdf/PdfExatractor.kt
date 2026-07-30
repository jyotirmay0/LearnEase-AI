package com.jyoti.learneaseai.pdf



    import android.content.Context
    import android.net.Uri
    import android.provider.OpenableColumns
    import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
    import com.tom_roush.pdfbox.pdmodel.PDDocument
    import com.tom_roush.pdfbox.text.PDFTextStripper
    import dagger.hilt.android.qualifiers.ApplicationContext
    import javax.inject.Inject

class PdfExatractor @Inject constructor(
        @ApplicationContext private val context: Context
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

        /**
         * Extracts text page-by-page, calling [onPage] for each page's text.
         * This avoids holding the entire document text in memory at once.
         */
        suspend fun extractTextPageByPage(uri: Uri, onPage: (pageText: String) -> Unit) {
            context.contentResolver
                .openInputStream(uri)
                ?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        val stripper = PDFTextStripper()
                        val totalPages = document.numberOfPages
                        for (page in 1..totalPages) {
                            stripper.startPage = page
                            stripper.endPage = page
                            val pageText = stripper.getText(document)
                            if (pageText.isNotBlank()) {
                                onPage(pageText)
                            }
                        }
                    }
                }
        }
    }
fun getFileName(context: Context, uri: Uri): String {
    var name = "Unknown.pdf"

    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1) {
                name = cursor.getString(index)
            }
        }
    }

    return name
}
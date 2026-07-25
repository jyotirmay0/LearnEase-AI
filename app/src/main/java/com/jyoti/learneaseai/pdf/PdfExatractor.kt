package com.jyoti.learneaseai.pdf



    import android.content.Context
    import android.net.Uri
    import android.provider.OpenableColumns
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
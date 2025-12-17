package com.phamnhantucode.aicareercoach.utils

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Utility to extract text from PDF files
 */
object PdfTextExtractor {

    private var isInitialized = false

    /**
     * Initializes the PDFBox library. Should be called once.
     */
    fun init(context: Context) {
        if (!isInitialized) {
            PDFBoxResourceLoader.init(context)
            isInitialized = true
        }
    }

    /**
     * Extracts text from a PDF Uri
     */
    suspend fun extractText(context: Context, pdfUri: Uri): String = withContext(Dispatchers.IO) {
        init(context)
        var pdfDocument: PDDocument? = null
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(pdfUri)
            
            if (inputStream == null) {
                throw Exception("Could not open PDF file")
            }

            pdfDocument = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            return@withContext stripper.getText(pdfDocument)
        } catch (e: Exception) {
            throw Exception("Failed to extract text from PDF: ${e.message}", e)
        } finally {
            try {
                pdfDocument?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}

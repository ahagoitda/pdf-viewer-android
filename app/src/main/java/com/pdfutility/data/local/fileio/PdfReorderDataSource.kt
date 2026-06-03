package com.pdfutility.data.local.fileio

import android.content.Context
import android.net.Uri
import com.pdfutility.domain.model.ConversionResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfReorderDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun reorderPages(
        sourceUri: Uri,
        newOrder: List<Int>,
        outputFileName: String,
    ): ConversionResult = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "pdf_output")
        outputDir.mkdirs()
        val safeFileName = FileNameSanitizer.sanitize(outputFileName, "reordered")
        val outputFile = File(outputDir, "$safeFileName.pdf")

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                PDDocument.load(input).use { sourceDoc ->
                    val totalPages = sourceDoc.numberOfPages
                    if (newOrder.isEmpty() || newOrder.any { it < 0 || it >= totalPages }) {
                        return@withContext ConversionResult.Error("유효하지 않은 페이지 순서입니다.")
                    }

                    PDDocument().use { outputDoc ->
                        for ((idx, pageIndex) in newOrder.withIndex()) {
                            ensureActive()
                            val page = sourceDoc.getPage(pageIndex)
                            outputDoc.importPage(page)
                        }

                        FileOutputStream(outputFile).use { fos ->
                            outputDoc.save(fos)
                        }
                    }

                    ConversionResult.Success(
                        outputPath = outputFile.absolutePath,
                        outputName = outputFileName,
                        pageCount = newOrder.size,
                        totalSize = outputFile.length(),
                    )
                }
            } ?: ConversionResult.Error("PDF 파일을 열 수 없습니다.")
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "페이지 재배열 중 오류가 발생했습니다.")
        }
    }
}
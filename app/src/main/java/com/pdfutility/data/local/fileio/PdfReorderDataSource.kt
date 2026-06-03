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
    private companion object {
        const val MAX_SOURCE_SIZE = 200L * 1024 * 1024
    }

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
            val fileSize = queryFileSize(sourceUri)
            if (fileSize > MAX_SOURCE_SIZE) {
                return@withContext ConversionResult.Error(
                    "파일이 너무 큽니다 (${formatSize(fileSize)}). 200MB 이하 파일만 처리할 수 있습니다."
                )
            }

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

    private fun queryFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.declaredLength
            } ?: 0L
        } catch (_: Exception) { 0L }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
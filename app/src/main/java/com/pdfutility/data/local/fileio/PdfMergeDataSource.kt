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
class PdfMergeDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        const val MAX_SOURCE_SIZE = 200L * 1024 * 1024
    }

    suspend fun mergePdfs(
        pdfUris: List<Uri>,
        outputFileName: String,
        onProgress: (Float) -> Unit,
    ): ConversionResult = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "pdf_output")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "$outputFileName.pdf")

        val outputDoc = PDDocument()
        try {
            for ((index, uri) in pdfUris.withIndex()) {
                ensureActive()

                val fileSize = queryFileSize(uri)
                if (fileSize > MAX_SOURCE_SIZE) {
                    return@withContext ConversionResult.Error(
                        "파일이 너무 큽니다 (${formatSize(fileSize)}). 200MB 이하 파일만 병합할 수 있습니다."
                    )
                }

                context.contentResolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input).use { sourceDoc ->
                        for (page in sourceDoc.pages) {
                            outputDoc.importPage(page)
                        }
                    }
                } ?: return@withContext ConversionResult.Error("PDF 파일을 열 수 없습니다.")

                onProgress((index + 1).toFloat() / pdfUris.size)
            }

            FileOutputStream(outputFile).use { fos ->
                outputDoc.save(fos)
            }

            ConversionResult.Success(
                outputPath = outputFile.absolutePath,
                outputName = outputFileName,
                pageCount = outputDoc.numberOfPages,
                totalSize = outputFile.length(),
            )
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "PDF 병합 중 오류가 발생했습니다.")
        } finally {
            runCatching { outputDoc.close() }
        }
    }

    private fun queryFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.declaredLength
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

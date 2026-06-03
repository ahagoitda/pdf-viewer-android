package com.pdfutility.data.local.fileio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
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
class PdfSplitDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun splitPdf(
        sourceUri: Uri,
        pageIndices: List<Int>,
        outputFileName: String,
    ): ConversionResult = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "pdf_output")
        outputDir.mkdirs()
        val safeFileName = FileNameSanitizer.sanitize(outputFileName, "split")
        val outputFile = File(outputDir, "$safeFileName.pdf")

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                PDDocument.load(input).use { sourceDoc ->
                    val totalSourcePages = sourceDoc.numberOfPages
                    val validIndices = pageIndices.filter { it in 0 until totalSourcePages }
                    if (validIndices.isEmpty()) {
                        return@withContext ConversionResult.Error("선택된 페이지가 없습니다.")
                    }

                    PDDocument().use { outputDoc ->
                        for (pageIndex in validIndices) {
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
                        pageCount = validIndices.size,
                        totalSize = outputFile.length(),
                    )
                }
            } ?: ConversionResult.Error("PDF 파일을 열 수 없습니다.")
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "PDF 분할 중 오류가 발생했습니다.")
        }
    }

    suspend fun getPageCount(sourceUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            } ?: 0
        } catch (_: Exception) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    PDDocument.load(input).use { it.numberOfPages }
                } ?: 0
            } catch (_: Exception) {
                0
            }
        }
    }

    suspend fun renderPageThumbnail(
        sourceUri: Uri,
        pageIndex: Int,
        width: Int,
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    val page = renderer.openPage(pageIndex)
                    val scale = width.toFloat() / page.width
                    val targetHeight = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(width, targetHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
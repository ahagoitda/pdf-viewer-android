package com.pdfutility.data.local.fileio

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import com.pdfutility.domain.model.ConversionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class ConversionFileDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        const val A4_WIDTH_PT = 595
        const val A4_HEIGHT_PT = 842
        const val MAX_IMAGE_DIMENSION = 2000
    }

    suspend fun convertImagesToPdf(
        images: List<Uri>,
        outputFileName: String,
    ): ConversionResult = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "pdf_output")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val safeFileName = FileNameSanitizer.sanitize(outputFileName, "pdf")
        val outputFile = File(outputDir, "$safeFileName.pdf")

        val pdfDocument = PdfDocument()
        val contentResolver = context.contentResolver
        var successCount = 0

        try {
            for ((index, imageUri) in images.withIndex()) {
                ensureActive()

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val originalWidth = options.outWidth
                val originalHeight = options.outHeight

                if (originalWidth <= 0 || originalHeight <= 0) continue

                val bitmap = decodeBitmapEfficiently(contentResolver, imageUri, originalWidth, originalHeight)

                bitmap?.let {
                    val scale = min(
                        A4_WIDTH_PT.toFloat() / it.width,
                        A4_HEIGHT_PT.toFloat() / it.height
                    )
                    val scaledWidth = (it.width * scale).toInt()
                    val scaledHeight = (it.height * scale).toInt()

                    val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, successCount + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    val left = (A4_WIDTH_PT - scaledWidth) / 2f
                    val top = (A4_HEIGHT_PT - scaledHeight) / 2f

                    canvas.drawBitmap(it, left, top, null)
                    pdfDocument.finishPage(page)

                    successCount++
                    it.recycle()
                }
            }

            if (successCount == 0) {
                return@withContext ConversionResult.Error("변환할 수 있는 이미지가 없습니다. 지원하지 않는 형식이거나 손상된 파일일 수 있습니다.")
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }

            ConversionResult.Success(
                outputPath = outputFile.absolutePath,
                outputName = outputFileName,
                pageCount = successCount,
                totalSize = outputFile.length()
            )
        } catch (e: Exception) {
            ConversionResult.Error(e.message ?: "변환 중 오류가 발생했습니다.")
        } finally {
            pdfDocument.close()
        }
    }

    private fun decodeBitmapEfficiently(
        contentResolver: ContentResolver,
        uri: Uri,
        width: Int,
        height: Int
    ): Bitmap? {
        val sampleSize = if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
            max(width / MAX_IMAGE_DIMENSION, height / MAX_IMAGE_DIMENSION)
        } else {
            1
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(contentResolver, uri)
                ) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    if (sampleSize > 1) {
                        decoder.setTargetSampleSize(sampleSize)
                    }
                }
            } catch (e: Exception) {
                decodeWithBitmapFactory(contentResolver, uri, sampleSize)
            }
        } else {
            decodeWithBitmapFactory(contentResolver, uri, sampleSize)
        }
    }

    private fun decodeWithBitmapFactory(
        contentResolver: ContentResolver,
        uri: Uri,
        sampleSize: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }
}
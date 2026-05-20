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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class ConversionFileDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun convertImagesToPdf(
        images: List<Uri>,
        outputFileName: String,
    ): ConversionResult = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "pdf_output")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, "$outputFileName.pdf")

        val pdfDocument = PdfDocument()
        val contentResolver = context.contentResolver

        try {
            for ((index, imageUri) in images.withIndex()) {
                // 1. Get image bounds first
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val originalWidth = options.outWidth
                val originalHeight = options.outHeight
                
                if (originalWidth <= 0 || originalHeight <= 0) continue

                // 2. Decode bitmap with downsampling if needed
                val bitmap = decodeBitmapEfficiently(contentResolver, imageUri, originalWidth, originalHeight)

                bitmap?.let {
                    // 3. Create PDF page matching the actual bitmap size
                    val pageInfo = PdfDocument.PageInfo.Builder(it.width, it.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    canvas.drawBitmap(it, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    
                    it.recycle() // Immediate recycle
                }
            }

            // 4. Write to file
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }

            ConversionResult.Success(
                outputPath = outputFile.absolutePath,
                outputName = outputFileName,
                pageCount = images.size,
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
        val maxDimension = 2000 // Max dimension to prevent OOM
        val sampleSize = if (width > maxDimension || height > maxDimension) {
            max(width / maxDimension, height / maxDimension)
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

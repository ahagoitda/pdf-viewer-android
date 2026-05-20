package com.pdfutility.data.local.repository

import android.net.Uri
import com.pdfutility.data.local.fileio.ConversionFileDataSource
import com.pdfutility.domain.model.ConversionResult
import com.pdfutility.domain.repository.ConversionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversionRepositoryImpl @Inject constructor(
    private val conversionFileDataSource: ConversionFileDataSource,
) : ConversionRepository {

    override suspend fun convertImagesToPdf(
        images: List<Uri>,
        outputFileName: String,
    ): ConversionResult {
        return conversionFileDataSource.convertImagesToPdf(images, outputFileName)
    }
}

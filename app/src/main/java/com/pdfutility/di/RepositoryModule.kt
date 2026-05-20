package com.pdfutility.di

import com.pdfutility.data.local.repository.ConversionRepositoryImpl
import com.pdfutility.data.local.repository.DocumentRepositoryImpl
import com.pdfutility.domain.repository.ConversionRepository
import com.pdfutility.domain.repository.DocumentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryImpl,
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindConversionRepository(
        impl: ConversionRepositoryImpl,
    ): ConversionRepository
}

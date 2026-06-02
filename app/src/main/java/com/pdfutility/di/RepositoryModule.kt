package com.pdfutility.di

import com.pdfutility.data.local.repository.ConversionRepositoryImpl
import com.pdfutility.data.local.repository.DocumentRepositoryImpl
import com.pdfutility.data.local.repository.MergeRepositoryImpl
import com.pdfutility.data.local.repository.SplitRepositoryImpl
import com.pdfutility.domain.repository.ConversionRepository
import com.pdfutility.domain.repository.DocumentRepository
import com.pdfutility.domain.repository.MergeRepository
import com.pdfutility.domain.repository.SplitRepository
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

    @Binds
    @Singleton
    abstract fun bindMergeRepository(
        impl: MergeRepositoryImpl,
    ): MergeRepository

    @Binds
    @Singleton
    abstract fun bindSplitRepository(
        impl: SplitRepositoryImpl,
    ): SplitRepository
}

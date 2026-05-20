package com.pdfutility.di

import android.content.Context
import androidx.room.Room
import com.pdfutility.data.local.db.PdfUtilityDatabase
import com.pdfutility.data.local.db.dao.RecentDocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): PdfUtilityDatabase {
        return Room.databaseBuilder(
            context,
            PdfUtilityDatabase::class.java,
            "pdf_utility.db",
        ).build()
    }

    @Provides
    fun provideRecentDocumentDao(
        database: PdfUtilityDatabase,
    ): RecentDocumentDao {
        return database.recentDocumentDao()
    }
}

package com.pdfutility.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pdfutility.data.local.db.PdfUtilityDatabase
import com.pdfutility.data.local.db.dao.BookmarkDao
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

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `bookmarks` (`uri` TEXT NOT NULL, `name` TEXT NOT NULL, `size` INTEGER NOT NULL, `bookmarked_at` INTEGER NOT NULL, PRIMARY KEY(`uri`))"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): PdfUtilityDatabase {
        return Room.databaseBuilder(
            context,
            PdfUtilityDatabase::class.java,
            "pdf_utility.db",
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRecentDocumentDao(
        database: PdfUtilityDatabase,
    ): RecentDocumentDao {
        return database.recentDocumentDao()
    }

    @Provides
    fun provideBookmarkDao(
        database: PdfUtilityDatabase,
    ): BookmarkDao {
        return database.bookmarkDao()
    }
}

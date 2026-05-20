package com.pdfutility.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pdfutility.data.local.db.dao.RecentDocumentDao
import com.pdfutility.data.local.db.entity.RecentDocumentEntity

@Database(
    entities = [RecentDocumentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PdfUtilityDatabase : RoomDatabase() {
    abstract fun recentDocumentDao(): RecentDocumentDao
}

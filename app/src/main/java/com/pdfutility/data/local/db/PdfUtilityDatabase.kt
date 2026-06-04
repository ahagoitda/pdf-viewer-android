package com.pdfutility.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pdfutility.data.local.db.dao.BookmarkDao
import com.pdfutility.data.local.db.dao.PageBookmarkDao
import com.pdfutility.data.local.db.dao.RecentDocumentDao
import com.pdfutility.data.local.db.entity.BookmarkEntity
import com.pdfutility.data.local.db.entity.PageBookmarkEntity
import com.pdfutility.data.local.db.entity.RecentDocumentEntity

@Database(
    entities = [RecentDocumentEntity::class, BookmarkEntity::class, PageBookmarkEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class PdfUtilityDatabase : RoomDatabase() {
    abstract fun recentDocumentDao(): RecentDocumentDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun pageBookmarkDao(): PageBookmarkDao
}

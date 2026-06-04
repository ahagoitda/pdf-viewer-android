package com.pdfutility.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "page_bookmarks", primaryKeys = ["uri", "page_index"])
data class PageBookmarkEntity(
    val uri: String,
    @ColumnInfo(name = "page_index")
    val pageIndex: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

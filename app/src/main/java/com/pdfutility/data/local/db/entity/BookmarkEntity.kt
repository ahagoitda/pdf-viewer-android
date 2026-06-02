package com.pdfutility.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "size")
    val size: Long,
    @ColumnInfo(name = "bookmarked_at")
    val bookmarkedAt: Long,
)
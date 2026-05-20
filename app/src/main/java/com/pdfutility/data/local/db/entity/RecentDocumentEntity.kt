package com.pdfutility.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_documents")
data class RecentDocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "size")
    val size: Long,
    @ColumnInfo(name = "last_opened")
    val lastOpened: Long,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long,
)

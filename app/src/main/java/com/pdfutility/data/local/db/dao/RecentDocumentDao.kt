package com.pdfutility.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdfutility.data.local.db.entity.RecentDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDocumentDao {
    @Query("SELECT * FROM recent_documents ORDER BY last_opened DESC LIMIT 20")
    fun getRecentDocuments(): Flow<List<RecentDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: RecentDocumentEntity)

    @Query("DELETE FROM recent_documents WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}

package com.pdfutility.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdfutility.data.local.db.entity.PageBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageBookmarkDao {
    @Query("SELECT * FROM page_bookmarks WHERE uri = :uri ORDER BY page_index ASC")
    fun getBookmarks(uri: String): Flow<List<PageBookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM page_bookmarks WHERE uri = :uri AND page_index = :pageIndex)")
    suspend fun isBookmarked(uri: String, pageIndex: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: PageBookmarkEntity)

    @Query("DELETE FROM page_bookmarks WHERE uri = :uri AND page_index = :pageIndex")
    suspend fun removeBookmark(uri: String, pageIndex: Int)
}

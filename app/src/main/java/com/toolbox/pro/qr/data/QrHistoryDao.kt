package com.toolbox.pro.qr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QrHistoryDao {
    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<QrHistoryEntity>>

    @Insert
    suspend fun insert(item: QrHistoryEntity): Long

    @Update
    suspend fun update(item: QrHistoryEntity)

    @Delete
    suspend fun delete(item: QrHistoryEntity)

    @Query("DELETE FROM qr_history")
    suspend fun deleteAll()
}

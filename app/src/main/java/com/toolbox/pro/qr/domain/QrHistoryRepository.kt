package com.toolbox.pro.qr.domain

import com.toolbox.pro.qr.data.QrHistoryDao
import com.toolbox.pro.qr.data.QrHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrHistoryRepository @Inject constructor(
    private val qrHistoryDao: QrHistoryDao
) {
    fun getAllHistory(): Flow<List<QrHistoryEntity>> {
        return qrHistoryDao.getAllHistory()
    }

    suspend fun addToHistory(
        content: String,
        contentType: String
    ): Long {
        return qrHistoryDao.insert(
            QrHistoryEntity(
                content = content,
                contentType = contentType
            )
        )
    }

    suspend fun toggleFavorite(item: QrHistoryEntity) {
        qrHistoryDao.update(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun delete(item: QrHistoryEntity) {
        qrHistoryDao.delete(item)
    }

    suspend fun deleteAll() {
        qrHistoryDao.deleteAll()
    }
}

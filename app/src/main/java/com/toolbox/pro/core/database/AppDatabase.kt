package com.toolbox.pro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.toolbox.pro.qr.data.QrHistoryDao
import com.toolbox.pro.qr.data.QrHistoryEntity

@Database(
    entities = [QrHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qrHistoryDao(): QrHistoryDao
}

package com.toolbox.pro.qr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_history")
data class QrHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val contentType: String, // URL, TEXT, WIFI, CONTACT
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

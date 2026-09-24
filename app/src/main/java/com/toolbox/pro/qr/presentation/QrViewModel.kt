package com.toolbox.pro.qr.presentation

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.pro.qr.data.QrHistoryEntity
import com.toolbox.pro.qr.domain.QrGenerator
import com.toolbox.pro.qr.domain.QrHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class QrUiState(
    val content: String = "",
    val contentType: String = "URL",
    val qrBitmap: Bitmap? = null,
    val logoBitmap: Bitmap? = null,
    val primaryColor: Long = 0xFF000000,
    val secondaryColor: Long = 0xFF6C63FF,
    val useGradient: Boolean = false,
    val history: List<QrHistoryEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val saveMessage: String? = null
)

@HiltViewModel
class QrViewModel @Inject constructor(
    private val qrGenerator: QrGenerator,
    private val historyRepository: QrHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.getAllHistory().collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
    }

    fun onContentChange(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
        generateQr()
    }

    fun onContentTypeChange(type: String) {
        _uiState.value = _uiState.value.copy(contentType = type)
    }

    fun onLogoSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            _uiState.value = _uiState.value.copy(logoBitmap = bitmap)
            generateQr()
        }
    }

    fun onLogoRemoved() {
        _uiState.value = _uiState.value.copy(logoBitmap = null)
        generateQr()
    }

    fun onPrimaryColorChange(color: Long) {
        _uiState.value = _uiState.value.copy(primaryColor = color)
        generateQr()
    }

    fun onSecondaryColorChange(color: Long) {
        _uiState.value = _uiState.value.copy(secondaryColor = color)
        generateQr()
    }

    fun onUseGradientChange(useGradient: Boolean) {
        _uiState.value = _uiState.value.copy(useGradient = useGradient)
        generateQr()
    }

    private fun generateQr() {
        val content = _uiState.value.content
        if (content.isBlank()) {
            _uiState.value = _uiState.value.copy(qrBitmap = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            try {
                val bitmap = qrGenerator.generateQrBitmap(
                    content = content,
                    size = 1024,
                    logoBitmap = _uiState.value.logoBitmap,
                    primaryColor = _uiState.value.primaryColor,
                    secondaryColor = _uiState.value.secondaryColor,
                    useGradient = _uiState.value.useGradient
                )
                _uiState.value = _uiState.value.copy(qrBitmap = bitmap, isGenerating = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            }
        }
    }

    fun saveQrToGallery(context: Context) {
        val bitmap = _uiState.value.qrBitmap ?: return

        viewModelScope.launch {
            try {
                val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "QR_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ToolBoxPro")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        true
                    } else false
                } else {
                    @Suppress("DEPRECATION")
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "QR_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
                        true
                    } else false
                }
                _uiState.value = _uiState.value.copy(saveMessage = if (saved) "QR saved to gallery!" else "Failed to save")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "Error: ${e.message}")
            }
        }
    }

    fun shareQrImage(context: Context) {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch {
            try {
                val file = File(context.cacheDir, "qr_share_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { os -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os) }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share QR"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "Error: ${e.message}")
            }
        }
    }

    fun clearSaveMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    fun saveToHistory() {
        val state = _uiState.value
        if (state.content.isNotBlank()) {
            viewModelScope.launch {
                historyRepository.addToHistory(content = state.content, contentType = state.contentType)
            }
        }
    }
}

package com.toolbox.pro.fileshare.presentation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.pro.fileshare.network.WifiUtils
import com.toolbox.pro.fileshare.server.SharedFile
import com.toolbox.pro.fileshare.service.FileServerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

data class FileShareUiState(
    val isServerRunning: Boolean = false,
    val serverUrl: String? = null,
    val deviceIp: String? = null,
    val wifiSsid: String? = null,
    val networkSpeed: String = "Unknown",
    val isWifiConnected: Boolean = false,
    val selectedFiles: List<SelectedFile> = emptyList(),
    val showQrDialog: Boolean = false
)

@HiltViewModel
class FileShareViewModel @Inject constructor(
    app: Application
) : AndroidViewModel(app) {

    private val appContext: Application
        get() = getApplication()

    private val _uiState = MutableStateFlow(FileShareUiState())
    val uiState: StateFlow<FileShareUiState> = _uiState.asStateFlow()

    init {
        loadNetworkInfo()
        observeServerState()
    }

    private fun loadNetworkInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deviceIp = WifiUtils.getDeviceIpAddress(appContext),
                wifiSsid = WifiUtils.getWifiSsid(appContext),
                networkSpeed = WifiUtils.getNetworkSpeed(appContext),
                isWifiConnected = WifiUtils.isWifiConnected(appContext)
            )
        }
    }

    private fun observeServerState() {
        viewModelScope.launch {
            FileServerService.isRunning.collect { running ->
                _uiState.value = _uiState.value.copy(isServerRunning = running)
            }
        }
    }

    fun onFileSelected(uris: List<Uri>) {
        val contentResolver = appContext.contentResolver
        val files = uris.mapNotNull { uri ->
            try {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                        val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                        SelectedFile(uri = uri, name = name, size = size, mimeType = mimeType)
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedFiles = _uiState.value.selectedFiles + files
        )

        if (_uiState.value.isServerRunning) {
            syncFilesToServer()
        }
    }

    fun removeFile(index: Int) {
        val currentFiles = _uiState.value.selectedFiles.toMutableList()
        if (index in currentFiles.indices) {
            currentFiles.removeAt(index)
            _uiState.value = _uiState.value.copy(selectedFiles = currentFiles)
            if (_uiState.value.isServerRunning) {
                syncFilesToServer()
            }
        }
    }

    fun clearFiles() {
        _uiState.value = _uiState.value.copy(selectedFiles = emptyList())
        if (_uiState.value.isServerRunning) {
            syncFilesToServer()
        }
    }

    fun toggleQrDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showQrDialog = show)
    }

    private fun buildSharedFiles(): List<SharedFile> {
        return _uiState.value.selectedFiles.map { f ->
            SharedFile(
                id = UUID.randomUUID().toString(),
                name = f.name,
                size = f.size,
                mimeType = f.mimeType,
                uri = f.uri.toString()
            )
        }
    }

    private fun syncFilesToServer() {
        val files = buildSharedFiles()
        val intent = Intent(appContext, FileServerService::class.java).apply {
            action = FileServerService.ACTION_UPDATE_FILES
            putExtra(FileServerService.EXTRA_FILES_JSON, ArrayList(files))
        }
        appContext.startService(intent)
    }

    fun startServer() {
        val ip = _uiState.value.deviceIp ?: return
        val files = buildSharedFiles()

        val intent = Intent(appContext, FileServerService::class.java).apply {
            action = FileServerService.ACTION_START
            putExtra(FileServerService.EXTRA_PORT, 8080)
            putExtra(FileServerService.EXTRA_FILES_JSON, ArrayList(files))
        }
        appContext.startForegroundService(intent)
        _uiState.value = _uiState.value.copy(
            serverUrl = "http://$ip:8080"
        )
    }

    fun stopServer() {
        val intent = Intent(appContext, FileServerService::class.java).apply {
            action = FileServerService.ACTION_STOP
        }
        appContext.startService(intent)
        _uiState.value = _uiState.value.copy(serverUrl = null, showQrDialog = false)
    }

    fun refreshNetworkInfo() {
        loadNetworkInfo()
    }
}

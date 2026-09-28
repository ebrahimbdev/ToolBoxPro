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
import com.toolbox.pro.qr.data.ContactsRepository
import com.toolbox.pro.qr.data.ContactItem
import com.toolbox.pro.qr.data.QrHistoryEntity
import com.toolbox.pro.qr.data.WifiNetwork
import com.toolbox.pro.qr.data.WifiNetworkRepository
import com.toolbox.pro.qr.domain.QrGenerator
import com.toolbox.pro.qr.domain.QrHistoryRepository
import com.toolbox.pro.qr.domain.QrStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val generatedForContent: String? = null,
    val logoBitmap: Bitmap? = null,
    val style: QrStyle = QrStyle(),
    val history: List<QrHistoryEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val saveMessage: String? = null,
    val selectedTab: Int = 0,
    val wifiNetworks: List<WifiNetwork> = emptyList(),
    val wifiLoading: Boolean = false,
    val selectedWifi: WifiNetwork? = null,
    val manualSsid: String = "",
    val wifiSecurity: String = "WPA",
    val wifiPassword: String = "",
    val contactsPermissionGranted: Boolean = false,
    val contactsLoading: Boolean = false,
    val contacts: List<ContactItem> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val selectedContactsBytes: Int = 0,
    val contactsSearch: String = ""
) {
    val isStale: Boolean
        get() = qrBitmap != null && generatedForContent != null && content != generatedForContent

    val contactsLimitExceeded: Boolean
        get() = selectedContacts.size >= ContactsRepository.MAX_CONTACTS ||
            selectedContactsBytes > ContactsRepository.MAX_BYTES

    val filteredContacts: List<ContactItem>
        get() {
            val query = contactsSearch.trim()
            if (query.isEmpty()) return contacts
            return contacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                    contact.numbers.any { it.contains(query) }
            }
        }

    val selectedContacts: List<ContactItem>
        get() = contacts.filter { it.id in selectedContactIds }
}

@HiltViewModel
class QrViewModel @Inject constructor(
    private val qrGenerator: QrGenerator,
    private val historyRepository: QrHistoryRepository,
    private val wifiNetworkRepository: WifiNetworkRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    private var styleJob: Job? = null

    init {
        viewModelScope.launch {
            historyRepository.getAllHistory().collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
        _uiState.value = _uiState.value.copy(contactsPermissionGranted = contactsRepository.hasPermission())
    }

    fun onSelectedTabChange(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun onContentChange(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }

    fun onGenerateClick() {
        generateQr()
    }

    fun onContentTypeChange(type: String) {
        _uiState.value = _uiState.value.copy(contentType = type)
        when (type) {
            "WIFI" -> if (_uiState.value.wifiNetworks.isEmpty()) loadWifiNetworks()
            "CONTACT" -> if (_uiState.value.contacts.isEmpty()) loadContacts()
        }
    }

    // ---------------------------------------------------------------- wifi

    fun loadWifiNetworks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(wifiLoading = true)
            val networks = try {
                wifiNetworkRepository.listNetworks()
            } catch (_: Throwable) {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(wifiLoading = false, wifiNetworks = networks)
        }
    }

    fun onManualSsidChange(value: String) {
        _uiState.value = _uiState.value.copy(manualSsid = value)
    }

    fun onAddManualWifi() {
        val ssid = _uiState.value.manualSsid.trim()
        if (ssid.isEmpty()) return
        val network = WifiNetwork(ssid = ssid, security = "WPA", isCurrent = false, isSaved = false)
        _uiState.value = _uiState.value.copy(
            wifiNetworks = listOf(network) + _uiState.value.wifiNetworks.filterNot { it.ssid == ssid },
            selectedWifi = network,
            manualSsid = "",
            wifiSecurity = "WPA",
            wifiPassword = ""
        )
    }

    fun onWifiSelected(network: WifiNetwork) {
        val alreadySelected = _uiState.value.selectedWifi?.ssid == network.ssid
        _uiState.value = _uiState.value.copy(
            selectedWifi = if (alreadySelected) null else network,
            wifiSecurity = if (alreadySelected) _uiState.value.wifiSecurity else network.security,
            wifiPassword = ""
        )
    }

    fun onWifiSecurityChange(security: String) {
        _uiState.value = _uiState.value.copy(wifiSecurity = security)
    }

    fun onWifiPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(wifiPassword = password)
    }

    fun onGenerateWifiClick() {
        val network = _uiState.value.selectedWifi ?: return
        val security = _uiState.value.wifiSecurity
        val password = _uiState.value.wifiPassword
        if (security != "nopass" && password.isBlank()) return

        val payload = wifiNetworkRepository.buildPayload(network.ssid, security, password)
        _uiState.value = _uiState.value.copy(content = payload, contentType = "WIFI")
        generateQr()
    }

    // ------------------------------------------------------------ contacts

    fun onContactsPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(contactsPermissionGranted = granted)
        if (granted) loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(contactsLoading = true)
            val contacts = try {
                contactsRepository.loadContacts()
            } catch (_: Throwable) {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(contactsLoading = false, contacts = contacts)
        }
    }

    fun onContactsSearchChange(value: String) {
        _uiState.value = _uiState.value.copy(contactsSearch = value)
    }

    fun onContactToggle(id: String) {
        val selected = _uiState.value.selectedContactIds
        val next = if (selected.contains(id)) selected - id else selected + id
        val picked = _uiState.value.contacts.filter { it.id in next }
        val bytes = try {
            contactsRepository.payloadBytes(picked)
        } catch (_: Throwable) {
            0
        }
        _uiState.value = _uiState.value.copy(
            selectedContactIds = next,
            selectedContactsBytes = bytes
        )
    }

    fun onGenerateContactsClick() {
        val selected = _uiState.value.selectedContacts
        if (selected.isEmpty()) return
        if (contactsRepository.payloadBytes(selected) > ContactsRepository.MAX_BYTES) return

        val payload = contactsRepository.buildVCards(selected)
        _uiState.value = _uiState.value.copy(content = payload, contentType = "CONTACT")
        generateQr()
    }

    // ------------------------------------------------------------- design

    fun onStyleChange(transform: (QrStyle) -> QrStyle) {
        _uiState.value = _uiState.value.copy(style = transform(_uiState.value.style))
        if (_uiState.value.qrBitmap == null) return
        // Live preview: debounce so dragging sliders renders once per pause.
        styleJob?.cancel()
        styleJob = viewModelScope.launch {
            delay(250)
            generateQr()
        }
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
            _uiState.value = _uiState.value.copy(
                logoBitmap = bitmap,
                style = _uiState.value.style.copy(
                    logoPosition = com.toolbox.pro.qr.domain.LogoPosition.CENTER,
                    logoSizeFraction = 0.20f
                )
            )
            if (_uiState.value.qrBitmap != null) generateQr()
        }
    }

    fun onLogoRemoved() {
        _uiState.value = _uiState.value.copy(logoBitmap = null)
        if (_uiState.value.qrBitmap != null) generateQr()
    }

    private fun generateQr() {
        val content = _uiState.value.content
        if (content.isBlank()) {
            _uiState.value = _uiState.value.copy(qrBitmap = null, generatedForContent = null, isGenerating = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            try {
                val bitmap = qrGenerator.generateQrBitmap(
                    content = content,
                    size = 1024,
                    logoBitmap = _uiState.value.logoBitmap,
                    style = _uiState.value.style
                )
                _uiState.value = _uiState.value.copy(
                    qrBitmap = bitmap,
                    generatedForContent = _uiState.value.content,
                    isGenerating = false
                )
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

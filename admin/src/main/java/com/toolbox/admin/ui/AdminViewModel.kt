package com.toolbox.admin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.admin.data.AdminSession
import com.toolbox.admin.data.api.AdminApi
import com.toolbox.admin.data.api.AdminApiResult
import com.toolbox.admin.data.model.AdminConfig
import com.toolbox.admin.data.model.AdminPayment
import com.toolbox.admin.data.model.AdminStats
import com.toolbox.admin.data.model.AdminUser
import com.toolbox.admin.data.model.ConfigPatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    application: Application,
    private val session: AdminSession,
    private val api: AdminApi
) : AndroidViewModel(application) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _ui = MutableStateFlow(AdminUiState())
    val ui: StateFlow<AdminUiState> = _ui.asStateFlow()

    private val _stats = MutableStateFlow<AdminStats?>(null)
    val stats: StateFlow<AdminStats?> = _stats.asStateFlow()

    private val _users = MutableStateFlow<List<AdminUser>>(emptyList())
    val users: StateFlow<List<AdminUser>> = _users.asStateFlow()

    private val _payments = MutableStateFlow<List<AdminPayment>>(emptyList())
    val payments: StateFlow<List<AdminPayment>> = _payments.asStateFlow()

    private val _config = MutableStateFlow<AdminConfig?>(null)
    val config: StateFlow<AdminConfig?> = _config.asStateFlow()

    sealed class AuthState {
        data object Unknown : AuthState()
        data object LoggedOut : AuthState()
        data object LoggedIn : AuthState()
    }

    init {
        viewModelScope.launch {
            val ok = session.restore()
            _authState.value = if (ok) AuthState.LoggedIn else AuthState.LoggedOut
            if (ok) refreshAll()
        }
    }

    fun login(token: String) {
        viewModelScope.launch {
            _ui.value = AdminUiState(loading = true)
            when (val r = session.login(token)) {
                is AdminApiResult.Success -> {
                    _authState.value = AuthState.LoggedIn
                    _ui.value = AdminUiState(message = "Logged in")
                    refreshAll()
                }
                is AdminApiResult.Failure -> {
                    _ui.value = AdminUiState(error = r.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.logout()
            _authState.value = AuthState.LoggedOut
            _stats.value = null
            _users.value = emptyList()
            _payments.value = emptyList()
            _config.value = null
            _ui.value = AdminUiState()
        }
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null, error = null)
    }

    fun refreshAll() {
        viewModelScope.launch {
            _ui.value = AdminUiState(loading = true)
            var error: String? = null
            when (val r = api.stats()) {
                is AdminApiResult.Success -> _stats.value = r.data
                is AdminApiResult.Failure -> error = r.message
            }
            when (val r = api.users()) {
                is AdminApiResult.Success -> _users.value = r.data.users
                is AdminApiResult.Failure -> error = error ?: r.message
            }
            when (val r = api.payments()) {
                is AdminApiResult.Success -> _payments.value = r.data.payments
                is AdminApiResult.Failure -> error = error ?: r.message
            }
            when (val r = api.getConfig()) {
                is AdminApiResult.Success -> _config.value = r.data
                is AdminApiResult.Failure -> error = error ?: r.message
            }
            _ui.value = AdminUiState(error = error)
        }
    }

    fun searchUsers(q: String) {
        viewModelScope.launch {
            when (val r = api.users(q = q)) {
                is AdminApiResult.Success -> _users.value = r.data.users
                is AdminApiResult.Failure -> _ui.value = _ui.value.copy(error = r.message)
            }
        }
    }

    fun updateUsername(deviceId: String, username: String) {
        viewModelScope.launch {
            when (api.setUsername(deviceId, username)) {
                is AdminApiResult.Success -> {
                    _ui.value = _ui.value.copy(message = "Username updated")
                    searchUsers("")
                }
                is AdminApiResult.Failure -> _ui.value = _ui.value.copy(error = "Update failed")
            }
        }
    }

    fun toggleBlock(user: AdminUser) {
        viewModelScope.launch {
            val blocked = user.isBlocked != 1
            when (api.setBlocked(user.deviceId, blocked)) {
                is AdminApiResult.Success -> {
                    _ui.value = _ui.value.copy(message = if (blocked) "User blocked" else "User unblocked")
                    searchUsers("")
                }
                is AdminApiResult.Failure -> _ui.value = _ui.value.copy(error = "Block failed")
            }
        }
    }

    fun grant(user: AdminUser, days: Int) {
        viewModelScope.launch {
            when (api.grantSubscription(user.deviceId, days)) {
                is AdminApiResult.Success -> {
                    _ui.value = _ui.value.copy(message = "Subscription granted ($days days)")
                    refreshAll()
                }
                is AdminApiResult.Failure -> _ui.value = _ui.value.copy(error = "Grant failed")
            }
        }
    }

    fun confirmPayment(id: String) {
        viewModelScope.launch {
            when (api.confirmPayment(id)) {
                is AdminApiResult.Success -> {
                    _ui.value = _ui.value.copy(message = "Payment confirmed")
                    refreshAll()
                }
                is AdminApiResult.Failure -> _ui.value = _ui.value.copy(error = "Confirm failed")
            }
        }
    }

    fun rejectPayment(id: String) {
        viewModelScope.launch {
            when (api.rejectPayment(id)) {
                is AdminApiResult.Success -> {
                    _ui.value = _ui.value.copy(message = "Payment rejected")
                    refreshAll()
                }
                is AdminApiResult.Failure -> _ui.value = _ui.value.copy(error = "Reject failed")
            }
        }
    }

    fun saveConfig(patch: ConfigPatch) {
        viewModelScope.launch {
            _ui.value = AdminUiState(loading = true)
            when (val r = api.patchConfig(patch)) {
                is AdminApiResult.Success -> {
                    _config.value = r.data
                    _ui.value = AdminUiState(message = "Config saved")
                }
                is AdminApiResult.Failure -> _ui.value = AdminUiState(error = r.message)
            }
        }
    }
}

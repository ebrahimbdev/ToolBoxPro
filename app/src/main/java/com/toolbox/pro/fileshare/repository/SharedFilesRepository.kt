package com.toolbox.pro.fileshare.repository

import com.toolbox.pro.fileshare.server.SharedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedFilesRepository @Inject constructor() {
    private val _files = MutableStateFlow<List<SharedFile>>(emptyList())
    val files: StateFlow<List<SharedFile>> = _files.asStateFlow()

    fun add(file: SharedFile) {
        _files.value = _files.value + file
    }

    fun addAll(files: List<SharedFile>) {
        _files.value = _files.value + files
    }

    fun remove(id: String) {
        _files.value = _files.value.filterNot { it.id == id }
    }

    fun replace(files: List<SharedFile>) {
        _files.value = files
    }

    fun clear() {
        _files.value = emptyList()
    }

    fun snapshot(): List<SharedFile> = _files.value
}

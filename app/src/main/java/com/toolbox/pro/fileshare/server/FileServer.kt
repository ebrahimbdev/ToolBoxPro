package com.toolbox.pro.fileshare.server

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.utils.io.core.copyTo
import io.ktor.utils.io.streams.asOutput
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class SharedFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val mimeType: String,
    val uri: String,
    val uploaded: Boolean = false,
    val uploadedAt: Long = 0L
) : java.io.Serializable

@Serializable
data class UploadResponse(
    val ok: Boolean,
    val error: String? = null,
    val files: List<SharedFile> = emptyList()
)

class FileServer(
    private val appContext: Context,
    private val port: Int = 8080,
    private val uploadsDir: File = File(appContext.filesDir, "shared_uploads"),
    private val onUploadsChanged: ((List<SharedFile>) -> Unit)? = null
) {
    private var server: ApplicationEngine? = null
    private val hostFiles = CopyOnWriteArrayList<SharedFile>()
    private val uploadedFiles = CopyOnWriteArrayList<SharedFile>()

    init {
        runCatching {
            uploadsDir.mkdirs()
            uploadsDir.listFiles { f -> f.isFile && !f.name.startsWith(".") }
                ?.sortedByDescending { it.lastModified() }
                ?.forEach { f -> uploadedFiles.add(toUploaded(f)) }
        }
    }

    fun addFile(name: String, size: Long, mimeType: String, uri: String): SharedFile {
        val file = SharedFile(name = name, size = size, mimeType = mimeType, uri = uri)
        hostFiles.add(file)
        return file
    }

    fun setFiles(files: List<SharedFile>) {
        hostFiles.clear()
        hostFiles.addAll(files)
    }

    fun removeFile(fileId: String) {
        hostFiles.removeAll { it.id == fileId }
    }

    fun clearFiles() {
        hostFiles.clear()
    }

    fun uploadedSnapshot(): List<SharedFile> = uploadedFiles.toList()

    fun removeUploaded(fileId: String): Boolean {
        val file = uploadedFiles.find { it.id == fileId } ?: return false
        uploadedFiles.remove(file)
        runCatching { File(file.uri).delete() }
        onUploadsChanged?.invoke(uploadedFiles.toList())
        return true
    }

    fun clearUploads() {
        uploadedFiles.forEach { f -> runCatching { File(f.uri).delete() } }
        uploadedFiles.clear()
        onUploadsChanged?.invoke(emptyList())
    }

    fun start() {
        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; prettyPrint = true })
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respondText("Error: ${cause.message}", status = HttpStatusCode.InternalServerError)
                }
            }
            routing {
                get("/") {
                    call.respondText(panelHtml, ContentType.Text.Html)
                }
                get("/api/files") {
                    call.respond(uploadedFiles.toList() + hostFiles.toList())
                }
                post("/upload") {
                    handleUpload(call)
                }
                get("/download/{fileId}") {
                    val fileId = call.parameters["fileId"]
                    if (fileId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing file ID")
                        return@get
                    }
                    val uploaded = uploadedFiles.find { it.id == fileId }
                    if (uploaded != null) {
                        val file = File(uploaded.uri)
                        if (!file.isFile) {
                            removeUploaded(uploaded.id)
                            call.respond(HttpStatusCode.NotFound, "File not found")
                            return@get
                        }
                        call.response.header(HttpHeaders.ContentDisposition, attachmentDisposition(uploaded.name))
                        call.respondFile(file)
                        return@get
                    }
                    val file = hostFiles.find { it.id == fileId }
                    if (file == null) {
                        call.respond(HttpStatusCode.NotFound, "File not found")
                        return@get
                    }
                    try {
                        val fileUri = Uri.parse(file.uri)
                        val fileInputStream = appContext.contentResolver.openInputStream(fileUri)
                        if (fileInputStream != null) {
                            val bytes = fileInputStream.readBytes()
                            fileInputStream.close()
                            call.response.header(HttpHeaders.ContentDisposition, attachmentDisposition(file.name))
                            call.respond(bytes)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "File not accessible")
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                    }
                }
            }
        }
        server?.start(wait = false)
    }

    private suspend fun handleUpload(call: io.ktor.server.application.ApplicationCall) {
        val contentLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
        if (contentLength > MAX_UPLOAD_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, UploadResponse(ok = false, error = "file too large"))
            return
        }
        runCatching { uploadsDir.mkdirs() }
        val saved = mutableListOf<File>()
        var error: String? = null
        try {
            val multipart = call.receiveMultipart()
            while (true) {
                val part = multipart.readPart() ?: break
                try {
                    if (part is PartData.FileItem) {
                        val original = part.originalFileName?.let { sanitizeName(it) } ?: "upload.bin"
                        val target = uniqueTarget(original)
                        try {
                            withContext(Dispatchers.IO) {
                                part.provider().use { input ->
                                    target.outputStream().use { fos ->
                                        val sink = fos.asOutput()
                                        input.copyTo(sink)
                                        sink.flush()
                                    }
                                }
                            }
                            saved.add(target)
                        } catch (e: Exception) {
                            runCatching { target.delete() }
                            if (error == null) {
                                error = e.message ?: "write failed"
                            }
                        }
                    }
                } finally {
                    part.dispose()
                }
            }
        } catch (e: Exception) {
            if (error == null) {
                error = e.message ?: "bad request"
            }
        }
        if (saved.isEmpty()) {
            saved.forEach { f -> runCatching { f.delete() } }
            call.respond(
                HttpStatusCode.BadRequest,
                UploadResponse(ok = false, error = error ?: "no file in request")
            )
            return
        }
        val added = saved.map { f -> registerUpload(f) }
        call.respond(UploadResponse(ok = true, error = error, files = added))
    }

    private fun registerUpload(file: File): SharedFile {
        uploadedFiles.find { it.id == file.name }?.let { return it }
        val shared = toUploaded(file)
        uploadedFiles.add(0, shared)
        onUploadsChanged?.invoke(uploadedFiles.toList())
        return shared
    }

    private fun toUploaded(file: File): SharedFile = SharedFile(
        id = file.name,
        name = file.name,
        size = file.length(),
        mimeType = guessMime(file.name),
        uri = file.absolutePath,
        uploaded = true,
        uploadedAt = file.lastModified()
    )

    private fun sanitizeName(raw: String): String {
        val cleaned = raw.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "_").trim().trim('.')
        return cleaned.ifEmpty { "upload.bin" }.take(180)
    }

    private fun uniqueTarget(name: String): File {
        var candidate = File(uploadsDir, name)
        if (!candidate.canonicalFile.toPath().startsWith(uploadsDir.canonicalFile.toPath())) {
            candidate = File(uploadsDir, "upload.bin")
        }
        val dot = candidate.name.lastIndexOf('.')
        val stem = if (dot > 0) candidate.name.substring(0, dot) else candidate.name
        val ext = if (dot > 0) candidate.name.substring(dot) else ""
        var i = 1
        while (!candidate.createNewFile()) {
            if (i > 9999) throw java.io.IOException("too many name collisions")
            candidate = File(uploadsDir, stem + " (" + i + ")" + ext)
            i++
        }
        return candidate
    }

    private fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun attachmentDisposition(name: String): String {
        val encoded = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
        return "attachment; filename*=UTF-8''$encoded"
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
    }

    fun isRunning(): Boolean = server != null

    companion object {
        private const val MAX_UPLOAD_BYTES = 8L * 1024 * 1024 * 1024
    }
}

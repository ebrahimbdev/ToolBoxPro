package com.toolbox.pro.fileshare.server

import android.content.Context
import android.net.Uri
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class SharedFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val mimeType: String,
    val uri: String
) : java.io.Serializable

class FileServer(
    private val appContext: Context,
    private val port: Int = 8080
) {
    private var server: ApplicationEngine? = null
    private val sharedFiles = CopyOnWriteArrayList<SharedFile>()

    fun addFile(name: String, size: Long, mimeType: String, uri: String): SharedFile {
        val file = SharedFile(name = name, size = size, mimeType = mimeType, uri = uri)
        sharedFiles.add(file)
        return file
    }

    fun setFiles(files: List<SharedFile>) {
        sharedFiles.clear()
        sharedFiles.addAll(files)
    }

    fun removeFile(fileId: String) {
        sharedFiles.removeAll { it.id == fileId }
    }

    fun clearFiles() { sharedFiles.clear() }

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
                    call.respondText(indexHtml, ContentType.Text.Html)
                }
                get("/api/files") {
                    call.respond(sharedFiles.toList())
                }
                get("/download/{fileId}") {
                    val fileId = call.parameters["fileId"]
                    if (fileId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing file ID")
                        return@get
                    }
                    val file = sharedFiles.find { it.id == fileId }
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

    fun stop() {
        server?.stop(1000, 5000)
        server = null
    }

    fun isRunning(): Boolean = server != null

    companion object {
        private val indexHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>ToolBox Pro - File Share</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:linear-gradient(135deg,#0B0F1E,#16213e);color:#eee;min-height:100vh;display:flex;justify-content:center;align-items:center;padding:20px}
.container{max-width:600px;width:100%;background:rgba(255,255,255,.05);backdrop-filter:blur(10px);border-radius:20px;padding:32px;box-shadow:0 8px 32px rgba(0,0,0,.3);border:1px solid rgba(255,255,255,.1)}
h1{text-align:center;margin-bottom:8px;background:linear-gradient(135deg,#6C63FF,#9C27B0);-webkit-background-clip:text;-webkit-text-fill-color:transparent;font-size:2em}
.subtitle{text-align:center;color:#888;margin-bottom:24px}
.file-list{list-style:none}
.file-item{display:flex;justify-content:space-between;align-items:center;padding:16px;background:rgba(255,255,255,.05);border-radius:12px;margin-bottom:12px;transition:background .2s}
.file-item:hover{background:rgba(255,255,255,.1)}
.file-name{font-weight:600;margin-bottom:4px}
.file-size{color:#888;font-size:.85em}
.download-btn{background:linear-gradient(135deg,#6C63FF,#9C27B0);color:#fff;border:none;padding:10px 20px;border-radius:8px;cursor:pointer;text-decoration:none;font-size:.9em;transition:transform .2s,box-shadow .2s}
.download-btn:hover{transform:scale(1.05);box-shadow:0 4px 15px rgba(108,99,255,.4)}
.empty{text-align:center;color:#888;padding:40px 0}
.info{text-align:center;margin-top:20px;color:#888;font-size:.85em}
.badge{display:inline-block;background:rgba(108,99,255,.2);color:#6C63FF;padding:4px 12px;border-radius:20px;font-size:.8em;margin-bottom:16px}
</style>
</head>
<body>
<div class="container">
<h1>ToolBox Pro</h1>
<p class="subtitle">Local File Sharing</p>
<div class="badge" id="count">Connecting...</div>
<ul class="file-list" id="fileList"></ul>
<p class="info" id="info">Files update automatically</p>
</div>
<script>
async function loadFiles(){try{const r=await fetch('/api/files?t='+Date.now());const f=await r.json();const l=document.getElementById('fileList');const i=document.getElementById('info');const c=document.getElementById('count');if(f.length===0){l.innerHTML='<li class="empty">No files shared yet</li>';i.textContent='Waiting for files...';c.textContent='0 files';return}l.innerHTML=f.map(x=>'<li class="file-item"><div><div class="file-name">'+x.name+'</div><div class="file-size">'+fmtSize(x.size)+'</div></div><a class="download-btn" href="/download/'+x.id+'">Download</a></li>').join('');i.textContent='Files update automatically';c.textContent=f.length+' file(s) available'}catch(e){document.getElementById('info').textContent='Connection error'}}
function fmtSize(b){if(b<1024)return b+' B';if(b<1048576)return(b/1024).toFixed(1)+' KB';return(b/1048576).toFixed(1)+' MB'}
loadFiles();setInterval(loadFiles,2000);
</script>
</body>
</html>
""".trimIndent()
    }
}

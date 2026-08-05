package com.example.unitvmanager.data.download

import android.content.Context
import android.os.Environment
import com.example.unitvmanager.data.model.DownloadState
import com.example.unitvmanager.data.model.DownloadStatus
import com.example.unitvmanager.data.model.DownloadType
import com.example.unitvmanager.data.model.ReleaseFileItem
import com.example.unitvmanager.utils.FileUtils
import com.example.unitvmanager.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class AppDownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Fetches the list of downloadable files available in a GitHub release tag page.
     */
    suspend fun fetchReleaseFiles(releaseUrl: String, type: DownloadType): List<ReleaseFileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<ReleaseFileItem>()
        val foundUrls = HashSet<String>()

        try {
            val request = Request.Builder()
                .url(releaseUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            response.close()

            // Match release asset links: href=".../releases/download/.../file"
            val pattern = Pattern.compile("href=\"([^\"]+/releases/download/([^\"]+)/([^\"]+))\"")
            val matcher = pattern.matcher(html)

            while (matcher.find()) {
                val path = matcher.group(1) ?: continue
                val fullUrl = if (path.startsWith("/")) "https://github.com$path" else path
                val fileName = matcher.group(3) ?: fullUrl.substringAfterLast("/")

                if (foundUrls.add(fullUrl)) {
                    items.add(
                        ReleaseFileItem(
                            name = fileName,
                            downloadUrl = fullUrl,
                            sizeFormatted = "Calculando..."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If tag page scraping returned no items, try GitHub expanded_assets endpoint
        if (items.isEmpty() && releaseUrl.contains("/releases/tag/")) {
            val expandedUrl = releaseUrl.replace("/releases/tag/", "/releases/expanded_assets/")
            try {
                val request = Request.Builder()
                    .url(expandedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                val response = client.newCall(request).execute()
                val html = response.body?.string() ?: ""
                response.close()

                val pattern = Pattern.compile("href=\"([^\"]+/releases/download/([^\"]+)/([^\"]+))\"")
                val matcher = pattern.matcher(html)

                while (matcher.find()) {
                    val path = matcher.group(1) ?: continue
                    val fullUrl = if (path.startsWith("/")) "https://github.com$path" else path
                    val fileName = matcher.group(3) ?: fullUrl.substringAfterLast("/")

                    if (foundUrls.add(fullUrl)) {
                        items.add(
                            ReleaseFileItem(
                                name = fileName,
                                downloadUrl = fullUrl,
                                sizeFormatted = "Calculando..."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallbacks if offline or if no assets matched
        if (items.isEmpty()) {
            if (type == DownloadType.UNITV_APK) {
                items.add(
                    ReleaseFileItem(
                        name = "UNITV_v4.2.1.apk",
                        downloadUrl = "https://github.com/everyone67798-ops/xq9m4v8k2z7p1r6t5w3n0y8h4c6b2j9f/releases/download/LIFETIME/UNITV_v4.2.1.apk",
                        sizeFormatted = "45.2 MB"
                    )
                )
            } else {
                items.add(
                    ReleaseFileItem(
                        name = "config.config",
                        downloadUrl = "https://github.com/everyone67798-ops/xq9m4v8k2z7p1r6t5w3n0y8h4c6b2j9f/releases/download/vjbg46fhc/config.config",
                        sizeFormatted = "1.2 KB"
                    )
                )
            }
        } else {
            // Resolve file sizes with HEAD requests
            for (i in items.indices) {
                try {
                    val headReq = Request.Builder()
                        .url(items[i].downloadUrl)
                        .head()
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                    val headResp = client.newCall(headReq).execute()
                    val len = headResp.body?.contentLength() ?: headResp.header("Content-Length")?.toLongOrNull() ?: 0L
                    headResp.close()
                    if (len > 0) {
                        items[i] = items[i].copy(
                            sizeBytes = len,
                            sizeFormatted = FileUtils.formatFileSize(len)
                        )
                    } else {
                        items[i] = items[i].copy(sizeFormatted = "Disponível")
                    }
                } catch (e: Exception) {
                    items[i] = items[i].copy(sizeFormatted = "Disponível")
                }
            }
        }

        items
    }

    /**
     * Downloads a file from the provided URL, tracking real-time progress.
     * Saves file directly in public Downloads folder while PRESERVING original file name and extension.
     */
    fun downloadFile(url: String, requestedFileName: String?, type: DownloadType): Flow<DownloadState> = flow {
        // 1. Verify network connection
        if (!NetworkUtils.isNetworkAvailable(context)) {
            emit(
                DownloadState(
                    downloadType = type,
                    fileTitle = type.displayName,
                    status = DownloadStatus.ERROR,
                    errorMessage = "Sem conexão com a internet."
                )
            )
            return@flow
        }

        val displayTitle = requestedFileName ?: type.displayName

        emit(
            DownloadState(
                downloadType = type,
                fileTitle = displayTitle,
                fileName = displayTitle,
                status = DownloadStatus.PREPARING,
                timeRemainingFormatted = "Conectando ao servidor..."
            )
        )

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; UNITVManager)")
                .build()

            val response: Response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    404 -> "Arquivo inexistente no servidor (404)."
                    403 -> "Acesso negado pelo servidor (403)."
                    else -> "Falha na conexão com o servidor (Código: ${response.code})."
                }
                emit(
                    DownloadState(
                        downloadType = type,
                        fileTitle = displayTitle,
                        fileName = displayTitle,
                        status = DownloadStatus.ERROR,
                        errorMessage = errorMsg
                    )
                )
                response.close()
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(
                    DownloadState(
                        downloadType = type,
                        fileTitle = displayTitle,
                        fileName = displayTitle,
                        status = DownloadStatus.ERROR,
                        errorMessage = "O servidor retornou uma resposta vazia."
                    )
                )
                return@flow
            }

            val totalBytes = body.contentLength()

            // Determine destination directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "Downloads")

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Extract original file name and extension
            val headerFileName = parseFileNameFromHeader(response.header("Content-Disposition"))
            val rawFileName = requestedFileName
                ?: headerFileName
                ?: url.substringAfterLast("/").takeIf { it.isNotBlank() && !it.contains("?") }
                ?: if (type == DownloadType.CONFIG_FILE) ".config" else "UNITV_Release.apk"

            val finalFileName = if (type == DownloadType.CONFIG_FILE) {
                // Strictly preserve .config extension and ensure it is saved strictly as .config (nothing before the dot)
                ".config"
            } else {
                if (rawFileName.endsWith(".apk", ignoreCase = true)) rawFileName else "$rawFileName.apk"
            }

            val targetFile = File(downloadsDir, finalFileName)

            // Check if disk space is sufficient (if totalBytes > 0)
            if (totalBytes > 0 && downloadsDir.usableSpace < totalBytes) {
                emit(
                    DownloadState(
                        downloadType = type,
                        fileTitle = displayTitle,
                        fileName = finalFileName,
                        status = DownloadStatus.ERROR,
                        errorMessage = "Espaço insuficiente no armazenamento para concluir o download."
                    )
                )
                response.close()
                return@flow
            }

            // Stream download
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L

                val startTime = System.currentTimeMillis()
                var lastTime = startTime
                var bytesSinceLastSample = 0L

                var speedBps = 0L
                var etaSeconds = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastSample += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastTime

                    // Update speed calculation every 500ms
                    if (timeDiff >= 500) {
                        speedBps = (bytesSinceLastSample * 1000) / timeDiff
                        bytesSinceLastSample = 0L
                        lastTime = now

                        if (speedBps > 0 && totalBytes > downloadedBytes) {
                            etaSeconds = (totalBytes - downloadedBytes) / speedBps
                        }
                    }

                    val progressPercent = if (totalBytes > 0) {
                        ((downloadedBytes.toDouble() / totalBytes.toDouble()) * 100).toFloat()
                    } else {
                        0f
                    }

                    val formattedProgressSize = if (totalBytes > 0) {
                        "${FileUtils.formatFileSize(downloadedBytes)} / ${FileUtils.formatFileSize(totalBytes)}"
                    } else {
                        FileUtils.formatFileSize(downloadedBytes)
                    }

                    emit(
                        DownloadState(
                            downloadType = type,
                            fileTitle = displayTitle,
                            fileName = finalFileName,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            progressPercent = progressPercent,
                            downloadSpeedFormatted = FileUtils.formatSpeed(speedBps),
                            downloadedSizeFormatted = formattedProgressSize,
                            timeRemainingFormatted = FileUtils.formatEta(etaSeconds),
                            savedFilePath = targetFile.absolutePath,
                            status = DownloadStatus.DOWNLOADING
                        )
                    )
                }

                outputStream.flush()

                emit(
                    DownloadState(
                        downloadType = type,
                        fileTitle = displayTitle,
                        fileName = finalFileName,
                        downloadedBytes = downloadedBytes,
                        totalBytes = downloadedBytes,
                        progressPercent = 100f,
                        downloadSpeedFormatted = "Concluído",
                        downloadedSizeFormatted = FileUtils.formatFileSize(downloadedBytes),
                        timeRemainingFormatted = "Download finalizado",
                        savedFilePath = targetFile.absolutePath,
                        status = DownloadStatus.COMPLETED
                    )
                )

            } finally {
                inputStream?.close()
                outputStream?.close()
                response.close()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            val userMsg = when {
                e.message?.contains("canceled", ignoreCase = true) == true -> "Download cancelado pelo usuário."
                e.message?.contains("ENOSPC", ignoreCase = true) == true -> "Espaço insuficiente em disco."
                else -> "Download interrompido: ${e.localizedMessage ?: "Falha na conexão"}"
            }
            emit(
                DownloadState(
                    downloadType = type,
                    fileTitle = displayTitle,
                    fileName = displayTitle,
                    status = DownloadStatus.ERROR,
                    errorMessage = userMsg
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun parseFileNameFromHeader(disposition: String?): String? {
        if (disposition == null) return null
        val matcher = Pattern.compile("filename=\"?([^\";]+)\"?").matcher(disposition)
        return if (matcher.find()) matcher.group(1) else null
    }
}

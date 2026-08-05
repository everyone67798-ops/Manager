package com.example.unitvmanager.data.model

/**
 * Enum representing the target product for download.
 */
enum class DownloadType(val displayName: String, val defaultUrl: String) {
    UNITV_APK(
        displayName = "UNITV App",
        defaultUrl = "https://github.com/everyone67798-ops/xq9m4v8k2z7p1r6t5w3n0y8h4c6b2j9f/releases/tag/LIFETIME"
    ),
    CONFIG_FILE(
        displayName = "Arquivo Config",
        defaultUrl = "https://github.com/everyone67798-ops/xq9m4v8k2z7p1r6t5w3n0y8h4c6b2j9f/releases/tag/vjbg46fhc"
    )
}

/**
 * Enum representing the status of an ongoing download.
 */
enum class DownloadStatus {
    IDLE,
    PREPARING,
    DOWNLOADING,
    COMPLETED,
    ERROR,
    CANCELLED
}

/**
 * Data class representing real-time download state and metrics.
 */
data class DownloadState(
    val downloadType: DownloadType = DownloadType.UNITV_APK,
    val fileTitle: String = "",
    val fileName: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Float = 0f,
    val downloadSpeedFormatted: String = "0 KB/s",
    val downloadedSizeFormatted: String = "0 MB / 0 MB",
    val timeRemainingFormatted: String = "Calculando...",
    val savedFilePath: String = "",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val errorMessage: String? = null
)

package com.example.unitvmanager.data.model

import android.net.Uri

/**
 * Data model representing a file or directory in the file manager.
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val sizeFormatted: String = "",
    val lastModifiedFormatted: String = "",
    val uri: Uri? = null,
    val extension: String = ""
) {
    val isConfigFile: Boolean
        get() = name.equals("config.config", ignoreCase = true) || name.endsWith(".config", ignoreCase = true)
}

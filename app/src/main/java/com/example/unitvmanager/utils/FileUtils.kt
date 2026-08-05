package com.example.unitvmanager.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    /**
     * Formats byte size into human readable string (e.g., 12.5 MB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceAtMost(units.size - 1)
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, index.toDouble())) + " " + units[index]
    }

    /**
     * Formats download speed in bytes/sec into readable string (e.g., 2.4 MB/s).
     */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        return "${formatFileSize(bytesPerSec)}/s"
    }

    /**
     * Formats remaining time in seconds to mm:ss format.
     */
    fun formatEta(seconds: Long): String {
        if (seconds <= 0 || seconds > 86400) return "Calculando..."
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) {
            String.format(Locale.getDefault(), "%02d:%02d restante", mins, secs)
        } else {
            String.format(Locale.getDefault(), "%d s restante", secs)
        }
    }

    /**
     * Formats timestamp into readable date string.
     */
    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Enforces the mandatory renaming rule for the config file.
     * Target filename must ALWAYS be exactly "config.config".
     *
     * Rules:
     * - Filename must be strictly "config"
     * - Extension must be strictly ".config"
     * - Prevents config.txt, config.xml, config, etc.
     */
    fun sanitizeConfigFileName(originalName: String?): String {
        // As per mandatory requirement: ALWAYS force exact filename "config.config"
        return "config.config"
    }

    /**
     * Ensures target file in Downloads directory is strictly named config.config.
     * If an existing config.config exists, safely handles or replaces it.
     */
    fun getTargetConfigFile(downloadsDir: File): File {
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return File(downloadsDir, "config.config")
    }

    /**
     * Returns the public Downloads folder of Android storage.
     */
    fun getPublicDownloadsDirectory(context: Context): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!publicDownloads.exists()) {
            publicDownloads.mkdirs()
        }
        return publicDownloads
    }
}

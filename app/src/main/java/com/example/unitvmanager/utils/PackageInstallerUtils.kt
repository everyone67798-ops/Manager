package com.example.unitvmanager.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object PackageInstallerUtils {

    /**
     * Prompts the Android Package Installer to install an APK file using FileProvider.
     * Returns an error message string if launching fails or if unknown sources permission is needed, or null on success.
     */
    fun promptInstallApk(context: Context, apkFile: File): String? {
        if (!apkFile.exists() || !apkFile.canRead()) {
            return "O arquivo APK baixado não foi encontrado ou não pode ser lido."
        }

        try {
            // Check unknown app sources installation permission on Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    try {
                        val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(settingsIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return "Autorize a instalação de fontes desconhecidas nas configurações e tente novamente."
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return "Não foi possível abrir o instalador do Android: ${e.localizedMessage ?: "Erro desconhecido"}"
        }
    }
}

package com.example.unitvmanager.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.unitvmanager.data.model.FileItem
import com.example.unitvmanager.data.model.OperationMode
import com.example.unitvmanager.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

sealed class FileOperationResult {
    data class Success(val message: String, val destinationPath: String) : FileOperationResult()
    data class RequiresOverwriteConfirmation(val source: FileItem, val existingTarget: FileItem, val mode: OperationMode, val targetDirPath: String) : FileOperationResult()
    data class Error(val message: String) : FileOperationResult()
}

class AppFileManager(private val context: Context) {

    /**
     * Gets default primary directory (e.g. /storage/emulated/0 or Downloads).
     */
    fun getDefaultRootDirectory(): File {
        val externalStorage = Environment.getExternalStorageDirectory()
        return if (externalStorage != null && externalStorage.exists() && externalStorage.canRead()) {
            externalStorage
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }

    /**
     * Lists files and folders in the given directory path.
     */
    suspend fun listFiles(dirPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val targetDir = File(dirPath)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext emptyList()
        }

        val rawFiles = targetDir.listFiles() ?: return@withContext emptyList()

        rawFiles.map { file ->
            val isDir = file.isDirectory
            val size = if (isDir) 0L else file.length()
            val ext = if (isDir) "" else file.extension

            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = isDir,
                sizeBytes = size,
                sizeFormatted = if (isDir) "Pasta" else FileUtils.formatFileSize(size),
                lastModifiedFormatted = FileUtils.formatDate(file.lastModified()),
                extension = ext
            )
        }.sortedWith(
            compareBy<FileItem> { !it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Copies or moves a file from source to target directory with overwrite protection.
     */
    suspend fun processFileOperation(
        source: FileItem,
        targetDirectoryPath: String,
        mode: OperationMode,
        overwriteConfirmed: Boolean = false
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(source.path)
            if (!sourceFile.exists()) {
                return@withContext FileOperationResult.Error("Arquivo inexistente: ${source.name}")
            }

            val targetDir = File(targetDirectoryPath)
            if (!targetDir.exists()) {
                val created = targetDir.mkdirs()
                if (!created && !targetDir.exists()) {
                    return@withContext FileOperationResult.Error("Falta de permissão ou falha ao criar a pasta de destino.")
                }
            }

            // Determine final file name (Enforce config.config naming rules if copying/moving config)
            val finalFileName = if (source.isConfigFile || source.name.lowercase().startsWith("config")) {
                "config.config"
            } else {
                source.name
            }

            val destinationFile = File(targetDir, finalFileName)

            // Overwrite check
            if (destinationFile.exists() && !overwriteConfirmed) {
                val existingTargetItem = FileItem(
                    name = destinationFile.name,
                    path = destinationFile.absolutePath,
                    isDirectory = destinationFile.isDirectory,
                    sizeBytes = destinationFile.length(),
                    sizeFormatted = FileUtils.formatFileSize(destinationFile.length()),
                    lastModifiedFormatted = FileUtils.formatDate(destinationFile.lastModified()),
                    extension = destinationFile.extension
                )
                return@withContext FileOperationResult.RequiresOverwriteConfirmation(
                    source = source,
                    existingTarget = existingTargetItem,
                    mode = mode,
                    targetDirPath = targetDirectoryPath
                )
            }

            // Verify disk space for copy operation
            if (mode == OperationMode.COPY && targetDir.usableSpace < sourceFile.length()) {
                return@withContext FileOperationResult.Error("Espaço insuficiente em disco na pasta de destino.")
            }

            if (mode == OperationMode.COPY) {
                copyFileStream(sourceFile, destinationFile)
                return@withContext FileOperationResult.Success(
                    message = "Arquivo '${destinationFile.name}' copiado com sucesso!",
                    destinationPath = destinationFile.absolutePath
                )
            } else if (mode == OperationMode.MOVE) {
                val moved = sourceFile.renameTo(destinationFile)
                if (!moved) {
                    // Fallback to copy then delete
                    copyFileStream(sourceFile, destinationFile)
                    sourceFile.delete()
                }
                return@withContext FileOperationResult.Success(
                    message = "Arquivo '${destinationFile.name}' movido com sucesso!",
                    destinationPath = destinationFile.absolutePath
                )
            }

            FileOperationResult.Error("Modo de operação inválido.")
        } catch (e: Exception) {
            e.printStackTrace()
            FileOperationResult.Error("Falha ao copiar/mover arquivo: ${e.localizedMessage ?: "Erro desconhecido"}")
        }
    }

    /**
     * Copies a document file using SAF Uri if standard File API cannot write directly.
     */
    suspend fun copyFileFromUri(
        sourceUri: Uri,
        targetDirectoryPath: String,
        overwriteConfirmed: Boolean = false
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val docFile = DocumentFile.fromSingleUri(context, sourceUri)
                ?: return@withContext FileOperationResult.Error("Arquivo de origem inválido.")

            val rawName = docFile.name ?: "config.config"
            val targetDir = File(targetDirectoryPath)
            if (!targetDir.exists()) targetDir.mkdirs()

            // Enforce config.config extension rule
            val finalName = if (rawName.lowercase().contains("config")) "config.config" else rawName
            val destFile = File(targetDir, finalName)

            if (destFile.exists() && !overwriteConfirmed) {
                val existingItem = FileItem(
                    name = destFile.name,
                    path = destFile.absolutePath,
                    isDirectory = destFile.isDirectory,
                    sizeBytes = destFile.length(),
                    sizeFormatted = FileUtils.formatFileSize(destFile.length()),
                    lastModifiedFormatted = FileUtils.formatDate(destFile.lastModified())
                )
                val sourceItem = FileItem(
                    name = docFile.name ?: "Arquivo",
                    path = sourceUri.toString(),
                    isDirectory = false,
                    sizeBytes = docFile.length(),
                    sizeFormatted = FileUtils.formatFileSize(docFile.length()),
                    uri = sourceUri
                )
                return@withContext FileOperationResult.RequiresOverwriteConfirmation(
                    source = sourceItem,
                    existingTarget = existingItem,
                    mode = OperationMode.COPY,
                    targetDirPath = targetDirectoryPath
                )
            }

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext FileOperationResult.Error("Não foi possível ler o arquivo selecionado.")

            FileOperationResult.Success(
                message = "Arquivo '$finalName' salvo na pasta de destino.",
                destinationPath = destFile.absolutePath
            )
        } catch (e: Exception) {
            FileOperationResult.Error("Falha ao processar arquivo SAF: ${e.localizedMessage}")
        }
    }

    private fun copyFileStream(source: File, dest: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }
}

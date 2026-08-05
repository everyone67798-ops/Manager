package com.example.unitvmanager.data.model

/**
 * Mode of file clipboard operation.
 */
enum class OperationMode {
    NONE,
    COPY,
    MOVE
}

/**
 * Data model for clipboard and active file operations (copy/move/paste).
 */
data class FileOperationState(
    val mode: OperationMode = OperationMode.NONE,
    val sourceItem: FileItem? = null,
    val targetDirectoryPath: String? = null,
    val pendingOverwriteItem: FileItem? = null,
    val isOperating: Boolean = false,
    val operationSuccessMessage: String? = null,
    val errorMessage: String? = null
)

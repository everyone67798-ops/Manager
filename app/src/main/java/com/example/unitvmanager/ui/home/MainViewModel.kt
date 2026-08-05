package com.example.unitvmanager.ui.home

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unitvmanager.data.download.AppDownloadManager
import com.example.unitvmanager.data.file.AppFileManager
import com.example.unitvmanager.data.file.FileOperationResult
import com.example.unitvmanager.data.model.DownloadState
import com.example.unitvmanager.data.model.DownloadStatus
import com.example.unitvmanager.data.model.DownloadType
import com.example.unitvmanager.data.model.FileItem
import com.example.unitvmanager.data.model.FileOperationState
import com.example.unitvmanager.data.model.OperationMode
import com.example.unitvmanager.data.model.ReleaseFileItem
import com.example.unitvmanager.data.model.UserMessage
import com.example.unitvmanager.utils.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

import com.example.unitvmanager.utils.PackageInstallerUtils

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = AppDownloadManager(application)
    private val fileManager = AppFileManager(application)

    private var pendingApkFile: File? = null

    // Apply Config Overwrite state
    private val _showApplyConfigOverwriteDialog = MutableStateFlow(false)
    val showApplyConfigOverwriteDialog: StateFlow<Boolean> = _showApplyConfigOverwriteDialog.asStateFlow()

    private var pendingApplyConfigSourceFile: File? = null
    private var pendingApplyConfigTargetFile: File? = null

    // Download state
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null

    // Release Files Dialog state
    private val _isReleaseDialogOpen = MutableStateFlow(false)
    val isReleaseDialogOpen: StateFlow<Boolean> = _isReleaseDialogOpen.asStateFlow()

    private val _activeDownloadType = MutableStateFlow(DownloadType.UNITV_APK)
    val activeDownloadType: StateFlow<DownloadType> = _activeDownloadType.asStateFlow()

    private val _releaseFiles = MutableStateFlow<List<ReleaseFileItem>>(emptyList())
    val releaseFiles: StateFlow<List<ReleaseFileItem>> = _releaseFiles.asStateFlow()

    private val _isReleaseFilesLoading = MutableStateFlow(false)
    val isReleaseFilesLoading: StateFlow<Boolean> = _isReleaseFilesLoading.asStateFlow()

    // File Manager state
    private val _isFileManagerOpen = MutableStateFlow(false)
    val isFileManagerOpen: StateFlow<Boolean> = _isFileManagerOpen.asStateFlow()

    private val _currentPath = MutableStateFlow(fileManager.getDefaultRootDirectory().absolutePath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _filesList = MutableStateFlow<List<FileItem>>(emptyList())
    val filesList: StateFlow<List<FileItem>> = _filesList.asStateFlow()

    private val _fileOperationState = MutableStateFlow(FileOperationState())
    val fileOperationState: StateFlow<FileOperationState> = _fileOperationState.asStateFlow()

    // Snackbars / User Notifications
    private val _userMessage = MutableSharedFlow<UserMessage>()
    val userMessage: SharedFlow<UserMessage> = _userMessage.asSharedFlow()

    init {
        refreshFilesList()
    }

    // --- Release Dialog Controls ---
    fun openReleaseDialog(type: DownloadType) {
        _activeDownloadType.value = type
        _isReleaseDialogOpen.value = true
        fetchReleaseFiles()
    }

    fun closeReleaseDialog() {
        _isReleaseDialogOpen.value = false
    }

    fun fetchReleaseFiles() {
        val type = _activeDownloadType.value
        _isReleaseFilesLoading.value = true
        viewModelScope.launch {
            val items = downloadManager.fetchReleaseFiles(type.defaultUrl, type)
            _releaseFiles.value = items
            _isReleaseFilesLoading.value = false
        }
    }

    // --- Download Controls ---
    fun startDownloadFile(fileItem: ReleaseFileItem, type: DownloadType) {
        _isReleaseDialogOpen.value = false // Close release dialog immediately
        downloadJob?.cancel()

        downloadJob = viewModelScope.launch {
            downloadManager.downloadFile(fileItem.downloadUrl, fileItem.name, type).collect { state ->
                _downloadState.value = state
                if (state.status == DownloadStatus.COMPLETED) {
                    refreshFilesList()
                    val msg = "Download concluído com sucesso: ${state.fileName}"
                    _userMessage.emit(UserMessage(text = msg))

                    // Automatically trigger APK installation if downloaded file is an APK
                    if (state.downloadType == DownloadType.UNITV_APK || state.fileName.endsWith(".apk", ignoreCase = true) || state.savedFilePath.endsWith(".apk", ignoreCase = true)) {
                        val apkFile = File(state.savedFilePath)
                        if (apkFile.exists()) {
                            pendingApkFile = apkFile
                            val installError = PackageInstallerUtils.promptInstallApk(getApplication(), apkFile)
                            if (installError != null) {
                                _userMessage.emit(UserMessage(text = installError))
                            }
                        } else {
                            _userMessage.emit(UserMessage(text = "Não foi possível localizar o arquivo APK para instalação."))
                        }
                    }
                } else if (state.status == DownloadStatus.ERROR) {
                    _userMessage.emit(UserMessage(text = state.errorMessage ?: "Falha no download."))
                }
            }
        }
    }

    fun triggerPendingApkInstallation() {
        val file = pendingApkFile ?: return
        if (file.exists()) {
            val err = PackageInstallerUtils.promptInstallApk(getApplication(), file)
            if (err != null) {
                viewModelScope.launch {
                    _userMessage.emit(UserMessage(text = err))
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadState.update { it.copy(status = DownloadStatus.CANCELLED) }
        viewModelScope.launch {
            _userMessage.emit(UserMessage(text = "Download cancelado pelo usuário."))
        }
    }

    fun dismissDownloadDialog() {
        _downloadState.value = DownloadState()
    }

    // --- File Manager Controls ---
    fun openFileManager() {
        _isFileManagerOpen.value = true
        refreshFilesList()
    }

    fun closeFileManager() {
        _isFileManagerOpen.value = false
        _fileOperationState.value = FileOperationState()
    }

    fun navigateToDirectory(path: String) {
        _currentPath.value = path
        refreshFilesList()
    }

    fun navigateToParentDirectory() {
        val currentFile = File(_currentPath.value)
        val parent = currentFile.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            _currentPath.value = parent.absolutePath
            refreshFilesList()
        }
    }

    fun navigateToHome() {
        _currentPath.value = fileManager.getDefaultRootDirectory().absolutePath
        refreshFilesList()
    }

    fun navigateToDownloads() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null && downloadsDir.exists()) {
            _currentPath.value = downloadsDir.absolutePath
            refreshFilesList()
        }
    }

    fun refreshFilesList() {
        viewModelScope.launch {
            _filesList.value = fileManager.listFiles(_currentPath.value)
        }
    }

    // --- File Operations (Copy / Move / Paste / SAF) ---
    fun startFileOperation(source: FileItem, mode: OperationMode) {
        _fileOperationState.value = FileOperationState(
            mode = mode,
            sourceItem = source
        )
        viewModelScope.launch {
            _userMessage.emit(
                UserMessage(
                    text = "Modo '${if (mode == OperationMode.COPY) "Copiar" else "Mover"}' ativado para ${source.name}. Navegue até a pasta de destino."
                )
            )
        }
    }

    fun cancelFileOperation() {
        _fileOperationState.value = FileOperationState()
    }

    fun executePaste(overwriteConfirmed: Boolean = false) {
        val currentState = _fileOperationState.value
        val source = currentState.sourceItem ?: return
        val targetDirPath = _currentPath.value

        _fileOperationState.update { it.copy(isOperating = true) }

        viewModelScope.launch {
            val result = if (source.uri != null) {
                fileManager.copyFileFromUri(source.uri, targetDirPath, overwriteConfirmed)
            } else {
                fileManager.processFileOperation(source, targetDirPath, currentState.mode, overwriteConfirmed)
            }

            when (result) {
                is FileOperationResult.Success -> {
                    _fileOperationState.value = FileOperationState()
                    refreshFilesList()
                    _userMessage.emit(UserMessage(text = result.message))
                }
                is FileOperationResult.RequiresOverwriteConfirmation -> {
                    _fileOperationState.update {
                        it.copy(
                            pendingOverwriteItem = result.existingTarget,
                            isOperating = false
                        )
                    }
                }
                is FileOperationResult.Error -> {
                    _fileOperationState.update { it.copy(isOperating = false) }
                    _userMessage.emit(UserMessage(text = result.message))
                }
            }
        }
    }

    fun confirmOverwrite() {
        _fileOperationState.update { it.copy(pendingOverwriteItem = null) }
        executePaste(overwriteConfirmed = true)
    }

    fun cancelOverwrite() {
        _fileOperationState.update { it.copy(pendingOverwriteItem = null) }
    }

    fun importSafUri(uri: Uri) {
        val targetDirPath = _currentPath.value
        viewModelScope.launch {
            val result = fileManager.copyFileFromUri(uri, targetDirPath, false)
            when (result) {
                is FileOperationResult.Success -> {
                    refreshFilesList()
                    _userMessage.emit(UserMessage(text = result.message))
                }
                is FileOperationResult.RequiresOverwriteConfirmation -> {
                    _fileOperationState.value = FileOperationState(
                        mode = OperationMode.COPY,
                        sourceItem = result.source,
                        pendingOverwriteItem = result.existingTarget
                    )
                }
                is FileOperationResult.Error -> {
                    _userMessage.emit(UserMessage(text = result.message))
                }
            }
        }
    }

    /**
     * Direct logic for "Aplicar Config":
     * Automatically locates .config file in Downloads and copies it directly to /storage/emulated/0/Android.
     * Prompts for overwrite confirmation if a file with the same name already exists.
     */
    fun applyConfigFileDirectly(overwriteConfirmed: Boolean = false) {
        viewModelScope.launch {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: File(Environment.getExternalStorageDirectory(), "Download")

            if (!downloadsDir.exists() || !downloadsDir.isDirectory) {
                _userMessage.emit(UserMessage(text = "Nenhum arquivo .config encontrado na pasta Downloads. Por favor, faça o download primeiro."))
                return@launch
            }

            val configFiles = downloadsDir.listFiles()?.filter { file ->
                !file.isDirectory && file.name.endsWith(".config", ignoreCase = true)
            }?.sortedByDescending { it.lastModified() }

            if (configFiles.isNullOrEmpty()) {
                _userMessage.emit(UserMessage(text = "Nenhum arquivo .config encontrado na pasta Downloads. Por favor, faça o download primeiro."))
                return@launch
            }

            val sourceFile = configFiles.first()

            val targetAndroidDir = File(Environment.getExternalStorageDirectory(), "Android")
            if (!targetAndroidDir.exists()) {
                val created = targetAndroidDir.mkdirs()
                if (!created && !targetAndroidDir.exists()) {
                    _userMessage.emit(UserMessage(text = "Sem permissão para criar/acessar a pasta Android. Conceda permissão nas configurações."))
                    return@launch
                }
            }

            val targetFile = File(targetAndroidDir, sourceFile.name)

            if (targetFile.exists() && !overwriteConfirmed) {
                pendingApplyConfigSourceFile = sourceFile
                pendingApplyConfigTargetFile = targetFile
                _showApplyConfigOverwriteDialog.value = true
                return@launch
            }

            _showApplyConfigOverwriteDialog.value = false

            try {
                sourceFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _userMessage.emit(UserMessage(text = "Arquivo '${sourceFile.name}' aplicado com sucesso na pasta Android!"))
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit(UserMessage(text = "Falha ao copiar para a pasta Android: ${e.localizedMessage ?: "Erro de acesso"}"))
            }
        }
    }

    fun confirmApplyConfigOverwrite() {
        _showApplyConfigOverwriteDialog.value = false
        applyConfigFileDirectly(overwriteConfirmed = true)
    }

    fun cancelApplyConfigOverwrite() {
        _showApplyConfigOverwriteDialog.value = false
        pendingApplyConfigSourceFile = null
        pendingApplyConfigTargetFile = null
    }
}

package com.example.unitvmanager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBackgroundGradientTop
import com.example.ui.theme.OrangePrimary
import com.example.unitvmanager.data.model.DownloadStatus
import com.example.unitvmanager.data.model.DownloadType
import com.example.unitvmanager.ui.components.BigActionButton
import com.example.unitvmanager.ui.components.ConfirmOverwriteDialog
import com.example.unitvmanager.ui.components.DownloadProgressDialog
import com.example.unitvmanager.ui.components.ErrorSnackbarHost
import com.example.unitvmanager.ui.components.FileManagerDialog
import com.example.unitvmanager.ui.components.ReleaseFileSelectionDialog
import kotlinx.coroutines.flow.collectLatest

import com.example.unitvmanager.ui.components.ConfirmSimpleOverwriteDialog

/**
 * Premium Home Screen for UNITV Manager.
 * Features black background with subtle gradient, large rounded cards (30dp),
 * bottom orange glow effects, orange rounded square icons, white titles,
 * and light gray subtitles.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel
) {
    val downloadState by viewModel.downloadState.collectAsState()

    val isReleaseDialogOpen by viewModel.isReleaseDialogOpen.collectAsState()
    val activeDownloadType by viewModel.activeDownloadType.collectAsState()
    val releaseFiles by viewModel.releaseFiles.collectAsState()
    val isReleaseFilesLoading by viewModel.isReleaseFilesLoading.collectAsState()

    val showApplyConfigOverwriteDialog by viewModel.showApplyConfigOverwriteDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Collect user notifications
    LaunchedEffect(key1 = true) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg.text)
        }
    }

    Scaffold(
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackgroundGradientTop,
                            DarkBackground,
                            DarkBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // Top Header Section: Title & Subtitle (Centered, No Icon)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "UNITV Manager",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Celular e TV Box",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // CARD 1: Baixar UNITV
                BigActionButton(
                    title = "Baixar UNITV",
                    subtitle = "Escolha o APK na lista e instale",
                    icon = Icons.Default.Tv,
                    onClick = {
                        viewModel.openReleaseDialog(DownloadType.UNITV_APK)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // CARD 2: Baixar Config
                BigActionButton(
                    title = "Baixar Config",
                    subtitle = "Salva o arquivo na pasta Downloads",
                    icon = Icons.Default.Download,
                    onClick = {
                        viewModel.openReleaseDialog(DownloadType.CONFIG_FILE)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // CARD 3: Aplicar Config
                BigActionButton(
                    title = "Aplicar Config",
                    subtitle = "Copia o arquivo .config para a pasta Android",
                    icon = Icons.Default.FolderCopy,
                    onClick = {
                        viewModel.applyConfigFileDirectly()
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Bottom explanatory text in light gray
                Text(
                    text = "Compatível com Controle Remoto (DPAD) e Toque. Todos os downloads são salvos na pasta Downloads.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // --- OVERLAY DIALOG 1: Direct Release Files Selector ---
            if (isReleaseDialogOpen) {
                ReleaseFileSelectionDialog(
                    downloadType = activeDownloadType,
                    filesList = releaseFiles,
                    isLoading = isReleaseFilesLoading,
                    onRefresh = { viewModel.fetchReleaseFiles() },
                    onClose = { viewModel.closeReleaseDialog() },
                    onSelectFile = { item ->
                        viewModel.startDownloadFile(item, activeDownloadType)
                    }
                )
            }

            // --- OVERLAY DIALOG 2: Download Progress Screen ---
            if (downloadState.status != DownloadStatus.IDLE) {
                DownloadProgressDialog(
                    state = downloadState,
                    onCancel = { viewModel.cancelDownload() },
                    onDismiss = { viewModel.dismissDownloadDialog() },
                    onApplyConfigDirectly = {
                        viewModel.applyConfigFileDirectly()
                    },
                    onInstallApk = {
                        viewModel.triggerPendingApkInstallation()
                    }
                )
            }

            // --- OVERLAY DIALOG 3: Overwrite Confirmation for Aplicar Config ---
            if (showApplyConfigOverwriteDialog) {
                ConfirmSimpleOverwriteDialog(
                    fileName = ".config",
                    onConfirm = { viewModel.confirmApplyConfigOverwrite() },
                    onCancel = { viewModel.cancelApplyConfigOverwrite() }
                )
            }
        }
    }
}

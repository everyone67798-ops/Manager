package com.example.unitvmanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.unitvmanager.data.model.DownloadState
import com.example.unitvmanager.data.model.DownloadStatus
import com.example.unitvmanager.data.model.DownloadType

/**
 * Premium Download Progress Dialog aligned with black and orange aesthetic.
 */
@Composable
fun DownloadProgressDialog(
    state: DownloadState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onApplyConfigDirectly: (() -> Unit)? = null,
    onInstallApk: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (state.progressPercent / 100f).coerceIn(0f, 1f),
        label = "progress"
    )

    Dialog(
        onDismissRequest = {
            if (state.status == DownloadStatus.COMPLETED || state.status == DownloadStatus.ERROR || state.status == DownloadStatus.CANCELLED) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                val iconBgColor = when (state.status) {
                    DownloadStatus.COMPLETED -> StatusSuccess.copy(alpha = 0.2f)
                    DownloadStatus.ERROR -> StatusError.copy(alpha = 0.2f)
                    else -> OrangePrimary
                }

                val iconTint = when (state.status) {
                    DownloadStatus.COMPLETED -> StatusSuccess
                    DownloadStatus.ERROR -> StatusError
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state.status) {
                            DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                            DownloadStatus.ERROR -> Icons.Default.Warning
                            else -> Icons.Default.Download
                        },
                        contentDescription = "Status",
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = when (state.status) {
                        DownloadStatus.COMPLETED -> "Download Concluído!"
                        DownloadStatus.ERROR -> "Erro no Download"
                        DownloadStatus.CANCELLED -> "Download Cancelado"
                        else -> "Baixando ${state.fileTitle}"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // File Name
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arquivo: ",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF9E9E9E)
                        )
                        Text(
                            text = if (state.fileName.isNotEmpty()) state.fileName else state.fileTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = OrangePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Downloading State: metrics & progress bar
                if (state.status == DownloadStatus.DOWNLOADING || state.status == DownloadStatus.PREPARING) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progresso",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9E9E9E)
                        )
                        Text(
                            text = "${state.progressPercent.toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OrangePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = OrangePrimary,
                        trackColor = DarkSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricRow(label = "Velocidade:", value = state.downloadSpeedFormatted)
                        MetricRow(label = "Tamanho:", value = state.downloadedSizeFormatted)
                        MetricRow(label = "Tempo restante:", value = state.timeRemainingFormatted)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StatusError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Cancelar Download", fontWeight = FontWeight.Bold)
                    }
                }

                // Completion State
                if (state.status == DownloadStatus.COMPLETED) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusSuccess.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val successText = if (state.downloadType == DownloadType.CONFIG_FILE) {
                                    "Arquivo .config salvo na pasta Downloads."
                                } else {
                                    "Arquivo salvo com sucesso em Downloads."
                                }
                                Text(
                                    text = successText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusSuccess,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Local",
                                        tint = Color(0xFF9E9E9E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = state.savedFilePath,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if ((state.downloadType == DownloadType.UNITV_APK || state.savedFilePath.endsWith(".apk")) && onInstallApk != null) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onInstallApk()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Instalar APK", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else if (state.downloadType == DownloadType.CONFIG_FILE && onApplyConfigDirectly != null) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onApplyConfigDirectly()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Aplicar Config", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Error State
                if (state.status == DownloadStatus.ERROR || state.status == DownloadStatus.CANCELLED) {
                    Text(
                        text = state.errorMessage ?: "Não foi possível concluir o download.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9E9E9E)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

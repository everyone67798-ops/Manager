package com.example.unitvmanager.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryAmber
import com.example.unitvmanager.data.model.FileItem
import com.example.unitvmanager.data.model.FileOperationState
import com.example.unitvmanager.data.model.OperationMode

/**
 * Internal File Manager dialog for "Aplicar Config".
 * Provides:
 * - Folder navigation & listing
 * - File selection
 * - Copy / Move / Paste operations
 * - Origin and Destination selection
 * - Overwrite confirmation check
 * - Storage Access Framework (SAF) tree picker integration
 */
@Composable
fun FileManagerDialog(
    currentPath: String,
    filesList: List<FileItem>,
    operationState: FileOperationState,
    onNavigateDirectory: (String) -> Unit,
    onNavigateParent: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateDownloads: () -> Unit,
    onStartOperation: (source: FileItem, mode: OperationMode) -> Unit,
    onCancelOperation: () -> Unit,
    onExecutePaste: () -> Unit,
    onSelectSafUri: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedItem by remember { mutableStateOf<FileItem?>(null) }

    // SAF Document Tree Launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onSelectSafUri(it) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    color = DarkSurface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateParent) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar Pasta",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gerenciador de Arquivos",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateHome) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Início",
                                    tint = PrimaryCyan
                                )
                            }
                            IconButton(onClick = onNavigateDownloads) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = "Downloads",
                                    tint = SecondaryAmber
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Path Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pasta: $currentPath",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { safLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Importar SAF", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Main Files List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (filesList.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Vazio",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Esta pasta está vazia.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filesList) { item ->
                                FileItemRow(
                                    item = item,
                                    isSelected = selectedItem?.path == item.path,
                                    onClick = {
                                        if (item.isDirectory) {
                                            selectedItem = null
                                            onNavigateDirectory(item.path)
                                        } else {
                                            selectedItem = if (selectedItem?.path == item.path) null else item
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Active Action / Clipboard Footer Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Scenario A: An item is selected, user wants to copy or move it
                        if (selectedItem != null && operationState.mode == OperationMode.NONE) {
                            val item = selectedItem!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Selecionado: ${item.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (item.isConfigFile) SecondaryAmber else PrimaryCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.sizeFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onStartOperation(item, OperationMode.COPY)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copiar",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copiar", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            onStartOperation(item, OperationMode.MOVE)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                            contentDescription = "Mover",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mover", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Scenario B: Clipboard operation active (Copy or Move ready to Paste)
                        if (operationState.mode != OperationMode.NONE && operationState.sourceItem != null) {
                            val source = operationState.sourceItem
                            val modeText = if (operationState.mode == OperationMode.COPY) "Copiando" else "Movendo"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$modeText: ${source.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SecondaryAmber,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Navegue até a pasta de destino e clique em Colar Aqui.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onCancelOperation,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Cancelar")
                                    }

                                    Button(
                                        onClick = onExecutePaste,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Colar",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Colar Aqui", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Default hint if no item selected and no operation active
                        if (selectedItem == null && operationState.mode == OperationMode.NONE) {
                            Text(
                                text = "Toque em um arquivo para selecionar as opções de Copiar ou Mover.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        PrimaryCyan.copy(alpha = 0.15f)
    } else {
        DarkSurface
    }

    val iconVector = when {
        item.isDirectory -> Icons.Default.Folder
        item.isConfigFile -> Icons.Default.Settings
        else -> Icons.Default.Description
    }

    val iconTint = when {
        item.isDirectory -> PrimaryCyan
        item.isConfigFile -> SecondaryAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .focusable(),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = if (item.isDirectory) "Pasta" else "Arquivo",
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (item.isConfigFile || item.isDirectory) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (item.isConfigFile) SecondaryAmber else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.isDirectory) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.sizeFormatted} • ${item.lastModifiedFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selecionado",
                    tint = PrimaryCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

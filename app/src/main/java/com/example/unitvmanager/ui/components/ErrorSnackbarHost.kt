package com.example.unitvmanager.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusError

/**
 * Custom styled SnackbarHost for presenting user messages and error notifications.
 */
@Composable
fun ErrorSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(16.dp)
    ) { data ->
        val isError = data.visuals.message.contains("Erro", ignoreCase = true) ||
                data.visuals.message.contains("Falha", ignoreCase = true) ||
                data.visuals.message.contains("Sem conexão", ignoreCase = true) ||
                data.visuals.message.contains("Permissão", ignoreCase = true) ||
                data.visuals.message.contains("insuficiente", ignoreCase = true)

        Snackbar(
            containerColor = DarkSurface,
            contentColor = if (isError) StatusError else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(16.dp),
            action = {
                data.visuals.actionLabel?.let { label ->
                    TextButton(onClick = { data.performAction() }) {
                        Text(
                            text = label,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

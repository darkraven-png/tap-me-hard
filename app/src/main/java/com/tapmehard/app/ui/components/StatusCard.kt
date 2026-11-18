package com.tapmehard.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tapmehard.app.ui.theme.GreenGranted
import com.tapmehard.app.ui.theme.RedDenied

data class PermissionStatus(
    val label: String,
    val granted: Boolean,
    val onFix: () -> Unit
)

@Composable
fun StatusCard(
    permissions: List<PermissionStatus>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Permissions",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            permissions.forEach { perm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (perm.granted) Icons.Filled.CheckCircle
                                      else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (perm.granted) GreenGranted else RedDenied,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = perm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (!perm.granted) {
                        TextButton(onClick = perm.onFix) {
                            Text("Grant")
                        }
                    }
                }
            }
        }
    }
}

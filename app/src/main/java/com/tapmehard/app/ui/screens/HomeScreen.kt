package com.tapmehard.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tapmehard.app.service.TapDetectionService
import com.tapmehard.app.ui.components.AutomationItem
import com.tapmehard.app.ui.components.PermissionStatus
import com.tapmehard.app.ui.components.StatusCard
import com.tapmehard.app.util.PermissionUtils
import com.tapmehard.app.viewmodel.MainViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddAutomation: () -> Unit
) {
    val context = LocalContext.current
    val sensitivity by viewModel.sensitivity.collectAsState()
    val sequenceDelay by viewModel.sequenceDelay.collectAsState()
    val serviceEnabled by viewModel.serviceEnabled.collectAsState()
    val rules by viewModel.automationRules.collectAsState()
    val lastTapInfo by TapDetectionService.lastTapMessage.collectAsState()

    // Refresh permission status whenever the screen is composed
    var permRefreshKey by remember { mutableIntStateOf(0) }
    val accessibilityGranted = remember(permRefreshKey) {
        PermissionUtils.isAccessibilityEnabled(context)
    }
    val overlayGranted = remember(permRefreshKey) {
        PermissionUtils.canDrawOverlays(context)
    }
    val batteryUnrestricted = remember(permRefreshKey) {
        PermissionUtils.isBatteryUnrestricted(context)
    }

    // POST_NOTIFICATIONS runtime permission (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored; user sees the status card */ }

    // Refresh permissions on resume
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        permRefreshKey++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TapMeHard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAutomation,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                text = { Text("Add Automation") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ---- Master toggle ----
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tap Detection",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (serviceEnabled) "Active — listening for taps"
                                       else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = serviceEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.toggleService(enabled)
                            }
                        )
                    }
                }
            }

            // ---- Live Tap Feedback Banner (Success & Wrong Tap) ----
            lastTapInfo?.let { info ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (info.isSuccess) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (info.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (info.isSuccess) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (info.isSuccess) "Tap Action Triggered" else "Wrong Tap Detected",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (info.isSuccess) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                                Text(
                                    text = info.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (info.isSuccess) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ---- Permission status card ----
            item {
                StatusCard(
                    permissions = listOf(
                        PermissionStatus(
                            label = "Accessibility Service",
                            granted = accessibilityGranted,
                            onFix = {
                                PermissionUtils.openAccessibilitySettings(context)
                                permRefreshKey++
                            }
                        ),
                        PermissionStatus(
                            label = "Overlay Permission",
                            granted = overlayGranted,
                            onFix = {
                                PermissionUtils.openOverlaySettings(context)
                                permRefreshKey++
                            }
                        ),
                        PermissionStatus(
                            label = "Unrestricted Battery",
                            granted = batteryUnrestricted,
                            onFix = {
                                PermissionUtils.openBatterySettings(context)
                                permRefreshKey++
                            }
                        )
                    )
                )
            }

            // ---- Sensitivity slider ----
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tap Sensitivity",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Z-axis force threshold — lower = more sensitive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = sensitivity,
                            onValueChange = { viewModel.setSensitivity(it) },
                            valueRange = 5f..30f,
                            steps = 24
                        )
                        Text(
                            text = "Threshold: ${"%.1f".format(sensitivity)} m/s²",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // ---- Sequence wait delay slider ----
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sequence Wait Delay",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Time to wait for more taps before firing action",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = sequenceDelay.toFloat(),
                            onValueChange = {
                                viewModel.setSequenceDelay(it.roundToLong())
                            },
                            valueRange = 300f..800f,
                            steps = 9
                        )
                        Text(
                            text = "${sequenceDelay} ms",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // ---- Automation rules header ----
            if (rules.isNotEmpty()) {
                item {
                    Text(
                        text = "Automations",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ---- Automation list ----
            items(rules, key = { it.id }) { rule ->
                AutomationItem(
                    rule = rule,
                    onToggle = { enabled -> viewModel.toggleRule(rule, enabled) },
                    onDelete = { viewModel.deleteRule(rule.id) }
                )
            }

            // ---- Bottom spacer for FAB ----
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

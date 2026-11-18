package com.tapmehard.app.ui.screens

import android.hardware.SensorManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapmehard.app.data.ActionType
import com.tapmehard.app.data.AutomationRule
import com.tapmehard.app.service.TapDetector
import com.tapmehard.app.ui.components.AppInfo
import com.tapmehard.app.ui.components.AppPickerDialog
import com.tapmehard.app.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

// ---- Morse Code Presets ----
private data class MorsePreset(
    val label: String,
    val sequence: String,
    val description: String
)

private val presets = listOf(
    MorsePreset("Double Tap (..)", "..", "Two quick taps"),
    MorsePreset("Double Dash (--)", "--", "Two hard taps"),
    MorsePreset("Triple Tap (...)", "...", "Three quick taps"),
    MorsePreset("Dot Dash (.-)", ".-", "Morse 'A': light tap then hard tap"),
    MorsePreset("Dash Dot (-.)", "-.", "Morse 'N': hard tap then light tap")
)

// ---- Quick Utility actions ----
private data class QuickAction(
    val label: String,
    val type: ActionType,
    val icon: @Composable () -> Unit
)

private val quickActions = listOf(
    QuickAction("Toggle Flashlight", ActionType.TOGGLE_FLASHLIGHT) {
        Icon(Icons.Filled.FlashlightOn, contentDescription = null)
    },
    QuickAction("Play / Pause Media", ActionType.PLAY_PAUSE_MEDIA) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
    },
    QuickAction("Take Screenshot", ActionType.TAKE_SCREENSHOT) {
        Icon(Icons.Filled.Screenshot, contentDescription = null)
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAutomationScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // ---- Step 1 state ----
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var isCustomMode by remember { mutableStateOf(false) }
    var customSequenceInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    // ---- Step 2 state ----
    var selectedActionType by remember { mutableStateOf<ActionType?>(null) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    // ---- Name ----
    var automationName by remember { mutableStateOf("") }

    // Current sequence resolution
    val activeSequence = if (isCustomMode) {
        customSequenceInput.trim()
    } else {
        presets[selectedPresetIndex].sequence
    }

    // Sequence Guard check: must strictly contain only '-' and '.'
    val isSequenceGuarded = AutomationRule.isValidSequence(activeSequence)

    // ---- Live recorder ----
    val recorderScope = remember { CoroutineScope(Dispatchers.Default) }
    val recorder = remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        TapDetector(sm, recorderScope)
    }

    // Collect recording results
    LaunchedEffect(Unit) {
        recorder.recordingResult.collect { result ->
            // Filter recorded sequence with the morse guard
            if (AutomationRule.isValidSequence(result.morseSequence)) {
                customSequenceInput = result.morseSequence
            }
            isRecording = false
            recorder.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.stop() }
    }

    val canSave = automationName.isNotBlank() &&
                  isSequenceGuarded &&
                  selectedActionType != null &&
                  (selectedActionType != ActionType.LAUNCH_APP || selectedApp != null)

    // ---- App picker dialog ----
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                selectedApp = app
                selectedActionType = ActionType.LAUNCH_APP
                showAppPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Automation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Name ----
            OutlinedTextField(
                value = automationName,
                onValueChange = { automationName = it },
                label = { Text("Automation Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ============================================================
            // STEP 1: Set Morse Code Tap Sequence
            // ============================================================
            Text(
                text = "Step 1 — Morse Code Tap Sequence",
                style = MaterialTheme.typography.titleLarge
            )

            // Guard explanation card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sequence Guard: Sequences must always be in Morse code format using only '.' (dot / light tap) and '-' (hyphen / hard tap).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Preset options
            presets.forEachIndexed { index, preset ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = !isCustomMode && selectedPresetIndex == index,
                            onClick = {
                                isCustomMode = false
                                selectedPresetIndex = index
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isCustomMode && selectedPresetIndex == index,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(preset.label, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = preset.sequence,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Custom sequence option
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isCustomMode,
                        onClick = { isCustomMode = true },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isCustomMode, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Custom Morse Sequence", style = MaterialTheme.typography.bodyLarge)
            }

            // Custom Morse Builder (visible in custom mode)
            AnimatedVisibility(visible = isCustomMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isSequenceGuarded || customSequenceInput.isEmpty()) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Display sequence preview
                    Text(
                        text = "Sequence Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = if (customSequenceInput.isNotEmpty()) customSequenceInput else "(enter '.' or '-')",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.sp
                                ),
                                color = if (customSequenceInput.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }

                    // Guard validation message
                    if (customSequenceInput.isNotEmpty() && !isSequenceGuarded) {
                        Text(
                            text = "Guard violation: Sequence must contain ONLY '-' and '.'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (customSequenceInput.isEmpty()) {
                        Text(
                            text = "Add at least one '.' or '-' using buttons or keyboard below",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Morse Keypad Buttons: Dot, Dash, Backspace, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { customSequenceInput += "." },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("•  Dot (.)", style = MaterialTheme.typography.titleSmall)
                        }

                        Button(
                            onClick = { customSequenceInput += "-" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("—  Dash (-)", style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (customSequenceInput.isNotEmpty()) {
                                    customSequenceInput = customSequenceInput.dropLast(1)
                                }
                            },
                            enabled = customSequenceInput.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Backspace, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backspace")
                        }

                        OutlinedButton(
                            onClick = { customSequenceInput = "" },
                            enabled = customSequenceInput.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }
                    }

                    // Strict Input TextField (filters non-Morse characters via regex guard)
                    OutlinedTextField(
                        value = customSequenceInput,
                        onValueChange = { input ->
                            // Guard filter: only allow '-' and '.'
                            customSequenceInput = input.filter { it == '.' || it == '-' }
                        },
                        label = { Text("Morse Input Filter") },
                        placeholder = { Text("e.g. .. or .- or --") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        isError = customSequenceInput.isNotEmpty() && !isSequenceGuarded,
                        supportingText = {
                            Text("Guard: Only '-' and '.' characters are accepted")
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Hardware Tap Recorder
                    if (isRecording) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the back of your phone… (normal tap = '.', hard tap = '-')",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        OutlinedButton(
                            onClick = {
                                isRecording = true
                                customSequenceInput = ""
                                recorder.start()
                                recorder.startRecording()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record Live Taps via Hardware")
                        }
                    }
                }
            }

            HorizontalDivider()

            // ============================================================
            // STEP 2: Choose Action
            // ============================================================
            Text(
                text = "Step 2 — Choose Action",
                style = MaterialTheme.typography.titleLarge
            )

            // Quick utilities
            Text(
                text = "Quick Utilities",
                style = MaterialTheme.typography.titleMedium
            )
            quickActions.forEach { action ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedActionType == action.type &&
                                       action.type != ActionType.LAUNCH_APP,
                            onClick = {
                                selectedActionType = action.type
                                selectedApp = null
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedActionType == action.type &&
                                   action.type != ActionType.LAUNCH_APP,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    action.icon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(action.label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Launch App
            Text(
                text = "Launch App",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(
                onClick = { showAppPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Apps, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedApp?.let { "Selected: ${it.label}" }
                        ?: "Choose an app…"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Save button (strictly guarded) ----
            Button(
                onClick = {
                    val validatedSequence = AutomationRule.validateSequenceGuard(activeSequence)
                    val rule = AutomationRule(
                        name = automationName.trim(),
                        sequence = validatedSequence,
                        actionType = selectedActionType!!,
                        targetPackage = selectedApp?.packageName,
                        targetAppName = selectedApp?.label,
                        enabled = true
                    )
                    viewModel.addRule(rule)
                    onNavigateBack()
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save Automation")
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

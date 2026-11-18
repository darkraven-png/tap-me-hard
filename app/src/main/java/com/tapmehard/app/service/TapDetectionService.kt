package com.tapmehard.app.service

import android.app.*
import android.content.*
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.tapmehard.app.R
import com.tapmehard.app.TapMeHardApp
import com.tapmehard.app.data.ActionType
import com.tapmehard.app.data.SettingsDataStore
import com.tapmehard.app.util.ActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Persistent foreground service that hosts the [TapDetector].
 *
 * Registers a [BroadcastReceiver] for SCREEN_ON / SCREEN_OFF so the sensor
 * listener is active whenever the screen is on (including the lock screen)
 * and paused when the screen is completely off to conserve battery.
 */
class TapDetectionService : LifecycleService() {

    private lateinit var tapDetector: TapDetector
    private lateinit var actionExecutor: ActionExecutor

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> tapDetector.start()
                Intent.ACTION_SCREEN_OFF -> tapDetector.stop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val settingsStore = (application as TapMeHardApp).settingsStore
        actionExecutor = ActionExecutor(this)
        tapDetector = TapDetector(sensorManager, lifecycleScope)

        // Observe sensitivity changes
        lifecycleScope.launch {
            settingsStore.sensitivity.collect { tapDetector.sensitivity = it }
        }

        // Observe sequence delay changes
        lifecycleScope.launch {
            settingsStore.sequenceDelay.collect { tapDetector.sequenceDelay = it }
        }

        // Handle detected tap sequences
        lifecycleScope.launch {
            tapDetector.tapEvents.collect { event ->
                handleTapEvent(event, settingsStore)
            }
        }

        // Register screen on/off receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        // Create notification channel & promote to foreground
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Start sensor listening (screen is on when user enables service)
        tapDetector.start()
    }

    private suspend fun handleTapEvent(
        event: TapEvent,
        settingsStore: SettingsDataStore
    ) {
        val rules = settingsStore.automationRules.first()
        // Primary: Match exact Morse sequence (e.g. ".." or "--" or ".-")
        val match = rules.find { it.enabled && it.sequence == event.morseSequence }
            ?: rules.find {
                // Secondary: Match if sequence is all dots matching count
                it.enabled && it.sequence == ".".repeat(event.tapCount)
            }
            ?: rules.find {
                // Tertiary fallback: match by tap count
                it.enabled && it.tapCount == event.tapCount
            }

        val tapTypeDescription = formatTapType(event.morseSequence)

        if (match != null) {
            val message = when (match.actionType) {
                ActionType.LAUNCH_APP -> {
                    val appName = match.targetAppName ?: match.targetPackage ?: "selected"
                    "$tapTypeDescription tap detected, opening $appName app"
                }
                ActionType.TOGGLE_FLASHLIGHT -> {
                    "$tapTypeDescription tap detected, toggling flashlight"
                }
                ActionType.TAKE_SCREENSHOT -> {
                    "$tapTypeDescription tap detected, taking screenshot"
                }
                ActionType.PLAY_PAUSE_MEDIA -> {
                    "$tapTypeDescription tap detected, toggling media playback"
                }
            }

            Log.i(TAG, message)
            _lastTapMessage.value = TapMessageInfo(message = message, isSuccess = true)
            showMessage(message)
            actionExecutor.execute(match)
        } else {
            val wrongMessage = "Wrong tap sequence [$tapTypeDescription: ${event.morseSequence}] detected. No automation configured."
            Log.w(TAG, wrongMessage)
            _lastTapMessage.value = TapMessageInfo(message = wrongMessage, isSuccess = false)
            showMessage(wrongMessage)
            actionExecutor.vibrateError()
        }
    }

    private fun formatTapType(sequence: String): String {
        return when (sequence) {
            ".." -> "Double Dot"
            "--" -> "Double Dash"
            "..." -> "Triple Dot"
            "---" -> "Triple Dash"
            ".-" -> "Dot-Dash"
            "-." -> "Dash-Dot"
            "." -> "Single Dot"
            "-" -> "Single Dash"
            else -> {
                if (sequence.isEmpty()) "Unknown"
                else "Morse [$sequence]"
            }
        }
    }

    private suspend fun showMessage(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Notification ----

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps tap detection running in the background"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    // ---- Lifecycle ----

    override fun onDestroy() {
        tapDetector.stop()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) { }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        const val TAG = "TapMeHard"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "tap_detection"

        private val _lastTapMessage = MutableStateFlow<TapMessageInfo?>(null)
        val lastTapMessage: StateFlow<TapMessageInfo?> = _lastTapMessage.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, TapDetectionService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TapDetectionService::class.java))
        }
    }
}

data class TapMessageInfo(
    val message: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

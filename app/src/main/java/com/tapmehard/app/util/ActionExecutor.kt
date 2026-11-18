package com.tapmehard.app.util

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import com.tapmehard.app.data.ActionType
import com.tapmehard.app.data.AutomationRule
import com.tapmehard.app.service.TapAccessibilityService

class ActionExecutor(private val context: Context) {

    private var flashlightOn = false

    fun execute(rule: AutomationRule) {
        when (rule.actionType) {
            ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight()
            ActionType.PLAY_PAUSE_MEDIA -> playPauseMedia()
            ActionType.TAKE_SCREENSHOT -> takeScreenshot()
            ActionType.LAUNCH_APP -> launchApp(rule.targetPackage)
        }
    }

    // ---- Flashlight ----

    private fun toggleFlashlight() {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            flashlightOn = !flashlightOn
            cm.setTorchMode(cameraId, flashlightOn)
            vibrateFeedback()
        } catch (_: CameraAccessException) {
            // Camera in use or unavailable
        }
    }

    // ---- Media Play/Pause ----

    private fun playPauseMedia() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        am.dispatchMediaKeyEvent(downEvent)
        am.dispatchMediaKeyEvent(upEvent)
        vibrateFeedback()
    }

    // ---- Screenshot ----

    private fun takeScreenshot() {
        val service = TapAccessibilityService.instance
        if (service != null) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        }
    }

    // ---- Launch App ----

    private fun launchApp(packageName: String?) {
        packageName ?: return
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (keyguard.isKeyguardLocked) {
            // Launching an intent with FLAG_ACTIVITY_NEW_TASK automatically prompts
            // the system lock screen to unlock before opening the target app.
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName ?: return)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                } ?: return

            try {
                context.startActivity(launchIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        else {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ?: return
            context.startActivity(launchIntent)
        }
    }

    // ---- Haptic Feedback ----

    fun vibrateFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun vibrateError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(longArrayOf(0, 80, 80, 80), -1)
        }
    }
}

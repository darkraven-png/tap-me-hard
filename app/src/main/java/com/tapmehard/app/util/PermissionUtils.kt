package com.tapmehard.app.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.tapmehard.app.service.TapAccessibilityService

object PermissionUtils {

    /** Check if TapAccessibilityService is currently enabled in system settings. */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myComponent = ComponentName(context, TapAccessibilityService::class.java)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.let { si ->
                ComponentName(si.packageName, si.name) == myComponent
            }
        }
    }

    /** Check if the app can draw overlays (SYSTEM_ALERT_WINDOW). */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /** Check if battery optimizations are disabled (unrestricted). */
    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Open the Accessibility settings screen. */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Open the Overlay permission settings for this app. */
    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Open the Battery Optimization settings for this app. */
    fun openBatterySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

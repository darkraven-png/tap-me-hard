package com.tapmehard.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service registered with the system.
 *
 * Required for performing privileged global actions such as
 * [GLOBAL_ACTION_TAKE_SCREENSHOT]. The service itself does not monitor
 * accessibility events — it only exposes a static [instance] reference
 * so other components can call [performGlobalAction].
 */
class TapAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        /** Non-null while the service is connected. */
        @Volatile
        var instance: TapAccessibilityService? = null
            private set
    }
}

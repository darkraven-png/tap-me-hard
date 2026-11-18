package com.tapmehard.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tapmehard_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val SEQUENCE_DELAY = longPreferencesKey("sequence_delay")
        val AUTOMATION_RULES = stringPreferencesKey("automation_rules")
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    }

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        /**
         * Two default pre-configured sequences adhering strictly to the Morse code guard.
         */
        val DEFAULT_RULES = listOf(
            AutomationRule(
                id = "default-rule-flashlight",
                name = "Double Tap (Flashlight)",
                sequence = "..",
                actionType = ActionType.TOGGLE_FLASHLIGHT,
                enabled = true
            ),
            AutomationRule(
                id = "default-rule-screenshot",
                name = "Double Dash (Screenshot)",
                sequence = "--",
                actionType = ActionType.TAKE_SCREENSHOT,
                enabled = true
            )
        )
    }

    // --- Sensitivity (Z-axis threshold) ---
    val sensitivity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.SENSITIVITY] ?: 15f
    }

    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = value }
    }

    // --- Sequence wait delay ---
    val sequenceDelay: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.SEQUENCE_DELAY] ?: 500L
    }

    suspend fun setSequenceDelay(value: Long) {
        context.dataStore.edit { it[Keys.SEQUENCE_DELAY] = value }
    }

    // --- Service enabled toggle ---
    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SERVICE_ENABLED] ?: false
    }

    suspend fun setServiceEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.SERVICE_ENABLED] = value }
    }

    // --- Automation Rules (JSON-serialized list with guard validation & 2 default sequences) ---
    val automationRules: Flow<List<AutomationRule>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.AUTOMATION_RULES]
        if (raw == null) {
            DEFAULT_RULES
        } else {
            try {
                val parsed = json.decodeFromString<List<AutomationRule>>(raw)
                if (parsed.isEmpty()) {
                    DEFAULT_RULES
                } else {
                    // Guard sanitization: ensure every restored rule adheres to the Morse format
                    parsed.map { rule ->
                        if (AutomationRule.isValidSequence(rule.sequence)) rule
                        else rule.copy(sequence = "..")
                    }
                }
            } catch (_: Exception) {
                DEFAULT_RULES
            }
        }
    }

    suspend fun saveAutomationRules(rules: List<AutomationRule>) {
        // Enforce guard on all saved rules
        rules.forEach { AutomationRule.validateSequenceGuard(it.sequence) }
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTOMATION_RULES] = json.encodeToString(rules)
        }
    }

    suspend fun addRule(rule: AutomationRule) {
        // Guard check before adding
        AutomationRule.validateSequenceGuard(rule.sequence)

        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.AUTOMATION_RULES]
            val current = if (raw == null) {
                DEFAULT_RULES.toMutableList()
            } else {
                try {
                    json.decodeFromString<List<AutomationRule>>(raw).toMutableList()
                } catch (_: Exception) {
                    DEFAULT_RULES.toMutableList()
                }
            }
            current.add(rule)
            prefs[Keys.AUTOMATION_RULES] = json.encodeToString(current)
        }
    }

    suspend fun updateRule(updated: AutomationRule) {
        // Guard check before updating
        AutomationRule.validateSequenceGuard(updated.sequence)

        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.AUTOMATION_RULES]
            val current = if (raw == null) {
                DEFAULT_RULES.toMutableList()
            } else {
                try {
                    json.decodeFromString<List<AutomationRule>>(raw).toMutableList()
                } catch (_: Exception) {
                    DEFAULT_RULES.toMutableList()
                }
            }
            val idx = current.indexOfFirst { it.id == updated.id }
            if (idx >= 0) current[idx] = updated
            prefs[Keys.AUTOMATION_RULES] = json.encodeToString(current)
        }
    }

    suspend fun deleteRule(ruleId: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.AUTOMATION_RULES]
            val current = if (raw == null) {
                DEFAULT_RULES.toMutableList()
            } else {
                try {
                    json.decodeFromString<List<AutomationRule>>(raw).toMutableList()
                } catch (_: Exception) {
                    DEFAULT_RULES.toMutableList()
                }
            }
            current.removeAll { it.id == ruleId }
            prefs[Keys.AUTOMATION_RULES] = json.encodeToString(current)
        }
    }
}

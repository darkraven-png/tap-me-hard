package com.tapmehard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tapmehard.app.TapMeHardApp
import com.tapmehard.app.data.AutomationRule
import com.tapmehard.app.service.TapDetectionService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = (application as TapMeHardApp).settingsStore

    val sensitivity: StateFlow<Float> = store.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15f)

    val sequenceDelay: StateFlow<Long> = store.sequenceDelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500L)

    val serviceEnabled: StateFlow<Boolean> = store.serviceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val automationRules: StateFlow<List<AutomationRule>> = store.automationRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSensitivity(value: Float) {
        viewModelScope.launch { store.setSensitivity(value) }
    }

    fun setSequenceDelay(value: Long) {
        viewModelScope.launch { store.setSequenceDelay(value) }
    }

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            store.setServiceEnabled(enabled)
            val ctx = getApplication<TapMeHardApp>()
            if (enabled) {
                TapDetectionService.start(ctx)
            } else {
                TapDetectionService.stop(ctx)
            }
        }
    }

    fun addRule(rule: AutomationRule) {
        viewModelScope.launch { store.addRule(rule) }
    }

    fun toggleRule(rule: AutomationRule, enabled: Boolean) {
        viewModelScope.launch { store.updateRule(rule.copy(enabled = enabled)) }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch { store.deleteRule(ruleId) }
    }
}

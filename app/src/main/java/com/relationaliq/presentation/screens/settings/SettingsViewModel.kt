package com.relationaliq.presentation.screens.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val isHighContrast: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val stimuliStyle: String = "text"
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
    }

    fun setHighContrast(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isHighContrast = enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }

    fun setHapticEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(hapticEnabled = enabled)
    }

    fun setReducedMotion(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reducedMotion = enabled)
    }
}

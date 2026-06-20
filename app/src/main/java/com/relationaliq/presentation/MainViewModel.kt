package com.relationaliq.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val hasCompletedPreAssessment: Boolean = false,
    val isDarkMode: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeProfile().collect { profile ->
                _uiState.value = MainUiState(
                    isLoading = false,
                    hasCompletedOnboarding = profile?.hasCompletedOnboarding == true,
                    hasCompletedPreAssessment = profile?.hasCompletedPreAssessment == true,
                    isDarkMode = true
                )
            }
        }
    }
}

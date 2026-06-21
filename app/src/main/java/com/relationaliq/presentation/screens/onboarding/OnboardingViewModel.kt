package com.relationaliq.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relationaliq.domain.model.UserProfile
import com.relationaliq.domain.repository.ProgressRepository
import com.relationaliq.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            val userId = if (profile == null) {
                userRepository.createProfile(UserProfile())
            } else {
                profile.id
            }
            userRepository.markOnboardingComplete(userId)
            progressRepository.initializeAchievements()
            progressRepository.unlockStage(1)
        }
    }
}

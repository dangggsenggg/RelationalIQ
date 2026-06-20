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
            var profile = userRepository.getProfile()
            if (profile == null) {
                val id = userRepository.createProfile(UserProfile())
                profile = userRepository.getProfile()
            }
            profile?.let {
                userRepository.markOnboardingComplete(it.id)
            }
            progressRepository.initializeAchievements()
            progressRepository.unlockStage(1)
        }
    }
}

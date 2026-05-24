package com.example.diphone.core.data.system

import android.content.Context
import android.content.SharedPreferences

class OnboardingManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_FIRST_INSTALL = "first_install"
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun isFirstInstall(): Boolean {
        return prefs.getBoolean(KEY_FIRST_INSTALL, true)
    }

    fun completeOnboarding() {
        prefs.edit().apply {
            putBoolean(KEY_ONBOARDING_COMPLETE, true)
            putBoolean(KEY_FIRST_INSTALL, false)
            apply()
        }
    }

    fun resetOnboarding() {
        prefs.edit().apply {
            putBoolean(KEY_ONBOARDING_COMPLETE, false)
            putBoolean(KEY_FIRST_INSTALL, true)
            apply()
        }
    }
}

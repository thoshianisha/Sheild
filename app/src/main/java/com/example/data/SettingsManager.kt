package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scamshield_settings")

data class UserSettings(
    val isDarkMode: Boolean = true,
    val alertSoundEnabled: Boolean = true,
    val voiceFeedbackEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val saveScanHistory: Boolean = true,
    val demoModeDashboardEnabled: Boolean = false,
    val cloudAiAnalysisEnabled: Boolean = true,
    val userName: String = "Cyber Guardian",
    val userRole: String = "Personal Protection Tier"
)

class SettingsManager(private val context: Context) {
    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_ALERT_SOUND = booleanPreferencesKey("alert_sound")
        private val KEY_VOICE_FEEDBACK = booleanPreferencesKey("voice_feedback")
        private val KEY_VIBRATION = booleanPreferencesKey("vibration")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_SAVE_HISTORY = booleanPreferencesKey("save_history")
        private val KEY_DEMO_DASHBOARD = booleanPreferencesKey("demo_dashboard")
        private val KEY_CLOUD_AI = booleanPreferencesKey("cloud_ai")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            isDarkMode = prefs[KEY_DARK_MODE] ?: true,
            alertSoundEnabled = prefs[KEY_ALERT_SOUND] ?: true,
            voiceFeedbackEnabled = prefs[KEY_VOICE_FEEDBACK] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            saveScanHistory = prefs[KEY_SAVE_HISTORY] ?: true,
            demoModeDashboardEnabled = prefs[KEY_DEMO_DASHBOARD] ?: false,
            cloudAiAnalysisEnabled = prefs[KEY_CLOUD_AI] ?: true,
            userName = prefs[KEY_USER_NAME] ?: "Cyber Guardian",
            userRole = prefs[KEY_USER_ROLE] ?: "Personal Protection Tier"
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setAlertSound(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALERT_SOUND] = enabled }
    }

    suspend fun setVoiceFeedback(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VOICE_FEEDBACK] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setSaveScanHistory(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SAVE_HISTORY] = enabled }
    }

    suspend fun setDemoModeDashboard(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEMO_DASHBOARD] = enabled }
    }

    suspend fun setCloudAiAnalysis(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CLOUD_AI] = enabled }
    }

    suspend fun updateProfile(name: String, role: String) {
        context.dataStore.edit {
            it[KEY_USER_NAME] = name.ifBlank { "Cyber Guardian" }
            it[KEY_USER_ROLE] = role.ifBlank { "Personal Protection Tier" }
        }
    }
}

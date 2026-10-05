package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "morsecode_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_FOLLOW_SYSTEM = booleanPreferencesKey("follow_system")
        val KEY_ACCENT = stringPreferencesKey("accent_color")
        val KEY_CONFLICT_POLICY = stringPreferencesKey("conflict_policy")
        val KEY_SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        val KEY_BROADCAST_LIMIT = intPreferencesKey("broadcast_limit")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_THEME] ?: true }
    val followSystemTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_FOLLOW_SYSTEM] ?: false }
    val accentColorId: Flow<String> = context.dataStore.data.map { it[KEY_ACCENT] ?: "sunflower" }
    val conflictPolicy: Flow<String> = context.dataStore.data.map { it[KEY_CONFLICT_POLICY] ?: "Rename duplicates" }
    val soundEffects: Flow<Boolean> = context.dataStore.data.map { it[KEY_SOUND_EFFECTS] ?: true }
    val notifications: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }
    val deviceName: Flow<String> = context.dataStore.data.map { it[KEY_DEVICE_NAME] ?: "MYA-L10" }
    val broadcastLimit: Flow<Int> = context.dataStore.data.map { it[KEY_BROADCAST_LIMIT] ?: 4 }
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setFollowSystemTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FOLLOW_SYSTEM] = enabled }
    }

    suspend fun setAccentColor(accentId: String) {
        context.dataStore.edit { it[KEY_ACCENT] = accentId }
    }

    suspend fun setConflictPolicy(policy: String) {
        context.dataStore.edit { it[KEY_CONFLICT_POLICY] = policy }
    }

    suspend fun setSoundEffects(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_EFFECTS] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { it[KEY_DEVICE_NAME] = name }
    }

    suspend fun setBroadcastLimit(limit: Int) {
        context.dataStore.edit { it[KEY_BROADCAST_LIMIT] = limit }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }
}

package com.juan.fittracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

data class ReminderSettings(val enabled: Boolean, val hour: Int, val minute: Int) {
    companion object {
        val Default = ReminderSettings(enabled = false, hour = 19, minute = 0)
    }
}

object UserPrefs {
    private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    private val KEY_AGE = intPreferencesKey("age")
    private val KEY_SEX = stringPreferencesKey("sex")
    private val KEY_FREQ = stringPreferencesKey("frequency")
    private val KEY_HEIGHT = intPreferencesKey("height_cm")
    private val KEY_WEIGHT = intPreferencesKey("weight_kg")
    private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
    private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_TOUR_SEEN = booleanPreferencesKey("tour_seen")

    fun observeState(context: Context): Flow<OnboardingState> =
        context.dataStore.data.map { prefs ->
            if (prefs[KEY_ONBOARDED] != true) {
                OnboardingState.NotOnboarded
            } else {
                OnboardingState.Onboarded(
                    UserProfile(
                        age = prefs[KEY_AGE] ?: UserProfile.Default.age,
                        sex = enumOrDefault(prefs[KEY_SEX], Sex.Unspecified),
                        frequency = enumOrDefault(prefs[KEY_FREQ], TrainingFrequency.Regular),
                        heightCm = prefs[KEY_HEIGHT] ?: UserProfile.Default.heightCm,
                        weightKg = prefs[KEY_WEIGHT] ?: UserProfile.Default.weightKg,
                    ),
                )
            }
        }

    suspend fun save(context: Context, profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDED] = true
            prefs[KEY_AGE] = profile.age
            prefs[KEY_SEX] = profile.sex.name
            prefs[KEY_FREQ] = profile.frequency.name
            prefs[KEY_HEIGHT] = profile.heightCm
            prefs[KEY_WEIGHT] = profile.weightKg
        }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
    }

    fun observeReminder(context: Context): Flow<ReminderSettings> =
        context.dataStore.data.map { prefs ->
            ReminderSettings(
                enabled = prefs[KEY_REMINDER_ENABLED] ?: ReminderSettings.Default.enabled,
                hour = prefs[KEY_REMINDER_HOUR] ?: ReminderSettings.Default.hour,
                minute = prefs[KEY_REMINDER_MINUTE] ?: ReminderSettings.Default.minute,
            )
        }

    suspend fun saveReminder(context: Context, settings: ReminderSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_ENABLED] = settings.enabled
            prefs[KEY_REMINDER_HOUR] = settings.hour
            prefs[KEY_REMINDER_MINUTE] = settings.minute
        }
    }

    fun observeThemeMode(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_THEME_MODE] }

    suspend fun saveThemeMode(context: Context, modeName: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = modeName }
    }

    fun observeTourSeen(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_TOUR_SEEN] ?: false }

    suspend fun setTourSeen(context: Context, seen: Boolean) {
        context.dataStore.edit { it[KEY_TOUR_SEEN] = seen }
    }

    suspend fun resetEverything(context: Context) {
        context.dataStore.edit { it.clear() }
        com.juan.fittracker.data.Db.get(context).clearAllTables()
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(value: String?, default: E): E =
        value?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
}

sealed class OnboardingState {
    object Loading : OnboardingState()
    object NotOnboarded : OnboardingState()
    data class Onboarded(val profile: UserProfile) : OnboardingState()
}

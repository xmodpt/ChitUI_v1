package com.chitui.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chitui_prefs")

class PreferencesManager(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val USERNAME = stringPreferencesKey("username")
        private val REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val NOTIFY_PRINT_COMPLETE = booleanPreferencesKey("notify_print_complete")
        private val NOTIFY_ERRORS = booleanPreferencesKey("notify_errors")
    }

    // Server URL
    suspend fun saveServerUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[SERVER_URL] = url
        }
    }

    fun getServerUrl(): Flow<String?> = dataStore.data.map { prefs ->
        prefs[SERVER_URL]
    }

    // Credentials
    suspend fun saveCredentials(username: String, rememberMe: Boolean) {
        dataStore.edit { prefs ->
            prefs[USERNAME] = username
            prefs[REMEMBER_ME] = rememberMe
        }
    }

    fun getUsername(): Flow<String?> = dataStore.data.map { prefs ->
        prefs[USERNAME]
    }

    fun getRememberMe(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[REMEMBER_ME] ?: false
    }

    // Login state
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = isLoggedIn
        }
    }

    fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN] ?: false
    }

    // Notifications
    suspend fun setNotifyPrintComplete(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[NOTIFY_PRINT_COMPLETE] = enabled
        }
    }

    fun getNotifyPrintComplete(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFY_PRINT_COMPLETE] ?: true
    }

    suspend fun setNotifyErrors(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[NOTIFY_ERRORS] = enabled
        }
    }

    fun getNotifyErrors(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFY_ERRORS] ?: true
    }

    // Clear all data
    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

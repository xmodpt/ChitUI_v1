package com.chitui.client.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chitui_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val USERNAME = stringPreferencesKey("username")
        private val PASSWORD = stringPreferencesKey("password")
        private val REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val SESSION_COOKIE = stringPreferencesKey("session_cookie")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME] }
    val password: Flow<String?> = context.dataStore.data.map { it[PASSWORD] }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[REMEMBER_ME] ?: false }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val sessionCookie: Flow<String?> = context.dataStore.data.map { it[SESSION_COOKIE] }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun saveCredentials(username: String, password: String, remember: Boolean) {
        context.dataStore.edit {
            it[USERNAME] = username
            if (remember) {
                it[PASSWORD] = password
            } else {
                it.remove(PASSWORD)
            }
            it[REMEMBER_ME] = remember
        }
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { it[IS_LOGGED_IN] = loggedIn }
    }

    suspend fun saveSessionCookie(cookie: String) {
        context.dataStore.edit { it[SESSION_COOKIE] = cookie }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it[IS_LOGGED_IN] = false
            it.remove(SESSION_COOKIE)
            if (it[REMEMBER_ME] != true) {
                it.remove(PASSWORD)
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

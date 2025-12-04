package org.example.app

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "stream_prefs")

data class UserSettings(
    val streamUrl: String,
    val autoTranslate: Boolean,
    val translationLatencyMs: Int,
    val sidecarVttUrl: String?
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val streamUrl = stringPreferencesKey("stream_url")
        val srcLang = stringPreferencesKey("src_lang")
        val dstLang = stringPreferencesKey("dst_lang")
        val autoTranslate = booleanPreferencesKey("auto_translate")
        val translationLatency = intPreferencesKey("translation_latency_ms")
        val sidecarVttUrl = stringPreferencesKey("sidecar_vtt_url")
    }

    suspend fun getSettings(): UserSettings {
        val prefs = context.dataStore.data.first()
        return prefs.toSettings()
    }

    fun getSettingsSync(): UserSettings = runBlocking {
        getSettings()
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.streamUrl] = settings.streamUrl
            prefs[Keys.autoTranslate] = settings.autoTranslate
            prefs[Keys.translationLatency] = settings.translationLatencyMs
            if (settings.sidecarVttUrl != null) {
                prefs[Keys.sidecarVttUrl] = settings.sidecarVttUrl
            } else {
                prefs.remove(Keys.sidecarVttUrl)
            }
        }
    }

    suspend fun saveLanguages(src: String, dst: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.srcLang] = src
            prefs[Keys.dstLang] = dst
        }
    }

    suspend fun setAutoTranslate(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.autoTranslate] = enabled
        }
    }

    private fun Preferences.toSettings(): UserSettings {
        return UserSettings(
            streamUrl = this[Keys.streamUrl] ?: AppConstants.DEFAULT_HLS_URL,
            autoTranslate = this[Keys.autoTranslate] ?: true,
            translationLatencyMs = this[Keys.translationLatency] ?: 150,
            sidecarVttUrl = this[Keys.sidecarVttUrl],
        )
    }
}

package org.example.app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.text.Cue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing player state, subtitles, translation, and preferences.
 */
class PlayerViewModel : ViewModel() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val repoRef = MutableStateFlow<PreferencesRepository?>(null)
    private val translationManager = TranslationManager()
    private val subtitleManager = SubtitleManager(translationManager)

    private val _uiState = MutableStateFlow(
        UiState(
            srcLang = "en",
            dstLang = "es",
            autoTranslate = true,
            ccEnabled = true,
            showBothSubtitles = false
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    val playerEventsListener = object : Player.Listener {
        override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Playback error: ${error.errorCodeName}. Retrying..."
            )
        }
    }

    fun onCues(cues: List<Cue>) {
        if (!_uiState.value.ccEnabled) return
        val merged = cues.joinToString("\n") { it.text?.toString().orEmpty() }.trim()
        if (merged.isEmpty()) return

        // Process through subtitle manager (debounce identical)
        scope.launch {
            val state = _uiState.value
            subtitleManager.processCue(
                originalText = merged,
                srcLang = state.srcLang,
                dstLang = state.dstLang,
                autoTranslate = state.autoTranslate
            ) { original, translated ->
                _uiState.value = _uiState.value.copy(
                    currentOriginalText = original,
                    currentTranslatedText = translated ?: ""
                )
            }
        }
    }

    fun updateSrcLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(srcLang = lang)
        saveLanguages()
    }

    fun updateDstLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(dstLang = lang)
        saveLanguages()
    }

    fun setAutoTranslate(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoTranslate = enabled)
        scope.launch { repoRef.value?.setAutoTranslate(enabled) }
    }

    fun setCcEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(ccEnabled = enabled)
    }

    fun setShowBothSubtitles(showBoth: Boolean) {
        _uiState.value = _uiState.value.copy(showBothSubtitles = showBoth)
    }

    fun resolveStreamUrl(context: Context): String {
        val repo = repoRef.value ?: PreferencesRepository(context).also { rp ->
            repoRef.value = rp
        }
        val settings = repo.getSettingsSync()
        // update toggles
        _uiState.value = _uiState.value.copy(
            autoTranslate = settings.autoTranslate
        )
        return settings.streamUrl.ifBlank { AppConstants.DEFAULT_HLS_URL }
    }

    fun resolveSidecarVtt(context: Context): Uri? {
        val repo = repoRef.value ?: PreferencesRepository(context).also { rp ->
            repoRef.value = rp
        }
        val settings = repo.getSettingsSync()
        return settings.sidecarVttUrl?.let { Uri.parse(it) }
    }

    private fun saveLanguages() {
        scope.launch {
            repoRef.value?.saveLanguages(_uiState.value.srcLang, _uiState.value.dstLang)
        }
    }
}

data class UiState(
    val srcLang: String,
    val dstLang: String,
    val autoTranslate: Boolean,
    val ccEnabled: Boolean,
    val showBothSubtitles: Boolean,
    val currentOriginalText: String = "",
    val currentTranslatedText: String = "",
    val errorMessage: String? = null
)

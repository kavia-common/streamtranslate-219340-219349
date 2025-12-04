package org.example.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Observes cues and processes them with optional translation.
 */
class SubtitleManager(
    private val translationManager: TranslationManager
) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var lastOriginal: String? = null

    fun processCue(
        originalText: String,
        srcLang: String,
        dstLang: String,
        autoTranslate: Boolean,
        onUpdate: (original: String, translated: String?) -> Unit
    ) {
        if (originalText == lastOriginal) {
            // Avoid reprocessing identical repeats
            return
        }
        lastOriginal = originalText

        if (!autoTranslate) {
            onUpdate(originalText, null)
            return
        }

        scope.launch {
            translationManager.translateAsync(
                text = originalText,
                src = srcLang,
                dst = dstLang
            ) { translated ->
                onUpdate(originalText, translated ?: originalText)
            }
        }
    }
}

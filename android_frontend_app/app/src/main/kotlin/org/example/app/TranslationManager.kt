package org.example.app

import android.content.Context
import org.example.app.translation.Translator
import org.example.app.translation.TranslationStub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Manages translation requests so UI isn't blocked. Can inject a mock translator for testing.
 */
class TranslationManager(
    private var translator: Translator = TranslationStub({ currentLatencyMs.value }, { "[Translated] " })
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val requestChannel = Channel<TranslationRequest>(capacity = Channel.UNLIMITED)

    companion object {
        val currentLatencyMs = MutableStateFlow(150)
    }

    init {
        scope.launch {
            for (request in requestChannel) {
                try {
                    val result = translator.translate(request.text, request.src, request.dst)
                    request.onResult(result)
                } catch (e: Exception) {
                    request.onResult(null)
                }
            }
        }
    }

    fun translateAsync(
        text: String,
        src: String,
        dst: String,
        onResult: (String?) -> Unit
    ) {
        scope.launch {
            requestChannel.send(TranslationRequest(text, src, dst, onResult))
        }
    }

    // PUBLIC_INTERFACE
    fun injectTranslator(fake: Translator) {
        translator = fake
    }

    fun updateLatency(latencyMs: Int) {
        currentLatencyMs.value = latencyMs
    }
}

data class TranslationRequest(
    val text: String,
    val src: String,
    val dst: String,
    val onResult: (String?) -> Unit
)

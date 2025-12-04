package org.example.app.translation

import kotlinx.coroutines.delay

/**
 * A simple on-device translation stub that simulates latency and returns a tagged translation.
 */
class TranslationStub(
    private val latencyMsProvider: () -> Int = { 150 },
    private val prefixProvider: () -> String = { "[Translated] " }
) : Translator {

    override suspend fun translate(text: String, srcLang: String, dstLang: String): String {
        val delayMs = latencyMsProvider().coerceAtLeast(0)
        if (delayMs > 0) delay(delayMs.toLong())
        return "${prefixProvider()}$text ($srcLang→$dstLang)"
    }
}

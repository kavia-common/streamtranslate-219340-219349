package org.example.app.translation

/**
 * PUBLIC_INTERFACE
 * Translator interface for on-device translation. Designed to be pluggable with real models.
 */
interface Translator {
    suspend fun translate(text: String, srcLang: String, dstLang: String): String
}

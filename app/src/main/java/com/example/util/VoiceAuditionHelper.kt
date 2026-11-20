package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceAuditionHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady: Boolean = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindiLocale = Locale("hi", "IN")
                val result = tts?.setLanguage(hindiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                isReady = true
            }
        }
    }

    fun speak(text: String): Boolean {
        if (!isReady || text.isBlank()) return false
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bharat_voice_audition")
        return true
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

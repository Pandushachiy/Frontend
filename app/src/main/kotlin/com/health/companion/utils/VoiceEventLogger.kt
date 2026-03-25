package com.health.companion.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VoiceEventLogger {
    private const val PREFS = "voice_events"
    private const val KEY = "events"
    private const val MAX_LINES = 200

    fun log(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY, "") ?: ""
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        val line = "${formatter.format(Date())}  $message"
        val lines = (current.split("\n").filter { it.isNotBlank() } + line).takeLast(MAX_LINES)
        prefs.edit().putString(KEY, lines.joinToString("\n")).commit()
    }

    fun read(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, "")?.ifBlank { "Нет событий" } ?: "Нет событий"
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY).apply()
    }

    fun diagnostics(context: Context): String {
        val pm = context.packageManager
        val recIntent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val handlers = pm.queryIntentActivities(recIntent, 0)
        val handlerList = if (handlers.isEmpty()) "none" else handlers.joinToString { it.activityInfo.packageName }
        val isAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
        return buildString {
            appendLine("SpeechRecognizer available: $isAvailable")
            appendLine("RecognizerIntent handlers: $handlerList")
        }
    }
}

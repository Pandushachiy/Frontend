package com.health.companion.utils

import android.app.Application
import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val FILE_NAME = "last_crash.txt"

    fun install(application: Application) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(application, thread, throwable)
            } catch (_: Exception) {
                // Best effort only
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun readCrash(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return "Нет данных о падениях"
        return file.readText()
    }

    fun clearCrash(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val header = "Time: ${formatter.format(Date())}\nThread: ${thread.name}\n\n"
        val sw = StringWriter()
        PrintWriter(sw).use { throwable.printStackTrace(it) }
        File(context.filesDir, FILE_NAME).writeText(header + sw.toString())
    }
}

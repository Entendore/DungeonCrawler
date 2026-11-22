package com.yourpackage.dungeoncrawler.telemetry

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class LocalCrashLog(context: Context) {
    private val appContext = context.applicationContext
    private val logDir: File = File(appContext.filesDir, "crashes").apply { mkdirs() }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val gameState = mutableMapOf<String, Any>()
    private var userId: String = context.getSharedPreferences("app", Context.MODE_PRIVATE).getString("user_id", null) ?: java.util.UUID.randomUUID().toString().also { context.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("user_id", it).apply() }
    
    fun install() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try { recordCrash(thread, throwable) } catch (e: Exception) {}
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
    
    fun setGameState(state: Map<String, Any>) { gameState.clear(); gameState.putAll(state) }
    fun log(message: String) { try { File(logDir, "log_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.txt").appendText("[${dateFormat.format(Date())}] $message\n") } catch (e: Exception) {} }
    
    fun recordNonFatal(throwable: Throwable) {
        try {
            val file = File(logDir, "log_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.txt")
            val sw = StringWriter(); throwable.printStackTrace(PrintWriter(sw))
            file.appendText("\n--- NON-FATAL ERROR [${dateFormat.format(Date())}] ---\n${throwable.javaClass.simpleName}: ${throwable.message}\n${sw.toString()}")
        } catch (e: Exception) {}
    }
    
    private fun recordCrash(thread: Thread, throwable: Throwable) {
        val file = File(logDir, "crash_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt")
        val sw = StringWriter(); throwable.printStackTrace(PrintWriter(sw))
        file.writeText(buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Time: ${dateFormat.format(Date())}")
            appendLine("Thread: ${thread.name}")
            appendLine("User ID: $userId")
            appendLine("App Version: ${getAppVersion()}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("\n=== GAME STATE ===")
            gameState.forEach { (k, v) -> appendLine("$k: $v") }
            appendLine("\n=== STACK TRACE ===")
            appendLine(sw.toString())
        })
    }
    
    private fun getAppVersion(): String = try { appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "unknown" } catch (e: Exception) { "unknown" }
}

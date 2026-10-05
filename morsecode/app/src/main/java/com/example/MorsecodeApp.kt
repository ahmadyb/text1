package com.example

import android.app.Application
import androidx.room.Room
import com.example.core.database.CrashReportEntity
import com.example.core.database.MorsecodeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MorsecodeApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: com.example.core.di.AppContainer
        private set

    val database: MorsecodeDatabase
        get() = container.database

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = com.example.core.di.DefaultAppContainer(this)
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dateFormat = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
                val report = CrashReportEntity(
                    id = UUID.randomUUID().toString(),
                    whenText = dateFormat.format(Date()),
                    component = throwable.stackTrace.firstOrNull()?.className?.substringAfterLast('.') ?: "Unknown",
                    error = throwable.javaClass.simpleName + ": " + (throwable.message ?: "No message"),
                    note = "Local report captured safely without remote telemetry."
                )
                applicationScope.launch {
                    database.crashReportDao().insertCrashReport(report)
                }
                Thread.sleep(200) // Brief grace period for persistence
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        lateinit var instance: MorsecodeApp
            private set
    }
}

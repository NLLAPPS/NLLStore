package com.nll.store.debug

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nll.store.R
import com.nll.store.log.CLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.LinkedList
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class DebugLogService : LifecycleService() {
    private val logStorage = LinkedList<String>()
    private val maxLogLines = 20000

    init {

        CLog.observableLog().onEach { message ->
            if (logStorage.size > maxLogLines) {
                logStorage.removeAt(0)
            }
            logStorage.add(message)
            _observableLogProxy.emit(message)

        }.launchIn(lifecycleScope)


        serviceCommand.onEach {
            when (it) {
                DebugLogServiceCommand.Clear -> clearLogStorage()
                DebugLogServiceCommand.Save -> save()
                DebugLogServiceCommand.Stop -> stop()
            }
        }.launchIn(lifecycleScope)


    }

    private fun sendServiceMessage(message: DebugLogServiceMessage) {
        lifecycleScope.launch {
            _serviceMessage.emit(message)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        CLog.enableDebug(this)
        val debugLogIntent = Intent(this, DebugLogActivity::class.java)
        debugLogIntent.action = "android.intent.action.MAIN"
        debugLogIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pi = PendingIntent.getActivity(this, 0, debugLogIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = DebugNotification.getDebugEnabledNotification(this, pi).build()
        startForeground(R.string.debug_log, notification)
        sendServiceMessage(DebugLogServiceMessage.Started(logStorage))

        /**
         * Android 14 seems to cause issues with START_STICKY
         * java.lang.RuntimeException: Unable to start service com.nll.store.debug.DebugLogService@4ddf6c8 with null: android.app.ForegroundServiceStartNotAllowedException: Service.startForeground() not allowed due to mAllowStartForeground false: service com.nll.store/.debug.DebugLogService
         *
         */
        return START_NOT_STICKY
    }


    companion object {
        private val _observableLogProxy = MutableSharedFlow<String>()
        fun observableLogProxy() = _observableLogProxy.asSharedFlow()

        //State Flow because we need to get last state when re opening activity from notification
        private val _serviceMessage = MutableStateFlow<DebugLogServiceMessage>(DebugLogServiceMessage.Stopped)
        fun serviceMessage() = _serviceMessage.asStateFlow()


        private val serviceCommand = MutableSharedFlow<DebugLogServiceCommand>()

        fun startLogging(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context.applicationContext, DebugLogService::class.java))
        }

        suspend fun stopLogging() {
            sendServiceCommand(DebugLogServiceCommand.Stop)
        }

        suspend fun clearLogs() {
            sendServiceCommand(DebugLogServiceCommand.Clear)
        }

        suspend fun saveLogs() {
            sendServiceCommand(DebugLogServiceCommand.Save)
        }

        private suspend fun sendServiceCommand(command: DebugLogServiceCommand) {
            serviceCommand.emit(command)
        }
    }


    private fun stop() {
        CLog.disableDebug()
        clearLogStorage()
        sendServiceMessage(DebugLogServiceMessage.Stopped)
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }


    private fun clearLogStorage() {
        logStorage.clear()
    }


    private fun save() {
        lifecycleScope.launch(Dispatchers.IO) {

            val paths = mutableListOf<String>()

            val debugLogDump = async { saveDebugLog() }
            val logcatgDump = async { saveLogCatDump() }

            debugLogDump.await()?.let { debugLogFile ->
                paths.add(debugLogFile.absolutePath)
            }

            logcatgDump.await()?.let { logcatFile ->
                paths.add(logcatFile.absolutePath)
            }

            val zipFile = zipFiles(paths)
            val saved = zipFile != null

            sendServiceMessage(DebugLogServiceMessage.Saved(saved, zipFile?.absolutePath))
            delay(500)
            stop()
        }
    }

    private fun zipFiles(files: List<String>): File? {
        return if (files.isEmpty()) {
            null
        } else {
            val zipFile = File(DebugLogAttachmentProvider.getLogPath(applicationContext).toString() + "/store_logs.zip")
            if (zipFile.exists()) {
                zipFile.delete()
            }
            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
                    for (file in files) {
                        FileInputStream(file).use { fi ->
                            BufferedInputStream(fi).use { origin ->
                                val entry = ZipEntry(File(file).name)
                                out.putNextEntry(entry)
                                origin.copyTo(out, 1024)
                                out.closeEntry()
                            }
                        }
                    }
                }
                zipFile
            } catch (e: Exception) {
                null
            }
        }

    }

    private fun saveDebugLog(): File? {
        val file = File(DebugLogAttachmentProvider.getLogPath(applicationContext).toString() + "/store_debugLog.txt")
        if (file.exists()) {
            file.delete()
        }
        return try {
            file.bufferedWriter().use { out ->
                logStorage.forEach { line ->
                    out.write(line)
                    out.newLine()
                }
            }
            file
        } catch (e: Exception) {
            null
        }

    }

    private fun saveLogCatDump(): File? {
        val file = File(DebugLogAttachmentProvider.getLogPath(applicationContext).toString() + "/store_logcat.txt")
        if (file.exists()) {
            file.delete()
        }
        return try {
            val args = arrayOf("logcat", "-v", "time", "-d")
            val process = Runtime.getRuntime().exec(args)
            val input = InputStreamReader(process.inputStream)

            file.bufferedWriter().use { out ->
                input.buffered().lines().forEach { line ->
                    out.write(line)
                    out.newLine()
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

}

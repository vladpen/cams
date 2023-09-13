package com.vladpen

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.vladpen.cams.R
import com.vladpen.cams.VideoActivity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConnectionDataModel(val session: Session, val channel: ChannelSftp)

class AlertWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notificationChannelId = "CamsNotificationChannelId"
    private val checkIntervalMillis = 4000
    private val connections = mutableMapOf<String, ConnectionDataModel>()

    companion object {
        private val lastTimes = mutableMapOf<Int, Int>()

        fun getLastTime(streamId: Int): String {
            if (!lastTimes.keys.contains(streamId) ||
                lastTimes[streamId]!! < (System.currentTimeMillis() / 1000).toInt() - 43200)
                return "" // show only last 12 hours (12 * 60 * 60)
            return SimpleDateFormat("hh:mm", Locale.getDefault()).format(
                Date(lastTimes[streamId]!!.toLong() * 1000)
            )
        }

        fun clearLastTimes() {
            lastTimes.clear()
        }
    }

    override suspend fun doWork(): Result {
        try {
            if (ActivityCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("AlertWork", "No permission")
                return Result.failure()
            }
            createNotificationChannel()
            while (true) {
                val streamId = checkNewEvent()
                NotificationManagerCompat.from(applicationContext).notify(
                    0, createNotification(streamId))
            }
        } catch (throwable: Throwable) {
            Log.e("AlertWork", "Loop interrupted (${throwable.localizedMessage})")
            return Result.failure()
        } finally {
            this.disconnectAll()
        }
    }

    private fun createNotification(streamId: Int) : Notification {
        val activityIntent = Intent(applicationContext, VideoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("streamId", streamId)
        }
        val activityPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_fullsize)
            .setContentText(StreamData.getById(streamId)?.name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) // Android 8 API 26 (O)
            return
        val notificationChannel = NotificationChannel(
            notificationChannelId,
            "CamsWorkChannel",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            getSystemService(applicationContext, NotificationManager::class.java)
        notificationManager?.createNotificationChannel(notificationChannel)
    }

    private fun connect(sftpData: UrlDataModel): ConnectionDataModel {
        val jsch = JSch()
        val password = Utils.decodeString(sftpData.password)

        val session = jsch.getSession(sftpData.user, sftpData.host, sftpData.port)
        session?.setPassword(password)
        session?.setConfig("StrictHostKeyChecking", "no")
        session?.connect(3000)

        val channel = session?.openChannel("sftp") as ChannelSftp
        channel.connect(3000)
        return ConnectionDataModel(session, channel)
    }

    private fun disconnectAll() {
        connections.forEach {
            it.value.channel.disconnect()
            it.value.session.disconnect()
        }
        connections.clear()
    }

    private suspend fun checkNewEvent(): Int {
        val streams = StreamData.getAll()
        var enable = false
        while (true) {
            for ((id, stream) in streams.withIndex()) {
                if (stream.alert != true || stream.sftp == "")
                    continue
                val sftpData = Utils.parseUrl(stream.sftp, 22, "sftp")
                    ?: continue

                enable = true
                val url = stream.sftp!!.dropLast(sftpData.path.length)
                if (!connections.keys.contains(url)) {
                    try {
                        connections[url] = this.connect(sftpData)
                    } catch (e: Exception) {
                        Log.e("Alert", "Can't connect ${sftpData.host} (${e.localizedMessage})")
                        continue
                    }
                }
                val path = if (sftpData.path == "") "/" else sftpData.path
                try {
                    val mTime = this.requestLastTime(connections[url]!!.channel, path)
                    if (!lastTimes.keys.contains(id))
                        lastTimes[id] = mTime
                    else if (mTime > lastTimes[id]!!) {
                        lastTimes[id] = mTime
                        return id
                    }
                } catch (e: Exception) {
                    connections.remove(url)
                    Log.e("AlertWork", "ls error (${e.localizedMessage})")
                }
            }
            if (!enable)
                throw Exception("All streams are disabled")
            if (connections.isEmpty())
                delay(10000)
            delay(checkIntervalMillis.toLong())
        }
    }

    private fun requestLastTime(channel: ChannelSftp, path: String): Int {
        val ls = channel.ls(path)
        var mTime = 0
        var folderName = ""
        ls.forEach {
            val e = it
            if (e.filename == "." || e.filename == "..")
                return@forEach
            if (mTime == 0 || e.attrs.mTime > mTime) {
                mTime = e.attrs.mTime
                folderName = if (e.attrs.isDir) e.filename else ""
            }
        }
        if (folderName != "")
            return this.requestLastTime(channel, "$path/$folderName")
        return mTime
    }
}

package com.taosc.taosc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ShareUploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "share_uploads"
    }

    private fun notifyStatus(message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Share uploads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status of shares sent to taOS"
            }
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentText(message)
            .setContentTitle("taOS share")
            .setAutoCancel(true)
            .setLocalOnly(true)
            .build()
        notificationManager.notify(id.hashCode(), notification)
    }

    override suspend fun doWork(): Result {
        val label = inputData.getString("label") ?: ""
        val cachePath = inputData.getString("cachePath") ?: ""
        notifyStatus("Uploading to $label")

        val token = EncryptedCredentialStore.create(applicationContext).readToken()
        if (token == null) {
            notifyStatus("Pair this phone again")
            if (cachePath.isNotEmpty()) {
                java.io.File(cachePath).delete()
            }
            return Result.failure()
        }

        val prefs = applicationContext.getSharedPreferences("taosc_settings", Context.MODE_PRIVATE)
        val settingsStore = SettingsStore(prefs)
        val baseUrl = settingsStore.serverUrl

        val attempt = runAttemptCount + 1

        val kindStr = inputData.getString("kind") ?: ""
        val destination = ShareDestination(
            kind = ShareDestinationKind.valueOf(kindStr),
            id = inputData.getString("id") ?: "",
            label = label,
            channelId = (inputData.getString("channelId") ?: "").ifEmpty { null }
        )

        val itemKind = inputData.getString("itemKind") ?: ""
        val item = when (itemKind) {
            "Text" -> ShareItem.Text(inputData.getString("text") ?: "")
            "Link" -> ShareItem.Link(
                url = inputData.getString("url") ?: "",
                title = inputData.getString("title")?.ifEmpty { null }
            )
            "File" -> ShareItem.File(
                uri = "",
                mimeType = inputData.getString("mimeType")?.ifEmpty { null }
            )
            else -> throw IllegalArgumentException("Unknown item kind: $itemKind")
        }

        val displayName = inputData.getString("displayName") ?: ""
        val mimeType = inputData.getString("mimeType")?.ifEmpty { null }

        val fileReader: (uri: String) -> SharedFile = { _ ->
            SharedFile(
                filename = displayName,
                mimeType = mimeType,
                bytes = java.io.File(cachePath).readBytes()
            )
        }

        val sender = ShareSender(DefaultHttpClient()) { java.util.UUID.randomUUID().toString() }
        val result = sender.send(baseUrl, token, destination, item, fileReader)

        return when (val decision = decide(result, attempt)) {
            is RetryDecision.Done -> {
                if (cachePath.isNotEmpty()) {
                    java.io.File(cachePath).delete()
                }
                notifyStatus("Sent to ${destination.label}")
                Result.success()
            }
            is RetryDecision.GiveUp -> {
                if (cachePath.isNotEmpty()) {
                    java.io.File(cachePath).delete()
                }
                notifyStatus(decision.reason)
                Result.failure()
            }
            is RetryDecision.RetryLater -> Result.retry()
        }
    }
}

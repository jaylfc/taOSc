package com.taosc.taosc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShareUploadWorker(
    workerParameters: androidx.work.WorkerParameters
) : CoroutineWorker(workerParameters) {

    private val TAG = "ShareUploadWorker"
    private val SHARE_UPLOADS_CHANNEL = "share_uploads"
    private val UPLOAD_NOTIFICATION_ID = 10001
    private val SENT_NOTIFICATION_ID = 10002

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SHARE_UPLOADS_CHANNEL,
                "Share Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background share upload notifications"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun doWork(): Result {
        val input = inputData
        val label = input.getString("label") ?: "share"
        val destKind = input.getString("kind") ?: "library"
        val destId = input.getString("id") ?: ""
        val channelId = input.getString("channelId") ?: ""
        val itemKind = input.getString("itemKind") ?: "text"
        val itemText = input.getString("itemText") ?: ""
        val itemUrl = input.getString("itemUrl") ?: ""
        val itemTitle = input.getString("itemTitle") ?: ""
        val cachedFilePath = input.getString("cachedFilePath") ?: ""
        val attemptStr = input.getString("attempt") ?: "1"
        val currentAttempt = attemptStr.toInt()

        val destination = when (destKind) {
            "library" -> ShareDestination(ShareDestinationKind.LIBRARY, destId, label)
            "projectFiles" -> ShareDestination(ShareDestinationKind.PROJECT_FILES, destId, label)
            "agentChat" -> ShareDestination(ShareDestinationKind.AGENT_CHAT, destId, label, channelId)
            else -> throw IllegalArgumentException("Unknown destination kind: $destKind")
        }

        val item = when (itemKind) {
            "text" -> ShareItem.Text(itemText)
            "link" -> ShareItem.Link(itemUrl, itemTitle)
            "file" -> {
                val file = java.io.File(cachedFilePath)
                if (file.exists()) {
                    ShareItem.File(file.absolutePath, null)
                } else {
                    ShareItem.File(cachedFilePath, null)
                }
            }
            else -> throw IllegalArgumentException("Unknown item kind: $itemKind")
        }

        val httpClient = DefaultHttpClient()
        val shareSender = ShareSender(httpClient) { cacheUri ->
            val file = java.io.File(cacheUri)
            SharedFile(
                filename = file.name,
                mimeType = null,
                bytes = file.readBytes()
            )
        }

        val result = shareSender.send(
            "https://example.com",
            "token",
            destination,
            item,
            ::readFromCache
        )

        val decision = decide(result, currentAttempt)

        return when (decision) {
            is RetryDecision.Done -> {
                java.io.File(cachedFilePath).delete()
                sendFinishedNotification(label, "Sent to $label")
                Result.success()
            }
            is RetryDecision.GiveUp -> {
                java.io.File(cachedFilePath).delete()
                val reason = decision.reason
                sendFinishedNotification(label, when (reason) {
                    "NotPaired" -> "Pair this phone again"
                    "Rejected" -> "taOS refused this destination"
                    else -> reason
                })
                Result.failure()
            }
            is RetryDecision.RetryLater -> {
                sendUploadingNotification(label)
                Result.retry()
            }
        }
    }

    private fun readFromCache(uri: String): SharedFile {
        val file = java.io.File(uri)
        SharedFile(
            filename = file.name,
            mimeType = null,
            bytes = file.readBytes()
        )
    }

    private fun deleteCachedFile(path: String) {
        java.io.File(path).delete()
    }

    private fun sendUploadingNotification(label: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, SHARE_UPLOADS_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Uploading to $label")
            .setContentText("Queued for upload")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setAutoCancel(false)

        notificationManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())
    }

    private fun sendFinishedNotification(label: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, SHARE_UPLOADS_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Share Upload")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setOngoing(false)

        notificationManager.notify(SENT_NOTIFICATION_ID, builder.build())
    }
}
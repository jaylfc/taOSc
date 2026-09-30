package com.taosc.taosc

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@HiltWorker
class ShareUploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val channel = Channel<String>(Channel.CONFLATED)
        channel.send("Uploading to ${params.inputData.getString("label", "")}")
        
        val token = EncryptedCredentialStore.create(applicationContext).readToken()
        if (token == null) {
            channel.send("Pair this phone again")
            val cachePath = params.inputData.getString("cachePath", "")
            if (!cachePath.isNullOrEmpty()) {
                java.io.File(cachePath).delete()
            }
            return Result.failure()
        }
        
        val prefs = applicationContext.getSharedPreferences("taosc_settings", Context.MODE_PRIVATE)
        val settingsStore = SettingsStore(prefs)
        val baseUrl = settingsStore.serverUrl
        
        val attempt = runAttemptCount + 1
        
        val kindStr = params.inputData.getString("kind", "")
        val destination = ShareDestination(
            kind = ShareDestinationKind.valueOf(kindStr),
            id = params.inputData.getString("id", ""),
            label = params.inputData.getString("label", ""),
            channelId = params.inputData.getString("channelId", "")
        )
        
        val itemKind = params.inputData.getString("itemKind", "")
        val item = when (itemKind) {
            "Text" -> ShareItem.Text(params.inputData.getString("text", ""))
            "Link" -> ShareItem.Link(
                url = params.inputData.getString("url", ""),
                title = params.inputData.getString("title", "")
            )
            "File" -> ShareItem.File(
                uri = "",
                mimeType = params.inputData.getString("mimeType", "")
            )
            else -> throw IllegalArgumentException("Unknown item kind: $itemKind")
        }
        
        val displayName = params.inputData.getString("displayName", "")
        val mimeType = params.inputData.getString("mimeType", "")
        val cachePath = params.inputData.getString("cachePath", "")
        
        val fileReader: (uri: String) -> SharedFile = { _ ->
            SharedFile(
                filename = displayName,
                mimeType = mimeType,
                bytes = java.io.File(cachePath).readBytes()
            )
        }
        
        val sender = ShareSender(DefaultHttpClient()) { java.util.UUID.randomUUID().toString() }
        val result = sender.send(baseUrl, token, destination, item, fileReader)
        
        val decision = decide(result, attempt)
        
        return when (decision) {
            is RetryDecision.Done -> {
                if (!cachePath.isNullOrEmpty()) {
                    java.io.File(cachePath).delete()
                }
                channel.send("Sent to ${destination.label}")
                Result.success()
            }
            is RetryDecision.GiveUp -> {
                if (!cachePath.isNullOrEmpty()) {
                    java.io.File(cachePath).delete()
                }
                channel.send(decision.reason)
                Result.failure()
            }
            is RetryDecision.RetryLater -> Result.retry()
        }
    }
}
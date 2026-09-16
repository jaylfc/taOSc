package com.taosc.taosc

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput

class DecisionActionReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_ACTION_TYPE = "action_type"
        const val EXTRA_DECISION_TITLE = "decision_title"
        const val EXTRA_DECISION_BODY = "decision_body"
        const val EXTRA_DECISION_ID = "decision_id"
        const val EXTRA_DECISION_TYPE = "decision_type"
        const val EXTRA_PICK_VALUE = "pick_value"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: return
        val decisionTitle = intent.getStringExtra(EXTRA_DECISION_TITLE) ?: ""
        val decisionBody = intent.getStringExtra(EXTRA_DECISION_BODY) ?: ""
        val decisionId = intent.getStringExtra(EXTRA_DECISION_ID) ?: return
        val decisionType = intent.getStringExtra(EXTRA_DECISION_TYPE) ?: ""
        
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val quickReplyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString() ?: ""
        
        val payload = DecisionPayload(
            title = decisionTitle,
            body = decisionBody,
            decisionType = decisionType,
            decisionId = decisionId,
            actions = emptyList(),
            image = null,
            raw = emptyMap()
        )
        
        val action = when (actionType) {
            "approve" -> DecisionAction.Approve
            "deny" -> DecisionAction.Deny
            "pick" -> {
                val pickValue = intent.getStringExtra(EXTRA_PICK_VALUE) ?: ""
                DecisionAction.Pick(pickValue, pickValue)
            }
            "quick_reply" -> DecisionAction.QuickReply
            "add_note" -> DecisionAction.AddNote
            else -> return
        }
        
        val asyncResult = goAsync()
        
        Thread {
            try {
                val prefs = context.getSharedPreferences("taosc_settings", Context.MODE_PRIVATE)
                val settingsStore = SettingsStore(prefs)
                val credentialStore = EncryptedCredentialStore.create(context)
                
                val baseUrl = settingsStore.serverUrl
                val deviceId = settingsStore.deviceId
                val scopedToken = credentialStore.readToken()
                
                if (baseUrl.isBlank() || deviceId.isBlank() || scopedToken == null) {
                    asyncResult.finish()
                    return@Thread
                }
                
                val text = if (action is DecisionAction.QuickReply || action is DecisionAction.AddNote) {
                    quickReplyText
                } else ""
                
                val outboundCall = DecisionActionMapper.toOutboundCall(
                    action = action,
                    payload = payload,
                    baseUrl = baseUrl,
                    deviceId = deviceId,
                    scopedToken = scopedToken,
                    quickReplyText = text
                )
                
                if (outboundCall != null) {
                    val client = DefaultHttpClient()
                    val headers = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to "Bearer $scopedToken"
                    )
                    client.post(outboundCall.url, outboundCall.body, headers)
                }
                
                val notificationManager = DecisionNotificationManager(context)
                notificationManager.dismissNotification(decisionId)
            } catch (e: Exception) {
                // best-effort
            } finally {
                asyncResult.finish()
            }
        }.start()
    }
}

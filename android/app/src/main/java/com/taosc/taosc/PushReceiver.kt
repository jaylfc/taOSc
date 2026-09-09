package com.taosc.taosc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PushReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MESSAGE = "org.unifiedpush.android.ACTION_MESSAGE"
        const val EXTRA_MESSAGE = "message"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MESSAGE) return
        
        val messageJson = intent.getStringExtra(EXTRA_MESSAGE) ?: return
        val payload = DecisionPayloadParser.parse(messageJson)
        
        if (payload == null) {
            return
        }
        
        val notificationManager = DecisionNotificationManager(context)
        notificationManager.showDecisionNotification(payload)
    }
}

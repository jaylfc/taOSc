package com.taosc.taosc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

class DecisionNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "decisions"
        const val NOTIFICATION_ID = 1001
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
    
    init {
        createChannel()
    }
    
    fun showDecisionNotification(payload: DecisionPayload) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val actions = payload.actions.map { action ->
            when (action) {
                is DecisionAction.QuickReply -> {
                    val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                        .setLabel("Reply")
                        .build()
                    
                    val resultIntent = Intent(context, DecisionActionReceiver::class.java).apply {
                        putExtra(DecisionActionReceiver.EXTRA_ACTION_TYPE, "quick_reply")
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_TITLE, payload.title)
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_BODY, payload.body)
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_ID, payload.decisionId)
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_TYPE, payload.decisionType)
                    }
                    
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        action.hashCode(),
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    NotificationCompat.Action.Builder(
                        0,
                        "Reply",
                        pendingIntent
                    ).addRemoteInput(remoteInput).build()
                }
                else -> {
                    val resultIntent = Intent(context, DecisionActionReceiver::class.java).apply {
                        putExtra(DecisionActionReceiver.EXTRA_ACTION_TYPE, actionToString(action))
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_TITLE, payload.title)
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_BODY, payload.body)
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_ID, payload.decisionId)
                        putExtra(DecisionActionReceiver.EXTRA_DECISION_TYPE, payload.decisionType)
                        if (action is DecisionAction.Pick) {
                            putExtra(DecisionActionReceiver.EXTRA_PICK_VALUE, action.value)
                        }
                    }
                    
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        action.hashCode(),
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    NotificationCompat.Action.Builder(
                        0,
                        actionLabel(action),
                        pendingIntent
                    ).build()
                }
            }
        }
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
        
        actions.forEach { builder.addAction(it) }
        
        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID + (payload.decisionId?.hashCode() ?: 0), notification)
    }
    
    fun dismissNotification(decisionId: String?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = NOTIFICATION_ID + (decisionId?.hashCode() ?: 0)
        notificationManager.cancel(id)
    }
    
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Decisions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Agent decision notifications"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun actionToString(action: DecisionAction): String {
        return when (action) {
            is DecisionAction.Approve -> "approve"
            is DecisionAction.Deny -> "deny"
            is DecisionAction.Pick -> "pick"
            is DecisionAction.QuickReply -> "quick_reply"
            is DecisionAction.AddNote -> "add_note"
        }
    }
    
    private fun actionLabel(action: DecisionAction): String {
        return when (action) {
            is DecisionAction.Approve -> "Approve"
            is DecisionAction.Deny -> "Deny"
            is DecisionAction.Pick -> action.label
            is DecisionAction.QuickReply -> "Reply"
            is DecisionAction.AddNote -> "Add note"
        }
    }
}

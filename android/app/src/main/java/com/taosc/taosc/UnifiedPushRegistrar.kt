package com.taosc.taosc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger

interface DistributorDiscovery {
    fun findDistributor(): String?
}

class PackageManagerDiscovery(private val packageManager: PackageManager) : DistributorDiscovery {
    override fun findDistributor(): String? {
        val intent = Intent("org.unifiedpush.distributor.PROVIDER")
        val providers = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        return providers.firstOrNull()?.serviceInfo?.packageName
    }
}

class UnifiedPushRegistrar(
    private val broadcastSender: (Intent) -> Unit,
    private val discovery: DistributorDiscovery,
    private val prefs: android.content.SharedPreferences
) {
    companion object {
        const val ACTION_REGISTER = "org.unifiedpush.android.ACTION_REGISTER"
        const val ACTION_UNREGISTER = "org.unifiedpush.android.ACTION_UNREGISTER"
        const val EXTRA_APP_ID = "app"
        const val EXTRA_MESSENGER = "messenger"
        const val MSG_REGISTERED = 1
        const val MSG_UNREGISTERED = 2
    }
    
    private val messenger: Messenger by lazy { Messenger(IncomingHandler()) }
    
    fun discoverDistributor(): String? = discovery.findDistributor()
    
    fun register(appId: String, onResult: (String?) -> Unit): Boolean {
        val distributor = discoverDistributor() ?: return false
        pendingResult = onResult
        val intent = Intent(ACTION_REGISTER).apply {
            `package` = distributor
            putExtra(EXTRA_APP_ID, appId)
            putExtra(EXTRA_MESSENGER, messenger)
        }
        broadcastSender(intent)
        return true
    }
    
    fun unregister(appId: String, onResult: (() -> Unit)? = null) {
        val distributor = discoverDistributor() ?: return
        pendingUnregisterResult = onResult
        val intent = Intent(ACTION_UNREGISTER).apply {
            `package` = distributor
            putExtra(EXTRA_APP_ID, appId)
            putExtra(EXTRA_MESSENGER, messenger)
        }
        broadcastSender(intent)
    }
    
    private var pendingResult: ((String?) -> Unit)? = null
    private var pendingUnregisterResult: (() -> Unit)? = null
    
    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REGISTERED -> {
                    val endpoint = msg.obj as? String
                    val result = pendingResult
                    pendingResult = null
                    result?.invoke(endpoint)
                    onRegistered(endpoint)
                }
                MSG_UNREGISTERED -> {
                    val result = pendingUnregisterResult
                    pendingUnregisterResult = null
                    result?.invoke()
                    onUnregistered()
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    
    private fun onRegistered(endpoint: String?) {
        val store = PushTokenStore(prefs)
        if (endpoint != null) {
            store.endpoint = endpoint
        } else {
            store.clear()
        }
    }
    
    private fun onUnregistered() {
        val store = PushTokenStore(prefs)
        store.clear()
    }
}

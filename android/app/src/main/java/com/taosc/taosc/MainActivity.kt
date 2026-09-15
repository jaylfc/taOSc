package com.taosc.taosc

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("taosc_settings", Context.MODE_PRIVATE)
        val settingsStore = SettingsStore(prefs)
        val credentialStore = EncryptedCredentialStore.create(this)
        
        if (settingsStore.hasServerUrl && settingsStore.isPaired(credentialStore)) {
            registerForPush(settingsStore, credentialStore)
        }
        
        setContent {
            val initialScreen = if (settingsStore.hasServerUrl && settingsStore.isPaired(credentialStore)) {
                Screen.WebView(settingsStore.serverUrl)
            } else if (settingsStore.hasServerUrl) {
                Screen.Pairing(settingsStore.serverUrl)
            } else {
                Screen.Settings
            }
            
            var currentScreen by remember { mutableStateOf(initialScreen) }
            
            when (val screen = currentScreen) {
                is Screen.Settings -> SettingsScreen(
                    onUrlSet = { url ->
                        settingsStore.serverUrl = url
                        currentScreen = Screen.Pairing(url)
                    }
                )
                is Screen.Pairing -> PairingGrantScreen(
                    baseUrl = screen.baseUrl,
                    onComplete = {
                        registerForPush(settingsStore, credentialStore)
                        currentScreen = Screen.WebView(settingsStore.serverUrl)
                    },
                    onDismiss = {
                        currentScreen = Screen.Settings
                    },
                    credentialStore = credentialStore,
                    settingsStore = settingsStore
                )
                is Screen.WebView -> WebViewScreen(url = screen.url)
            }
        }
    }
    
    private fun registerForPush(settingsStore: SettingsStore, credentialStore: CredentialStore) {
        Thread {
            try {
                val prefs = getSharedPreferences("taosc_settings", Context.MODE_PRIVATE)
                val registrar = UnifiedPushRegistrar(
                    broadcastSender = { intent -> sendBroadcast(intent) },
                    discovery = PackageManagerDiscovery(packageManager),
                    prefs = prefs
                )
                
                val distributor = registrar.discoverDistributor()
                if (distributor == null) {
                    return@Thread
                }
                
                val latch = java.util.concurrent.CountDownLatch(1)
                
                registrar.register(packageName) { _ ->
                    latch.countDown()
                }
                
                latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                
                val pushStore = PushTokenStore(prefs)
                
                if (pushStore.hasEndpoint) {
                    val scopedToken = credentialStore.readToken() ?: return@Thread
                    val service = PairingService()
                    service.updatePushToken(
                        baseUrl = settingsStore.serverUrl,
                        deviceId = settingsStore.deviceId,
                        pushToken = pushStore.endpoint!!,
                        scopedToken = scopedToken
                    )
                }
            } catch (e: Exception) {
                // best-effort
            }
        }.start()
    }
}

sealed class Screen {
    data object Settings : Screen()
    data class Pairing(val baseUrl: String) : Screen()
    data class WebView(val url: String) : Screen()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String) {
    if (WebViewUtils.isHttpUrl(url)) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Text(text = "Invalid URL: $url")
    }
}

@Composable
fun SettingsScreen(onUrlSet: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("taOS Base URL") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )
        Button(
            onClick = { onUrlSet(url) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Pair")
        }
    }
}

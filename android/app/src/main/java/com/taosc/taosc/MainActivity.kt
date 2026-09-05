package com.taosc.taosc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("taosc_settings", Context.MODE_PRIVATE)
        val settingsStore = SettingsStore(prefs)
        val credentialStore = EncryptedCredentialStore.create(this)
        
        val url = if (settingsStore.hasServerUrl && settingsStore.isPaired(credentialStore)) {
            settingsStore.serverUrl
        } else {
            ""
        }
        
        setContent {
            WebViewScreen(url = url)
        }
    }
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

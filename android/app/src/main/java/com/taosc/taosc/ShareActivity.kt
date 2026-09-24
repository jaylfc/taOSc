package com.taosc.taosc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent ?: run {
            setContent { CloseScreen() }
            return
        }

        val action = intent.action
        val type = intent.type
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        val streamUris = when (action) {
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.map { it.toString() } ?: emptyList()
            }
            else -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) listOf(uri.toString()) else emptyList()
            }
        }

        val items = ShareIntakeParser.parse(
            action = action,
            mimeType = type,
            text = text,
            subject = subject,
            streamUris = streamUris
        )

        val credentialStore = EncryptedCredentialStore.create(this)

        setContent {
            if (credentialStore.readToken() == null) {
                PairRequiredScreen()
            } else {
                ShareItemsScreen(items = items, onClose = { finish() })
            }
        }
    }

    @Composable
    fun PairRequiredScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pair this phone with taOS first",
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = { finish() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Close")
            }
        }
    }

    @Composable
    fun ShareItemsScreen(items: List<ShareItem>, onClose: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Received ${items.size} item${if (items.size == 1) "" else "s"}",
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .weight(1f)
            ) {
                items(items) { item ->
                    Text(
                        text = when (item) {
                            is ShareItem.Text -> "Text: ${item.text}"
                            is ShareItem.Link -> "Link: ${item.url}${item.title?.let { " ($it)" } ?: ""}"
                            is ShareItem.File -> "File: ${item.uri}${item.mimeType?.let { " ($it)" } ?: ""}"
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Button(
                onClick = onClose,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Close")
            }
        }
    }

    @Composable
    fun CloseScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nothing to share")
            Button(onClick = { finish() }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Close")
            }
        }
    }
}

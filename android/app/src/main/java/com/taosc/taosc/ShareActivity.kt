package com.taosc.taosc

import android.content.ContentResolver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val token = credentialStore.readToken()

        setContent {
            if (token == null) {
                PairRequiredScreen()
            } else {
                val prefs = getSharedPreferences("taosc_settings", android.content.Context.MODE_PRIVATE)
                val settingsStore = SettingsStore(prefs)
                val baseUrl = settingsStore.serverUrl
                ShareScreen(baseUrl = baseUrl, token = token, items = items)
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
    fun ShareScreen(baseUrl: String, token: String, items: List<ShareItem>) {
        val scope = rememberCoroutineScope()
        var destinations by remember { mutableStateOf<List<ShareDestination>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var sendResult by remember { mutableStateOf<SendResult?>(null) }

        val httpClient = remember { DefaultHttpClient() }
        val shareSender = remember { ShareSender(httpClient) { java.util.UUID.randomUUID().toString() } }

        fun readFile(uri: String): SharedFile {
            val contentResolver: ContentResolver = applicationContext.contentResolver
            val cursor = contentResolver.query(Uri.parse(uri), null, null, null, null)
            val displayName = cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) it.getString(nameIndex) else null
                } else null
            } ?: Uri.parse(uri).lastPathSegment ?: "file"

            val mimeType = contentResolver.getType(Uri.parse(uri))
            val bytes = contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Cannot read file")

            return SharedFile(filename = displayName, mimeType = mimeType, bytes = bytes)
        }

        fun copyFileToCache(uri: String): String {
            val contentResolver: ContentResolver = applicationContext.contentResolver
            val inputStream = contentResolver.openInputStream(Uri.parse(uri)) ?: return ""
            
            val cacheDir = applicationContext.cacheDir
            val filename = "share_${System.currentTimeMillis()}"
            val cacheFile = java.io.File(cacheDir, filename)
            
            try {
                cacheFile.outputStream().use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                return cacheFile.absolutePath
            } catch (e: Exception) {
                return ""
            }
        }

        fun fetchDestinations() {
            isLoading = true
            errorMessage = null
            scope.launch(Dispatchers.IO) {
                try {
                    val client = ShareDestinationsClient(httpClient)
                    val result = client.fetch(baseUrl, token)
                    destinations = result
                } catch (e: ShareDestinationsError) {
                    errorMessage = when (e) {
                        is ShareDestinationsError.NotPaired -> "Pair this phone again"
                        is ShareDestinationsError.Unreachable -> "Could not reach taOS"
                        is ShareDestinationsError.InvalidResponse -> "taOS returned an invalid response"
                    }
                } catch (e: Exception) {
                    errorMessage = "Could not reach taOS"
                } finally {
                    isLoading = false
                }
            }
        }

        fun sendToDestination(destination: ShareDestination) {
            if (!shareSender.canSend(destination, items)) {
                errorMessage = "taOS refused this destination"
                return
            }
            
            scope.launch(Dispatchers.IO) {
                var copyFailed = false
                for (item in items) {
                    when (item) {
                        is ShareItem.File -> {
                            val path = copyFileToCache(item.uri)
                            if (path.isEmpty()) {
                                copyFailed = true
                                continue
                            }
                            val shared = readFile(item.uri)
                            val displayName = shared.filename
                            val mimeType = shared.mimeType
                            
                            val data = workDataOf(
                                "kind" to destination.kind.name,
                                "id" to destination.id,
                                "label" to destination.label,
                                "channelId" to (destination.channelId ?: ""),
                                "itemKind" to "File",
                                "text" to "",
                                "url" to "",
                                "title" to "",
                                "cachePath" to path,
                                "displayName" to displayName,
                                "mimeType" to mimeType
                            )
                            
                            val request = OneTimeWorkRequestBuilder<ShareUploadWorker>()
                                .setInputData(data)
                                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            
                            WorkManager.getInstance(applicationContext).enqueue(request)
                        }
                        is ShareItem.Text -> {
                            val data = workDataOf(
                                "kind" to destination.kind.name,
                                "id" to destination.id,
                                "label" to destination.label,
                                "channelId" to (destination.channelId ?: ""),
                                "itemKind" to "Text",
                                "text" to item.text,
                                "url" to "",
                                "title" to "",
                                "cachePath" to "",
                                "displayName" to "",
                                "mimeType" to ""
                            )
                            
                            val request = OneTimeWorkRequestBuilder<ShareUploadWorker>()
                                .setInputData(data)
                                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            
                            WorkManager.getInstance(applicationContext).enqueue(request)
                        }
                        is ShareItem.Link -> {
                            val data = workDataOf(
                                "kind" to destination.kind.name,
                                "id" to destination.id,
                                "label" to destination.label,
                                "channelId" to (destination.channelId ?: ""),
                                "itemKind" to "Link",
                                "text" to "",
                                "url" to item.url,
                                "title" to (item.title ?: ""),
                                "cachePath" to "",
                                "displayName" to "",
                                "mimeType" to ""
                            )
                            
                            val request = OneTimeWorkRequestBuilder<ShareUploadWorker>()
                                .setInputData(data)
                                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            
                            WorkManager.getInstance(applicationContext).enqueue(request)
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (copyFailed) {
                        errorMessage = "Could not read the shared file"
                    } else {
                        Toast.makeText(this@ShareActivity, "Queued", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            fetchDestinations()
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Loading destinations...", modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = errorMessage!!, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
                    Button(
                        onClick = { fetchDestinations() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
            destinations.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No destinations available", modifier = Modifier.padding(horizontal = 32.dp))
                    Button(
                        onClick = { finish() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
            sendResult == SendResult.Sent -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Sent", modifier = Modifier.padding(horizontal = 32.dp))
                }
                androidx.compose.runtime.LaunchedEffect(sendResult) {
                    finish()
                }
            }
            sendResult != null && sendResult !is SendResult.Sent -> {
                val result = sendResult!!
                val message = when (result) {
                    is SendResult.NotPaired -> "Pair this phone again"
                    is SendResult.Rejected -> "taOS refused this destination"
                    is SendResult.Unreachable -> "Could not reach taOS"
                    else -> "Unknown error"
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = message, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
                    Button(
                        onClick = {
                            sendResult = null
                            fetchDestinations()
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose a destination",
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .weight(1f)
                    ) {
                        items(destinations) { destination ->
                            val canSend = shareSender.canSend(destination, items)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .then(
                                        if (canSend) {
                                            Modifier.clickable { sendToDestination(destination) }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                Text(text = destination.label)
                                Text(
                                    text = destination.kind.name,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { finish() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Close")
                    }
                }
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

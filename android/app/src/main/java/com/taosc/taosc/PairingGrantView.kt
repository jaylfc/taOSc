package com.taosc.taosc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

sealed class PairingPhase {
    data object Creating : PairingPhase()
    data class Pending(val verifyCode: String) : PairingPhase()
    data object Approved : PairingPhase()
    data object Denied : PairingPhase()
    data object Expired : PairingPhase()
    data class Unreachable(val message: String) : PairingPhase()
}

@Composable
fun PairingGrantScreen(
    baseUrl: String,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    credentialStore: CredentialStore,
    settingsStore: SettingsStore
) {
    var phase by remember { mutableStateOf<PairingPhase>(PairingPhase.Creating) }
    var retryToken by remember { mutableStateOf(0) }

    LaunchedEffect(baseUrl, retryToken) {
        try {
            val service = PairingService()
            val response = service.createPairRequest(baseUrl, "android", "Android Device")
            phase = PairingPhase.Pending(response.verifyCode)

            while (true) {
                val status = service.pollPairRequest(baseUrl, response.pairRequestId)
                when (status) {
                    PairRequestStatus.Pending -> delay(3000)
                    is PairRequestStatus.Approved -> {
                        credentialStore.saveToken(status.scopedToken)
                        phase = PairingPhase.Approved
                        onComplete()
                        return@LaunchedEffect
                    }
                    PairRequestStatus.Denied -> {
                        phase = PairingPhase.Denied
                        return@LaunchedEffect
                    }
                    PairRequestStatus.Expired -> {
                        phase = PairingPhase.Expired
                        return@LaunchedEffect
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            phase = PairingPhase.Unreachable(e.message ?: "Unknown error")
        }
    }

    when (val currentPhase = phase) {
        is PairingPhase.Creating -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Creating pairing request...")
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
        is PairingPhase.Pending -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Compare this code with the one shown on your taOS instance.",
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Text(
                    text = currentPhase.verifyCode,
                    modifier = Modifier.padding(16.dp)
                )
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
        is PairingPhase.Approved -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pairing approved. Opening...")
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
        is PairingPhase.Denied -> {
            TerminalStateView(
                title = "Pairing Denied",
                message = "The request was denied. Please try again.",
                buttonTitle = "Back",
                action = onDismiss
            )
        }
        is PairingPhase.Expired -> {
            TerminalStateView(
                title = "Pairing Expired",
                message = "The verification code has expired. Please try again.",
                buttonTitle = "Back",
                action = onDismiss
            )
        }
        is PairingPhase.Unreachable -> {
            ErrorRetryView(
                message = currentPhase.message,
                retry = {
                    phase = PairingPhase.Creating
                    retryToken++
                }
            )
        }
    }
}

@Composable
fun TerminalStateView(
    title: String,
    message: String,
    buttonTitle: String,
    action: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
        Button(
            onClick = action,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(buttonTitle)
        }
    }
}

@Composable
fun ErrorRetryView(
    message: String,
    retry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Server Unreachable", modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
        Button(
            onClick = retry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Retry")
        }
    }
}

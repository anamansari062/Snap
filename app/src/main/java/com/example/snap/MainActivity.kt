package com.example.snap

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.snap.ui.theme.SnapTheme
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityResultSender = ActivityResultSender(this)

        setContent {
            SnapTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(
                        identityUri = Uri.parse(application.getString((R.string.id_url))),
                        iconUri = Uri.parse(application.getString(R.string.id_favicon)),
                        identityName = application.getString(R.string.app_name),
                        activityResultSender = activityResultSender,
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    identityUri: Uri,
    iconUri: Uri,
    identityName: String,
    activityResultSender: ActivityResultSender,
    snapViewModel: SnapViewModel = SnapViewModel()
) {
    val viewState by snapViewModel.viewState.collectAsState()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to Snap!", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You can connect to your wallet and sign a message on chain.",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (viewState.userAddress.isEmpty()) {
                snapViewModel.connect(identityUri, iconUri, identityName, activityResultSender)
            } else {
                snapViewModel.disconnect()
            }
        }) {
            val pubKey = viewState.userAddress
            val buttonText = when {
                viewState.noWallet -> "Please install a wallet"
                pubKey.isEmpty() -> "Connect Wallet"
                viewState.userAddress.isNotEmpty() -> pubKey.take(4).plus("...")
                    .plus(pubKey.takeLast(4))

                else -> ""
            }

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = buttonText,
                maxLines = 1,
            )
        }

        Button(
            onClick = {
                snapViewModel.sign_message(identityUri, iconUri, identityName, activityResultSender)
            },
        ) {
            Text(text = "Sign Message")
        }

    }
}
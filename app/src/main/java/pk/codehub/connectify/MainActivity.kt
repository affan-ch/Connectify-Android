package pk.codehub.connectify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pk.codehub.connectify.ui.theme.ConnectifyTheme
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConnectifyTheme {
                WebRTCApp()
            }
        }
    }
}

@Composable
fun WebRTCApp(viewModel: WebRTCViewModel = hiltViewModel()) {
    var offer by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var messageToSend by remember { mutableStateOf("") }

    val receivedMessages by viewModel.receivedMessages.observeAsState(emptyList())
    val createdAnswer by viewModel.answer.observeAsState("")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = offer,
            onValueChange = { offer = it },
            label = { Text("Offer") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { viewModel.setOfferFromJson(offer) }) {
            Text("Set Offer")
        }

        OutlinedTextField(
            value = createdAnswer,
            onValueChange = { answer = it },
            label = { Text("Answer") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = messageToSend,
            onValueChange = { messageToSend = it },
            label = { Text("Send Message") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { viewModel.sendMessage(messageToSend) }) {
            Text("Send Message")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Received Messages:")
        receivedMessages.forEach { message ->
            Text(message)
        }
    }
}
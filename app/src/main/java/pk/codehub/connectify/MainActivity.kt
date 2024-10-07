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
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pk.codehub.connectify.ui.screens.SignUpScreen
import pk.codehub.connectify.ui.screens.SignInScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pk.codehub.connectify.ui.screens.HomeScreen
import pk.codehub.connectify.utils.DataStoreManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConnectifyTheme {
//                WebRTCApp()
                MyAppContent()
            }
        }
    }
}


@Composable
fun MyAppContent() {
    val navController = rememberNavController()
    val startDestination = remember {
        mutableStateOf("sign_in")
    }

    val appContext = LocalContext.current.applicationContext

    // Launch an effect to check if token exists in DataStore
    LaunchedEffect(Unit) {
        // Use DataStoreManager to get the token
        DataStoreManager.getValue(appContext, "token", "").collect { token ->
            if (token.isNotEmpty()) {
                startDestination.value = "home"
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination.value) {
        composable("sign_in") { SignInScreen(navController) }
        composable("sign_up") { SignUpScreen(navController) }
        composable("home") { HomeScreen(navController) }
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
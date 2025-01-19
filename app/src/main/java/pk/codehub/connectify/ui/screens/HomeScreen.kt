package pk.codehub.connectify.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import pk.codehub.connectify.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import pk.codehub.connectify.utils.DataStoreManager
import pk.codehub.connectify.models.Device
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.delay
import org.json.JSONObject
import pk.codehub.connectify.viewmodels.WebRTCViewModel
import java.net.URISyntaxException
import pk.codehub.connectify.utils.ApiRoutes


@Composable
fun WebRTC_Manager(viewModel: WebRTCViewModel = hiltViewModel()) {
    var socket: Socket? = null

    try {
        socket = IO.socket(ApiRoutes.BASE_URL)
        Log.i("Socket.io", "Initialized")
    } catch (e: URISyntaxException) {
        e.printStackTrace()
    }

    // Connect to the server
    socket?.connect()

    val appContext = LocalContext.current.applicationContext
    val loginToken = DataStoreManager.getValue(appContext, "token", "").collectAsState(initial = "").value
    val deviceToken = DataStoreManager.getValue(appContext, "deviceToken", "").collectAsState(initial = "").value


    // Register the device with the server
    socket?.emit("register", JSONObject().apply {
        put("loginToken", loginToken)
        put("deviceToken", deviceToken)
    })

    val createdAnswer by viewModel.answer.observeAsState("")
    var receivedOffer by remember { mutableStateOf<OfferData?>(null) }


    // Listen for WebRTC offers from the server
    socket?.on("webrtc_offer") { args ->
        Log.d("WebRTC", "Received Offer")
        val offer = (args[0] as JSONObject).getString("offer")
        val deviceId = (args[0] as JSONObject).getString("callbackDeviceId")

        Log.d("WebRTC", "Received offer from Device $deviceId: $offer")

        viewModel.setOfferFromJson(offer)

        receivedOffer = OfferData(offer, deviceId)
    }

    LaunchedEffect(key1 = receivedOffer) {
        receivedOffer?.let {
            delay(2000L)
            socket?.emit("webrtc_answer", JSONObject().apply {
                put("loginToken", loginToken)
                put("deviceToken", deviceToken)
                put("deviceId", it.deviceId)
                put("answer", createdAnswer)
            })

            Log.d("WebRTC", "Sent Answer to Device ${it.deviceId}: $createdAnswer")
        }
    }
}

data class OfferData(val offer: String, val deviceId: String)

@Composable
fun HomeContent(
    viewModel: WebRTCViewModel = hiltViewModel()
) {

    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val righteous = GoogleFont("Righteous")

    val fontFamily = FontFamily(
        Font(googleFont = righteous, fontProvider = provider)
    )

    val context = LocalContext.current
    val devicesState = remember { mutableStateOf<List<Device>>(emptyList()) }
    val status by viewModel.state.observeAsState("disconnected")

    LaunchedEffect(Unit) {
        val devicesJson = DataStoreManager.getValue(context, "devices", "").first()

        if (devicesJson.isEmpty()) {
            return@LaunchedEffect
        }

        val devices = Json.decodeFromString<List<Device>>(devicesJson)
        devicesState.value = devices.filter { it.deviceType.equals("desktop", true) }
    }

    val device = devicesState.value.firstOrNull() // TODO: Implement for all desktop devices

    Log.d("HomeContent", "Final Device: $device")

    WebRTC_Manager()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back!",
                fontSize = 35.sp,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(10.dp))

            device?.deviceName?.let {
                Text(
                    text = it,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            device?.let {
                Text(
                    text = "${it.osName} ${it.osVersion}",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Status: $status",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = if (status == "connected") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

        }
    }
}


@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { NavigationGraph(navController = navController) }
    }
}

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val hasNews: Boolean,
    val badgeCount: Int? = null
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "home", false, null),
        BottomNavItem(
            "Chat",
            Icons.AutoMirrored.Filled.Send,
            Icons.AutoMirrored.Outlined.Send,
            "chat",
            true,
            2
        ),
        BottomNavItem("Files", Icons.Filled.Folder, Icons.Outlined.Folder, "files", true, null),
        BottomNavItem(
            "Settings",
            Icons.Filled.Settings,
            Icons.Outlined.Settings,
            "settings",
            false,
            null
        )
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }


    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    BadgedBox(badge = {
                        if (item.badgeCount != null) {
                            Badge {
                                Text(text = item.badgeCount.toString())
                            }
                        } else if (item.hasNews) {
                            Badge()
                        }
                    }) {
                        Icon(
                            imageVector = if (selectedItemIndex == index) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                },

                label = {
                    Text(
                        text = item.title,
                        fontSize = 12.sp
                    )
                },
                selected = selectedItemIndex == index,
                alwaysShowLabel = true,
                onClick = {
                    selectedItemIndex = index
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },

                )
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeContent() }
        composable("chat") { ChatScreen() }
        composable("files") { FileManagerScreen() }
        composable("settings") { SettingsScreen() }
    }
}
package pk.codehub.connectify

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import pk.codehub.connectify.ui.theme.ConnectifyTheme
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pk.codehub.connectify.ui.screens.SignUpScreen
import pk.codehub.connectify.ui.screens.SignInScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pk.codehub.connectify.services.AccessibilityService
import pk.codehub.connectify.services.ForegroundService
import pk.codehub.connectify.ui.screens.HomeScreen
import pk.codehub.connectify.ui.screens.TfaVerifyScreen
import pk.codehub.connectify.utils.DataStoreManager
import pk.codehub.connectify.utils.TokenManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConnectifyTheme {
                MyAppContent()
            }
        }

        // Start Foreground Service
        startForegroundService()

        // Redirect to Accessibility Settings
        if (!isAccessibilityServiceEnabled()) {
            promptAccessibilitySettings()
        }
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(this, AccessibilityService::class.java)
        val enabledServices =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service.flattenToString()) == true
    }

    private fun promptAccessibilitySettings() {
        Toast.makeText(this, "Please enable Accessibility Service for Clipboard Sync.", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}


@Composable
fun MyAppContent() {
    val navController = rememberNavController()

    val appContext = LocalContext.current.applicationContext

    CheckNotificationListenerPermission()

    // TODO("Implement Splash Screen as Start Destination")
    NavHost(navController = navController, startDestination = "sign_in") {
        composable("sign_in") { SignInScreen(navController) }
        composable("sign_up") { SignUpScreen(navController) }
        composable("tfa_verify") { TfaVerifyScreen(navController)  }
        composable("home") { HomeScreen() }
    }

    LaunchedEffect(Unit) {
        DataStoreManager.getValue(appContext, "token", "").collect { loginToken ->
            if (loginToken.isNotEmpty()) {
                val isValid = TokenManager.verifyLoginToken(appContext, loginToken)

                // If login token is invalid, navigate to sign in screen
                if(!isValid){
                    navController.navigate("sign_in") {
                        popUpTo(0) // Clears back stack
                    }
                }

                DataStoreManager.getValue(appContext, "deviceToken", "").collect { deviceToken ->
                    if (deviceToken.isEmpty()) {
                        val success = TokenManager.registerDevice(appContext, loginToken)

                        if(success){
                            Log.d("Device Token", "Device Registered Successfully")

                            navController.navigate("home") {
                                popUpTo(0) // Clears back stack
                            }
                        }
                        else{
                            navController.navigate("sign_in") {
                                popUpTo(0) // Clears back stack
                            }
                        }
                    } else {
                        val success = TokenManager.verifyDeviceToken(appContext, loginToken, deviceToken)

                        if(success){
                            Log.d("Device Token", "Device Verified Successfully")

                            navController.navigate("home") {
                                popUpTo(0) // Clears back stack
                            }
                        }
                        else{
                            navController.navigate("sign_in") {
                                popUpTo(0) // Clears back stack
                            }
                        }
                    }
                }
            } else {
                navController.navigate("sign_in") {
                    popUpTo(0) // Clears back stack
                }
            }
        }
    }
}


@Composable
fun CheckNotificationListenerPermission() {
    val context = LocalContext.current

    // Function to check if notification listener permission is granted
    fun isNotificationListenerEnabled(): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }
    // Remember the permission check result
    val hasPermission = remember { isNotificationListenerEnabled() }
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
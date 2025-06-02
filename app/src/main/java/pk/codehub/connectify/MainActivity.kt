package pk.codehub.connectify

import android.content.ComponentName
import android.content.Context
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
import pk.codehub.connectify.ui.screens.SplashScreen
import pk.codehub.connectify.ui.screens.TfaVerifyScreen
import pk.codehub.connectify.utils.DataStoreManager
import pk.codehub.connectify.utils.TokenManager
import android.app.NotificationManager
import android.net.Uri
import androidx.core.net.toUri


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
        
//        // Redirect to Accessibility Settings
//        if (!isAccessibilityServiceEnabled()) {
//            promptAccessibilitySettings()
//        }

        if(!ensureDndPermission(this)) {
            Toast.makeText(this, "Please grant Do Not Disturb access to Connectify.", Toast.LENGTH_LONG).show()
//            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
//            startActivity(intent)
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

//    private fun isAccessibilityServiceEnabled(): Boolean {
//        val service = ComponentName(this, AccessibilityService::class.java)
//        val enabledServices =
//            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
//        return enabledServices?.contains(service.flattenToString()) == true
//    }
//
//    private fun promptAccessibilitySettings() {
//        Toast.makeText(this, "Please enable Accessibility Service for Clipboard Sync.", Toast.LENGTH_LONG).show()
//        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
//        startActivity(intent)
//    }

    private fun ensureDndPermission(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

}


@Composable
fun MyAppContent() {
    val navController = rememberNavController()

    CheckNotificationListenerPermission()

    // TODO("Implement Splash Screen as Start Destination")
    NavHost(navController = navController, startDestination = "splash_screen") {
        composable("splash_screen") { SplashScreen(navController)  }
        composable("sign_in") { SignInScreen(navController) }
        composable("sign_up") { SignUpScreen(navController) }
        composable("tfa_verify") { TfaVerifyScreen(navController)  }
        composable("home") { HomeScreen() }
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
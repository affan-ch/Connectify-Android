package pk.codehub.connectify.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.first
import pk.codehub.connectify.utils.DataStoreManager
import pk.codehub.connectify.utils.TokenManager

@Composable
fun SplashScreen(navController: NavController) {
    var isVisible by remember { mutableStateOf(false) }
    val appContext = LocalContext.current.applicationContext

    LaunchedEffect(Unit) {
        isVisible = true

        val loginToken = DataStoreManager.getValue(appContext, "token", "").first() // Fetch only once

        if (loginToken.isNotEmpty()) {
            val isValid = TokenManager.verifyLoginToken(appContext, loginToken)

            if (!isValid) {
                isVisible = false
                navController.navigate("sign_in") { popUpTo(0) }
                return@LaunchedEffect
            }

            val deviceToken = DataStoreManager.getValue(appContext, "deviceToken", "").first() // Fetch only once

            if (deviceToken.isEmpty()) {
                val success = TokenManager.registerDevice(appContext, loginToken)

                if (success) {
                    Log.d("Device Token", "Device Registered Successfully")

                    // get refresh device token
                    val newDeviceToken = DataStoreManager.getValue(appContext, "deviceToken", "").first() // Fetch only once
                    if(newDeviceToken.isNotEmpty()){
                        val devices = TokenManager.verifyDeviceToken(appContext, loginToken, newDeviceToken)
                        if(devices){
                            Log.d("Device Token", "Device Verified Successfully")
                            isVisible = false
                            navController.navigate("home") { popUpTo(0) }
                        } else {
                            Log.d("Device Token", "Device Verification Failed")
                            isVisible = false
                            navController.navigate("sign_in") { popUpTo(0) }
                        }
                    }

                } else {
                    isVisible = false
                    navController.navigate("sign_in") { popUpTo(0) }
                }
            } else {
                val success = TokenManager.verifyDeviceToken(appContext, loginToken, deviceToken)

                if (success) {
                    Log.d("Device Token", "Device Verified Successfully")
                    isVisible = false
                    navController.navigate("home") { popUpTo(0) }
                } else {
                    isVisible = false
                    navController.navigate("sign_in") { popUpTo(0) }
                }
            }
        } else {
            isVisible = false
            navController.navigate("sign_in") { popUpTo(0) }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isVisible, enter = fadeIn()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading...", fontSize = 20.sp)
            }
        }
    }
}
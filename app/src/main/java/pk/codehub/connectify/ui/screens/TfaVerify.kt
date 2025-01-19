package pk.codehub.connectify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pk.codehub.connectify.R
import pk.codehub.connectify.ui.components.MaterialTextField
import pk.codehub.connectify.viewmodels.TfaVerifyViewModel

@Composable
fun TfaVerifyScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: TfaVerifyViewModel = viewModel()
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val righteous = GoogleFont("Righteous")

    val fontFamily = FontFamily(
        Font(googleFont = righteous, fontProvider = provider)
    )

    val otp by viewModel.otp.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Connectify",
                fontSize = 35.sp,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            ) // Branding

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter the 8-digit Otp from Authenticator App",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ) // Tagline

            Spacer(modifier = Modifier.height(15.dp))

            MaterialTextField(
                value = otp,
                onValueChange = { viewModel.onOtpChange(it) },
                label = "Otp"
            ) // Name Field

            Spacer(modifier = Modifier.height(25.dp))

            Button(modifier = Modifier
                .width(170.dp)
                .height(45.dp), onClick = { viewModel.tfaVerify(context, navController) }, enabled = !isLoading
            ) {
                Text("Verify & Continue", fontWeight = FontWeight.Bold)
            } // Login Button
        }
    }
}

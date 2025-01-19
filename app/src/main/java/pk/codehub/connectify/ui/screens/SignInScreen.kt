package pk.codehub.connectify.ui.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import pk.codehub.connectify.ui.components.PasswordFieldComponent
import pk.codehub.connectify.viewmodels.SignInViewModel


@Composable
fun SignInScreen(
    navController: NavController
) {
    var context = LocalContext.current
    val viewModel: SignInViewModel = viewModel()
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val righteous = GoogleFont("Righteous")

    val fontFamily = FontFamily(
        Font(googleFont = righteous, fontProvider = provider)
    )

    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
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
                text = "Sign in to continue using app",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ) // Tagline

            Spacer(modifier = Modifier.height(10.dp))

            MaterialTextField(
                value = email,
                onValueChange = {viewModel.onEmailChange(it)},
                label ="Email"
            ) // Email Field

            Spacer(modifier = Modifier.height(10.dp))

            PasswordFieldComponent(
                value = password,
                onValueChange = {viewModel.onPasswordChange(it)},
                label ="Password"
            ) // Password Field

            Spacer(modifier = Modifier.height(15.dp))

            Button(modifier = Modifier
                .width(150.dp)
                .height(45.dp), onClick = { viewModel.signIn(context, navController) }, enabled = !isLoading
            ) {
                Text("Login", fontWeight = FontWeight.Bold)
            } // Login Button

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Bottom) {

                Text(text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium)

                Text(
                    text = "Create One",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)

                        .clickable { navController.navigate("sign_up") },
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

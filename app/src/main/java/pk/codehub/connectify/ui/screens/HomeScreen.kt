package pk.codehub.connectify.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import pk.codehub.connectify.R

@Composable
fun HomeScreen(
    navController: NavController
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

    Text(
        text = "Connectify",
        fontSize = 35.sp,
        fontFamily = fontFamily,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyLarge
    )
}
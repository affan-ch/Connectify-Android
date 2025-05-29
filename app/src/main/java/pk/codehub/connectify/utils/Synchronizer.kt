package pk.codehub.connectify.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pk.codehub.connectify.viewmodels.WebRTCViewModel

class Synchronizer {
    @Serializable
    data class PackageInfoInput(
        val packageName: String,
        val versionCode: Long
    )

    @Serializable
    data class PackageIconInfo(
        val packageName: String,
        val versionCode: Long,
        val iconBase64: String
    )

    companion object{
        // Sync Function
        fun sync(viewModel: WebRTCViewModel, context: Context){
            // Launching in IO context to avoid blocking the main thread
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                    // Permissions granted, proceed with syncing device state
                    val deviceState = DeviceStateUtils.getDeviceState(context)

                    viewModel.sendMessage(
                        Json.encodeToString(deviceState),
                        "DeviceStateInfo"
                    )
                }
            }

        }

        // Function to get updated icons
        fun getUpdatedIcons(context: Context, manifestJson: String): String {
            val pm = context.packageManager
            val installed = pm.getInstalledPackages(0)

            // If empty string or empty array, return all installed packages
            if (manifestJson.isBlank() || manifestJson == "[]") {
                val allPackages = installed.map { pkgInfo ->
                    val pkg = pkgInfo.packageName
                    val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode
                    } else {
                        pkgInfo.versionCode.toLong()
                    }

                    val iconB64 = try {
                        val icon = pm.getApplicationIcon(pkg)
                        drawableToBase64(icon)
                    } catch (_: Exception) {
                        ""
                    }

                    if (iconB64 != null) {
                        PackageIconInfo(
                            packageName = pkg,
                            versionCode = version,
                            iconBase64 = iconB64
                        )
                    }
                    else null
                }

                return Json.encodeToString(allPackages)
            }

            // 1. Decode the incoming manifest
            val inputList: List<PackageInfoInput> = Json.decodeFromString(manifestJson)
            val inputMap = inputList.associateBy { it.packageName }

            // 2. Build the list of changed/new apps
            val toSend = installed.mapNotNull { pkgInfo ->
                val pkg = pkgInfo.packageName
                val installedVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    pkgInfo.versionCode.toLong()
                }

                // include if missing from input or version changed
                val known = inputMap[pkg]
                if (known == null || known.versionCode != installedVersion) {
                    val iconB64 = try {
                        val icon = pm.getApplicationIcon(pkg)
                        drawableToBase64(icon)
                    } catch (_: Exception) {
                        ""
                    }

                    if (iconB64 != null) {
                        PackageIconInfo(
                            packageName = pkg,
                            versionCode = installedVersion,
                            iconBase64 = iconB64
                        )
                    }
                    else null
                } else null
            }

            return Json.encodeToString(toSend)
        }

        // Function to convert a Drawable to a Base64 string
        private fun drawableToBase64(drawable: Drawable): String? {
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
                createBitmap(width, height).apply {
                    val canvas = android.graphics.Canvas(this)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        }
    }

}
package pk.codehub.connectify.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.os.Build
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
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
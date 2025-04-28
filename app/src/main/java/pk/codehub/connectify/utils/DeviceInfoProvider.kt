package pk.codehub.connectify.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.util.TimeZone
import android.provider.Settings

class DeviceInfoProvider {
    companion object {
        fun exportAsJson(context: Context): JSONObject {
            val deviceInfo = JSONObject()

            try {
                val deviceName = getDeviceName(context)
                val model = Build.MODEL ?: "Unknown"
                val osVersion = Build.VERSION.RELEASE ?: "Unknown"
                val deviceUuid = getDeviceUuid(context)
                val serialNumber = "Unknown"
                val boardId = Build.BOARD ?: "Unknown"
                val manufacturer = Build.MANUFACTURER ?: "Unknown"
                val timezone = TimeZone.getDefault().id

                deviceInfo.put("device_type", "mobile")
                deviceInfo.put("device_name", deviceName)
                deviceInfo.put("model", model)
                deviceInfo.put("os_name", "android")
                deviceInfo.put("os_version", osVersion)
                deviceInfo.put("uuid", deviceUuid)
                deviceInfo.put("serial_number", serialNumber)
                deviceInfo.put("board_id", boardId)
                deviceInfo.put("manufacturer", manufacturer)
                deviceInfo.put("timezone", timezone)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return deviceInfo
        }

        private fun getDeviceName(context: Context): String {
            return Settings.Global.getString(context.contentResolver, "device_name")
                ?: Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                ?: Build.MODEL // Fallback to model name
        }

        @SuppressLint("HardwareIds")
        fun getDeviceUuid(context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }

    }
}

package pk.codehub.connectify.utils

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresPermission
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.app.NotificationManager
import android.net.NetworkRequest
import org.json.JSONObject

class DeviceStateUtils private constructor() {

    companion object {

        @RequiresPermission(allOf = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_WIFI_STATE"])
        private fun getWifiInfo(context: Context): JSONObject {
            val json = JSONObject()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                        wifiInfo?.let {
                            json.put("SSID", it.ssid.replace("\"", ""))
                            json.put("BSSID", it.bssid)
                        }
                        connectivityManager.unregisterNetworkCallback(this)
                    }
                }

                connectivityManager.requestNetwork(request, networkCallback)
            } else {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                json.put("SSID", wifiInfo.ssid.replace("\"", ""))
                json.put("BSSID", wifiInfo.bssid)
            }

            return json
        }

        @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
        private fun getSimInfo(context: Context): JSONObject {
            val json = JSONObject()
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList<SubscriptionInfo>()

            val simArray = mutableListOf<JSONObject>()
            for (subInfo in activeSubscriptions) {
                val simJson = JSONObject()
                simJson.put("Carrier", subInfo.carrierName?.toString() ?: "Unknown")
                simJson.put("SIM Slot", subInfo.simSlotIndex)
                simJson.put("Signal Strength", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telephonyManager.signalStrength?.level ?: "Unknown"
                } else {
                    "Not Available"
                })
                simArray.add(simJson)
            }

            json.put("SIMs", simArray)
            return json
        }

        private fun getVolumeState(context: Context): JSONObject {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return JSONObject().apply {
                put("Media Volume", audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
                put("Call Volume", audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL))
                put("Ring Volume", audioManager.getStreamVolume(AudioManager.STREAM_RING))
                put("Notification Volume", audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION))
                put("Alarm Volume", audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
                put("Vibrate Mode", if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) 1 else 0)
                put("Silent Mode", if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) 1 else 0)
            }
        }

        private fun getDeviceInfo(): JSONObject {
            return JSONObject().apply {
                put("Device Name", Build.MANUFACTURER + " " + Build.MODEL)
                put("Device Model", Build.MODEL)
            }
        }

        private fun getBatteryLevel(context: Context): Int {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        }

        private fun isDoNotDisturbEnabled(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }

        // Public method to get all device states as a JSON object
        @RequiresPermission(allOf = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_WIFI_STATE", "android.permission.READ_PHONE_STATE"])
        fun getDeviceState(context: Context): JSONObject {
            return JSONObject().apply {
                put("WiFi Info", getWifiInfo(context))
                put("SIM Info", getSimInfo(context))
                put("Volume State", getVolumeState(context))
                put("Device Info", getDeviceInfo())
                put("Battery Level", getBatteryLevel(context))
                put("Do Not Disturb", isDoNotDisturbEnabled(context))
            }
        }
    }
}

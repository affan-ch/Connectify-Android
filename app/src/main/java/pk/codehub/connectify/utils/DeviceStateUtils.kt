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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume

@Serializable
data class WifiDetails(
    val ssid: String? = null,
    val bssid: String? = null
)

@Serializable
data class SimInfo(
    val carrier: String,
    val simSlot: Int,
    val signalStrength: String
)


@Serializable
data class VolumeState(
    val mediaVolume: String,
    val callVolume: String,
    val ringVolume: String,
    val notificationVolume: String,
    val alarmVolume: String,
    val vibrateMode: Boolean,
    val silentMode: Boolean
)

@Serializable
data class DeviceInfo(
    val deviceName: String,
    val deviceModel: String
)

@Serializable
data class DeviceState(
    val wifiDetails: WifiDetails?,
    val simInfo: List<SimInfo>,
    val volumeState: VolumeState,
    val deviceInfo: DeviceInfo,
    val batteryLevel: Int,
    val isBatteryCharging: Boolean = false,
    val networkType: String,
    val doNotDisturb: Boolean
)


class DeviceStateUtils private constructor() {

    companion object {

        @RequiresPermission(allOf = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_WIFI_STATE"])
        suspend fun getWifiInfo(context: Context): WifiDetails? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                suspendCancellableCoroutine { cont ->
                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()

                    val callback = object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                            val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                            val result = wifiInfo?.let {
                                WifiDetails(
                                    ssid = it.ssid.replace("\"", ""),
                                    bssid = it.bssid
                                )
                            }
                            cont.resume(result)
                            connectivityManager.unregisterNetworkCallback(this)
                        }

                        override fun onUnavailable() {
                            cont.resume(null)
                            connectivityManager.unregisterNetworkCallback(this)
                        }

                        override fun onLost(network: Network) {
                            // Optional: handle lost network if needed
                        }
                    }

                    connectivityManager.requestNetwork(request, callback)

                    cont.invokeOnCancellation {
                        connectivityManager.unregisterNetworkCallback(callback)
                    }
                }
            } else {
                // For below Android 10, synchronous retrieval
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                WifiDetails(
                    ssid = wifiInfo.ssid.replace("\"", ""),
                    bssid = wifiInfo.bssid
                )
            }
        }

        @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
        fun getSimInfo(context: Context): List<SimInfo> {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList<SubscriptionInfo>()

            return activeSubscriptions.map { subInfo ->
                val signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telephonyManager.signalStrength?.level?.toString() ?: "Unknown"
                } else {
                    "Not Available"
                }

                SimInfo(
                    carrier = subInfo.carrierName?.toString() ?: "Unknown",
                    simSlot = subInfo.simSlotIndex,
                    signalStrength = signalStrength
                )
            }
        }

        private fun getVolumeState(context: Context): VolumeState {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            fun getVolumeString(stream: Int): String {
                val current = audioManager.getStreamVolume(stream)
                val max = audioManager.getStreamMaxVolume(stream)
                return "$current/$max"
            }

            return VolumeState(
                mediaVolume = getVolumeString(AudioManager.STREAM_MUSIC),
                callVolume = getVolumeString(AudioManager.STREAM_VOICE_CALL),
                ringVolume = getVolumeString(AudioManager.STREAM_RING),
                notificationVolume = getVolumeString(AudioManager.STREAM_NOTIFICATION),
                alarmVolume = getVolumeString(AudioManager.STREAM_ALARM),
                vibrateMode = audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE,
                silentMode = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
            )
        }

        private fun getDeviceInfo(): DeviceInfo {
            val name = "${Build.MANUFACTURER} ${Build.MODEL}"
            val model = Build.MODEL
            return DeviceInfo(deviceName = name, deviceModel = model)
        }

        private fun getBatteryLevel(context: Context): Int {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        }

        private fun isBatteryCharging(context: Context): Boolean {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        }

        private fun getNetworkType(context: Context): String {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return "none"
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "none"

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
                else -> "unknown"
            }
        }

        private fun isDoNotDisturbEnabled(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        }

        // Public method to get all device states as a JSON object
        @RequiresPermission(allOf = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_WIFI_STATE", "android.permission.READ_PHONE_STATE"])
        suspend fun getDeviceState(context: Context): DeviceState {
            val wifi = getWifiInfo(context) // suspend
            val sim = getSimInfo(context)   // list of SimInfo
            val volume = getVolumeState(context)
            val device = getDeviceInfo()
            val battery = getBatteryLevel(context)
            val isCharging = isBatteryCharging(context)
            val networkType = getNetworkType(context)
            val dnd = isDoNotDisturbEnabled(context)


            return DeviceState(
                wifiDetails = wifi,
                simInfo = sim,
                volumeState = volume,
                deviceInfo = device,
                batteryLevel = battery,
                isBatteryCharging = isCharging,
                networkType = networkType,
                doNotDisturb = dnd
            )
        }
    }
}

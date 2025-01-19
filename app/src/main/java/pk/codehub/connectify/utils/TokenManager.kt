package pk.codehub.connectify.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import pk.codehub.connectify.models.Device


class TokenManager {
    companion object {
        suspend fun verifyLoginToken(context: Context, token: String): Boolean {

            val responseData = withContext(Dispatchers.IO) {
                val client = OkHttpClient()

                val request = Request.Builder()
                    .url(ApiRoutes.GET_USER_DETAILS)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", token)
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    response.body?.string()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val toastMessage = responseData ?: "No response from server"
            val toastDisplayMessage: String
            val success: Boolean
            val isOtpVerified: Boolean

            try {
                val jsonObject = JSONObject(toastMessage)
                toastDisplayMessage = jsonObject.optString("message", "No message found")

                success = jsonObject.optBoolean("success", false)
                isOtpVerified = jsonObject.optBoolean("isOtpVerified", false)

                Toast.makeText(context, toastDisplayMessage, Toast.LENGTH_SHORT).show()

                if (success && isOtpVerified) {
                    return true
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
        }

        suspend fun verifyDeviceToken(
            context: Context,
            token: String,
            deviceToken: String
        ): Boolean {

            val responseData = withContext(Dispatchers.IO) {
                val client = OkHttpClient()

                val request = Request.Builder()
                    .url(ApiRoutes.GET_ALL_DEVICES)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", token)
                    .addHeader("Device-Authorization", deviceToken)
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    response.body?.string()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val message: String
            val success: Boolean
            val devices: List<Device>
            val deviceId: String

            try {
                val jsonObject = JSONObject(responseData ?: "No response from server")
                message = jsonObject.optString("message", "No message found")
                success = jsonObject.optBoolean("success", false)
                deviceId = jsonObject.optString("deviceId", "")
                DataStoreManager.saveValue(context, "deviceId", deviceId)

                val devicesJsonArray = jsonObject.optJSONArray("devices")

                devices = if (devicesJsonArray != null) {
                    (0 until devicesJsonArray.length()).map { index ->
                        val deviceJson = devicesJsonArray.getJSONObject(index)
                        Device(
                            id = deviceJson.optString("id"),
                            userId = deviceJson.optString("userId"),
                            deviceType = deviceJson.optString("deviceType"),
                            deviceName = deviceJson.optString("deviceName"),
                            model = deviceJson.optString("model"),
                            osName = deviceJson.optString("osName"),
                            osVersion = deviceJson.optString("osVersion"),
                            deviceUuid = deviceJson.optString("deviceUuid"),
                            serialNumber = deviceJson.optString("serialNumber"),
                            boardId = deviceJson.optString("boardId"),
                            timezone = deviceJson.optString("timezone"),
                            manufacturer = deviceJson.optString("manufacturer"),
                            createdAt = deviceJson.optString("createdAt"),
                            updatedAt = deviceJson.optString("updatedAt")
                        )
                    }
                } else {
                    emptyList()
                }

                Log.d("TokenManager", "Devices: $devices")

                devicesJsonArray?.toString()
                    ?.let { DataStoreManager.saveValue(context, "devices", it) }

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                if (success) {
                    return true
                }
                else{
                    DataStoreManager.saveValue(context, "devices", "")
                    DataStoreManager.saveValue(context, "deviceToken", "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
        }


        suspend fun registerDevice(context: Context, token: String): Boolean {

            val info = DeviceInfoProvider.exportAsJson(context)

            val responseData = withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val mediaType = "application/json".toMediaType()

                val body = info.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(ApiRoutes.REGISTER_DEVICE)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", token)
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    response.body?.string()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val message: String
            val success: Boolean
            val deviceToken: String

            try {
                val jsonObject = JSONObject(responseData ?: "No response from server")
                message = jsonObject.optString("message", "No message found")
                success = jsonObject.optBoolean("success", false)
                deviceToken = jsonObject.optString("deviceToken", "No device token found")

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                if (success) {
                    DataStoreManager.saveValue(context, "deviceToken", deviceToken)
                    return true
                }
                else{
                    return false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

    }
}
package pk.codehub.connectify.viewmodels

import android.content.Context
import pk.codehub.connectify.utils.DataStoreManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import pk.codehub.connectify.utils.ApiRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import pk.codehub.connectify.utils.TokenManager


class TfaVerifyViewModel : ViewModel() {
    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun onOtpChange(newEmail: String) {
        _otp.value = newEmail
    }

    fun tfaVerify(context: Context, navController: NavController) {
        _isLoading.value = true

        Log.d("TfaVerifyViewModel", "Otp: ${_otp.value}")

        viewModelScope.launch {
            val token = DataStoreManager.getValue(context, "token", "").first()
            Log.d("TfaVerifyViewModel", "Token: $token")
            val responseData = withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val mediaType = "application/json".toMediaType()

                val jsonObject = JSONObject().apply {
                    put("otp", _otp.value)
                }

                val body = jsonObject.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(ApiRoutes.VERIFY_TFA)
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

            val toastMessage = responseData ?: "No response from server"
            var toastDisplayMessage = "No response from server"
            var success = false
            var newToken = ""

            try {
                val jsonObject = JSONObject(toastMessage)
                toastDisplayMessage = jsonObject.optString("message", "No message found")
                success = jsonObject.optBoolean("success", false)
                newToken = jsonObject.optString("token", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Toast.makeText(context, toastDisplayMessage, Toast.LENGTH_SHORT).show()

            _otp.value = ""
            _isLoading.value = false

            if (success) {
                DataStoreManager.saveValue(context, "token", newToken)
                TokenManager.registerDevice(context, newToken)
                TokenManager.verifyDeviceToken(context, newToken, DataStoreManager.getValue(context, "deviceToken", "").first())
                navController.navigate("home")
            }
        }
    }

}
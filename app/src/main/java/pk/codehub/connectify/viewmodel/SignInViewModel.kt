package pk.codehub.connectify.viewmodel

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class SignInViewModel : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun signIn(context: Context, navController: NavController) {
        _isLoading.value = true

        Log.d("SignInViewModel", "Email: ${_email.value}, Password: ${_password.value}")

        viewModelScope.launch {
            val responseData = withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val mediaType = "application/json".toMediaType()

                val jsonObject = JSONObject().apply {
                    put("email", _email.value)
                    put("password", _password.value)
                }

                val body = jsonObject.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(ApiRoutes.LOGIN)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
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
            var token: String? = null

            try {
                val jsonObject = JSONObject(toastMessage)
                toastDisplayMessage = jsonObject.optString("message", "No message found")
                token = jsonObject.optString("token", null.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }


            Toast.makeText(context, toastDisplayMessage, Toast.LENGTH_SHORT).show()

            _email.value = ""
            _password.value = ""
            _isLoading.value = false

            if (toastDisplayMessage == "User Login successfully!" && token != null) {
                DataStoreManager.saveValue(context, "token", token)
                navController.navigate("home")
            }
        }
    }

}
package pk.codehub.connectify.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import pk.codehub.connectify.ApiRoutes
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SignUpViewModel : ViewModel() {
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun signUp(context: Context, navController: NavController) {
        _isLoading.value = true

        Log.d("SignUpViewModel", "Name: ${_name.value}, Email: ${_email.value}, Password: ${_password.value}")

        viewModelScope.launch {
            val responseData = withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val mediaType = "application/json".toMediaType()

                val jsonObject = JSONObject().apply {
                    put("name", _name.value)
                    put("email", _email.value)
                    put("password", _password.value)
                }

                val body = jsonObject.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(ApiRoutes.REGISTER)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    response.body?.string()
                }
                catch(e: Exception){
                    e.printStackTrace()
                    null
                }
            }

            val toastMessage = responseData ?: "No response from server"
            val toastDisplayMessage = try {
                val jsonObject = JSONObject(toastMessage)
                jsonObject.optString("message", "No message found") // Default to a fallback message
            } catch (e: Exception) {
                e.printStackTrace()
                "No response from server"
            }
            Toast.makeText(context, toastDisplayMessage, Toast.LENGTH_SHORT).show()

            _name.value = ""
            _email.value = ""
            _password.value = ""
            _isLoading.value = false

            if(toastDisplayMessage == "User registered successfully"){
                navController.navigate("sign_in")
            }
        }

    }
}
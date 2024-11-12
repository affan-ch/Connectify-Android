package pk.codehub.connectify.utils

object ApiRoutes {
    private const val BASE_URL = "http://192.168.100.238:8000"
    const val REGISTER = "$BASE_URL/user/register"
    const val LOGIN = "$BASE_URL/user/login"
    const val VERIFY_TFA = "$BASE_URL/user/verify_otp"
}
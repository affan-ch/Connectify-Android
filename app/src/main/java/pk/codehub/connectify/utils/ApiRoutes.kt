package pk.codehub.connectify.utils

object ApiRoutes {
    private const val BASE_URL = "http://192.168.100.224:8000"
    const val REGISTER = "$BASE_URL/user/register"
    const val LOGIN = "$BASE_URL/user/login"
}
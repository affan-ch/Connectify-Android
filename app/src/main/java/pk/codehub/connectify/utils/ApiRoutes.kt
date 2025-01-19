package pk.codehub.connectify.utils

object ApiRoutes {
    const val BASE_URL = "https://connectify.affan.com.pk"
    const val REGISTER = "$BASE_URL/user/register"
    const val LOGIN = "$BASE_URL/user/login"
    const val VERIFY_TFA = "$BASE_URL/user/verify_otp"
    const val GET_USER_DETAILS = "$BASE_URL/user/details"
    const val REGISTER_DEVICE = "$BASE_URL/device/add"
    const val GET_ALL_DEVICES = "$BASE_URL/device/getAll"
}
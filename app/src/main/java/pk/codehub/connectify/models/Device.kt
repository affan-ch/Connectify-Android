package pk.codehub.connectify.models

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val id: String,
    val userId: String,
    val deviceType: String,
    val deviceName: String,
    val model: String,
    val osName: String,
    val osVersion: String,
    val deviceUuid: String,
    val serialNumber: String,
    val boardId: String,
    val timezone: String,
    val manufacturer: String,
    val createdAt: String,
    val updatedAt: String
)
package com.chitui.client.data.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ServerSettings(
    val autoDiscover: Boolean,
    val defaultPrinter: String?,
    val sessionTimeout: Int
)

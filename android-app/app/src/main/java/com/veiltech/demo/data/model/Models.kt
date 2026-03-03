package com.veiltech.demo.data.model

import java.time.LocalDateTime

data class RegisterRequest(val name: String, val phone: String, val passwordHash: String)
data class LoginRequest(val phone: String, val passwordHash: String)
data class AuthResponse(val token: String, val userId: Long, val name: String)
data class UserDto(val id: Long, val name: String, val phone: String)
data class ConnectionRequestCreate(val senderId: Long, val receiverId: Long)
data class AcceptRequest(val requestId: Long)
data class RequestView(val id: Long, val senderId: Long, val receiverId: Long, val status: String, val expiryTime: String)
data class VerifyPinRequest(val sessionId: Long, val pinHash: String)
data class VerifyPinResponse(val success: Boolean, val message: String, val attemptsLeft: Int)

package com.veiltech.demo.data.repository

import com.veiltech.demo.data.api.ApiService
import com.veiltech.demo.data.model.*

class VeilRepository(private val api: ApiService) {
    suspend fun register(name: String, phone: String, passwordHash: String) = api.register(RegisterRequest(name, phone, passwordHash))
    suspend fun login(phone: String, passwordHash: String) = api.login(LoginRequest(phone, passwordHash))
    suspend fun users() = api.users()
    suspend fun requests(userId: Long) = api.requests(userId)
    suspend fun connect(senderId: Long, receiverId: Long) = api.createRequest(ConnectionRequestCreate(senderId, receiverId))
    suspend fun accept(requestId: Long) = api.accept(AcceptRequest(requestId))
    suspend fun verifyPin(sessionId: Long, pinHash: String) = api.verifyPin(VerifyPinRequest(sessionId, pinHash))
}

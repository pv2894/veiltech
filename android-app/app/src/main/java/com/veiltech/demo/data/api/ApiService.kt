package com.veiltech.demo.data.api

import com.veiltech.demo.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    @POST("register") suspend fun register(@Body req: RegisterRequest): AuthResponse
    @POST("login") suspend fun login(@Body req: LoginRequest): AuthResponse
    @GET("users") suspend fun users(): List<UserDto>
    @POST("request") suspend fun createRequest(@Body req: ConnectionRequestCreate): RequestView
    @POST("accept") suspend fun accept(@Body req: AcceptRequest): RequestView
    @GET("requests/{userId}") suspend fun requests(@Path("userId") userId: Long): List<RequestView>

    @Multipart
    @POST("upload")
    suspend fun upload(
        @Part("requestId") requestId: RequestBody,
        @Part("pinHash") pinHash: RequestBody,
        @Part("expiryTime") expiry: RequestBody,
        @Part masked: MultipartBody.Part,
        @Part encryptedActual: MultipartBody.Part
    )

    @POST("verifyPin") suspend fun verifyPin(@Body req: VerifyPinRequest): VerifyPinResponse
}

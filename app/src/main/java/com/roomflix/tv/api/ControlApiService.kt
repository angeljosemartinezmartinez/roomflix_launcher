package com.roomflix.tv.api

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.Response
import java.util.concurrent.TimeUnit

data class ControlApiResponse(
    val success: Boolean,
    val message: String?,
    val duration: Float?
)

data class PingResponse(
    val success: Boolean,
    val ip: String?
)

interface ControlApiEndpoints {

    @POST("api/v1/devices/{id}/clear-data")
    suspend fun clearData(
        @Header("Authorization") token: String,
        @Path("id") deviceId: String,
        @Body body: okhttp3.RequestBody
    ): Response<ControlApiResponse>

    @POST("api/v1/devices/{id}/ping")
    suspend fun ping(
        @Header("Authorization") token: String,
        @Path("id") deviceId: String
    ): Response<PingResponse>

    @POST("api/v1/devices/{id}/reboot")
    suspend fun reboot(
        @Header("Authorization") token: String,
        @Path("id") deviceId: String
    ): Response<ControlApiResponse>
}

object ControlApiService {

    private var cachedInstance: ControlApiEndpoints? = null
    private var cachedBaseUrl: String? = null

    fun create(baseUrl: String): ControlApiEndpoints {
        if (cachedInstance != null && cachedBaseUrl == baseUrl) {
            return cachedInstance!!
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        cachedInstance = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ControlApiEndpoints::class.java)

        cachedBaseUrl = baseUrl
        return cachedInstance!!
    }
}

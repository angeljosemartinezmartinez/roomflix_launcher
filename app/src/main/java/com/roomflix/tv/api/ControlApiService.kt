package com.roomflix.tv.api

import com.roomflix.tv.vpn.VpnManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

data class ControlApiResponse(
    val success: Boolean,
    val message: String?,
    val duration: Float?
)

data class PingResponse(
    val success: Boolean,
    val ip: String?
)

data class PingRequest(
    val remoteConnected: Boolean = false,
    val remoteNames: List<String> = emptyList(),
    val remoteDaysSinceActivity: Int = -1
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
        @Path("id") deviceId: String,
        @Body request: PingRequest = PingRequest()
    ): Response<PingResponse>

    @POST("api/v1/devices/{id}/reboot")
    suspend fun reboot(
        @Header("Authorization") token: String,
        @Path("id") deviceId: String
    ): Response<ControlApiResponse>
}

object ControlApiService {

    // VPN activa: trafico por tunel WireGuard
    private const val VPN_BASE_URL = "http://${VpnManager.CONTROL_VPN_IP}/"
    // Fallback: dominio publico
    private const val DOMAIN_BASE_URL = "https://control.roomflix.tv/"

    private var cachedVpn: ControlApiEndpoints? = null
    private var cachedDomain: ControlApiEndpoints? = null

    fun create(vpnConnected: Boolean): ControlApiEndpoints {
        return if (vpnConnected) getVpnInstance() else getDomainInstance()
    }

    private fun getVpnInstance(): ControlApiEndpoints {
        if (cachedVpn != null) return cachedVpn!!
        cachedVpn = buildClient(VPN_BASE_URL)
        return cachedVpn!!
    }

    private fun getDomainInstance(): ControlApiEndpoints {
        if (cachedDomain != null) return cachedDomain!!
        cachedDomain = buildClient(DOMAIN_BASE_URL)
        return cachedDomain!!
    }

    private fun buildClient(baseUrl: String): ControlApiEndpoints {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ControlApiEndpoints::class.java)
    }
}

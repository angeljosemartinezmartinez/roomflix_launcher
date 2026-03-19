package com.roomflix.tv.network.service

import com.roomflix.tv.network.response.ResponseAllInfo
import com.roomflix.tv.network.response.ResponseChannels
import com.roomflix.tv.network.response.ResponseLanguages
import com.roomflix.tv.network.response.ResponseTemplates
import com.roomflix.tv.network.response.ResponseUpdate
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * Service - Interfaz Retrofit para las llamadas a la API de Roomflix
 * 
 * Todas las URLs apuntan a: https://panel.hotelplay.tv/api/main/{deviceId}
 * El deviceId se obtiene mediante DeviceIdProvider y se inyecta mediante DeviceIdInterceptor
 */
interface Service {

    @Headers("Cache-Control: max-age=40")
    @GET("/api/main/{mac}")
    fun getData(@Path("mac") id: String): Call<ResponseAllInfo>

    @GET("/api/launcher/{mac}")
    fun getUpdate(@Path("mac") mac: String): Call<ResponseUpdate>

    /**
     * Obtiene la lista de canales de TV en formato M3U playlist (texto plano)
     * URL: https://panel.hotelplay.tv/api/tv/{deviceId}
     * 
     * Devuelve un archivo de texto M3U que debe ser parseado manualmente
     * Formato esperado:
     * #EXTM3U
     * #EXTINF:-1 tvg-logo="http://example.com/logo.png",Canal Name
     * http://example.com/stream.mpd
     */
    @GET("/api/tv/{mac}")
    fun getChannels(@Path("mac") deviceId: String): retrofit2.Call<String>
}

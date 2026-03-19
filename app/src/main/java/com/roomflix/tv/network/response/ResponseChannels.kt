package com.roomflix.tv.network.response

import com.google.gson.annotations.SerializedName

/**
 * ResponseChannels - Modelo de respuesta para la lista de canales de TV
 * 
 * Endpoint: https://panel.hotelplay.tv/api/tv/{deviceId}
 * 
 * Ejemplo de respuesta JSON esperada:
 * {
 *   "channels": [
 *     {
 *       "id": 1,
 *       "name": "Canal 1",
 *       "url": "http://example.com/stream.m3u8",
 *       "logo": "http://example.com/logo.png"
 *     }
 *   ]
 * }
 */
data class ResponseChannels(
    @SerializedName("channels")
    val channels: List<Channel>? = emptyList()
)

/**
 * Channel - Modelo de un canal individual
 * 
 * Usado tanto para respuestas JSON como para canales parseados desde M3U
 */
data class Channel(
    @SerializedName("id")
    val id: Int = 0,  // ID opcional (para M3U se genera automáticamente)
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("url")
    val url: String,  // URL del video (.mpd para DASH o .m3u8 para HLS)
    
    @SerializedName("logo")
    val logo: String? = null,  // URL del logo del canal (opcional)
    
    @SerializedName("tvg-id")
    val tvgId: String? = null,  // ID del canal para EPG (extraído de M3U tvg-id="...")

    @SerializedName("group-title")
    val groupTitle: String? = null  // País/Categoría (extraído de M3U group-title="...")
)

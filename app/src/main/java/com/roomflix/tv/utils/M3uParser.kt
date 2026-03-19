package com.roomflix.tv.utils

import android.util.Log
import com.roomflix.tv.network.response.Channel

/**
 * M3uParser - Utilidad para parsear archivos M3U
 * 
 * Extrae información de canales desde formato M3U estándar:
 * - #EXTINF con metadatos (tvg-logo, tvg-id, nombre)
 * - URL del stream en la siguiente línea
 */
object M3uParser {
    
    private const val TAG = "M3uParser"
    private val GROUP_TITLE_REGEX = Regex("group-title=\"([^\"]+)\"")
    
    /**
     * Parsea contenido M3U y extrae canales
     * 
     * @param m3uContent Contenido del archivo M3U como String
     * @return Lista de canales parseados
     */
    fun parse(m3uContent: String): List<Channel> {
        val channelList = mutableListOf<Channel>()
        val lines = m3uContent.lines()
        var currentChannel: Channel? = null
        var channelIndex = 0
        
        lines.forEach { line ->
            val trimmedLine = line.trim()
            
            // Ignorar líneas vacías o comentarios que no sean #EXTINF
            if (trimmedLine.isEmpty() || (trimmedLine.startsWith("#") && !trimmedLine.startsWith("#EXTINF"))) {
                return@forEach
            }
            
            if (trimmedLine.startsWith("#EXTINF")) {
                // Extraer información del canal desde #EXTINF
                val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(trimmedLine)
                val tvgIdMatch = Regex("tvg-id=\"([^\"]+)\"").find(trimmedLine)
                val groupTitleMatch = GROUP_TITLE_REGEX.find(trimmedLine)
                val nameMatch = Regex(",(.+)$").find(trimmedLine)
                
                val logo = logoMatch?.groupValues?.get(1)
                val tvgId = tvgIdMatch?.groupValues?.get(1)
                val groupTitle = groupTitleMatch?.groupValues?.get(1)?.trim()
                val name = nameMatch?.groupValues?.get(1)?.trim() ?: "Canal $channelIndex"
                
                currentChannel = Channel(
                    id = channelIndex++,
                    name = name,
                    url = "", // Se llenará con la siguiente línea
                    logo = logo,
                    tvgId = tvgId,
                    groupTitle = groupTitle
                )
            } else if (trimmedLine.startsWith("http")) {
                // URL del canal (siguiente línea después de #EXTINF)
                currentChannel?.let { channelToUse ->
                    val channel = channelToUse.copy(url = trimmedLine)
                    channelList.add(channel)
                    currentChannel = null
                }
            }
        }
        
        Log.d(TAG, "Parseados ${channelList.size} canales del M3U")
        return channelList
    }
}

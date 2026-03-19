package com.roomflix.tv.repository

import android.util.Log
import com.roomflix.tv.network.response.Channel
import java.util.concurrent.CopyOnWriteArrayList

/**
 * ChannelsRepository - Singleton para almacenar canales pre-cargados
 * 
 * Almacena la lista de canales cargada desde SplashActivity
 * para acceso rápido sin necesidad de parsear M3U en PlayerActivity
 */
object ChannelsRepository {
    
    private const val TAG = "ChannelsRepository"
    
    // Lista thread-safe para almacenar canales
    private val channels: CopyOnWriteArrayList<Channel> = CopyOnWriteArrayList()
    
    /**
     * Almacena los canales pre-cargados
     */
    fun setChannels(channelsList: List<Channel>) {
        channels.clear()
        channels.addAll(channelsList)
        Log.d(TAG, "${channelsList.size} canales almacenados en repositorio")
    }
    
    /**
     * Obtiene todos los canales
     */
    fun getChannels(): List<Channel> = channels.toList()
    
    /**
     * Obtiene un canal por índice
     * @param index Índice del canal (0-based)
     * @return Canal en el índice especificado o null si el índice es inválido
     */
    fun getChannelByIndex(index: Int): Channel? {
        return if (index >= 0 && index < channels.size) {
            channels[index]
        } else {
            null
        }
    }
    
    /**
     * Obtiene el total de canales disponibles
     * @return Número total de canales
     */
    fun getTotalChannels(): Int = channels.size
    
    /**
     * Busca un canal por URL
     * @param url URL del canal a buscar
     * @return Canal con la URL especificada o null si no se encuentra
     */
    fun findChannelByUrl(url: String): Channel? {
        return channels.firstOrNull { it.url == url }
    }
    
    /**
     * Busca el índice de un canal por URL
     * @param url URL del canal a buscar
     * @return Índice del canal o -1 si no se encuentra
     */
    fun findChannelIndexByUrl(url: String): Int {
        return channels.indexOfFirst { it.url == url }
    }
    
    /**
     * Verifica si hay canales cargados
     * @return true si hay canales disponibles, false en caso contrario
     */
    fun hasChannels(): Boolean = channels.isNotEmpty()
    
    /**
     * Limpia todos los canales (útil para testing o reset)
     */
    fun clear() {
        channels.clear()
        Log.d(TAG, "Canales limpiados del repositorio")
    }
}

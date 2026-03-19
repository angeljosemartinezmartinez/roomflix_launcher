package com.roomflix.tv.repository

import android.util.Log
import com.roomflix.tv.epg.EpgProgram
import com.roomflix.tv.network.response.Channel
import com.roomflix.tv.network.response.ResponseAllInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * EpgRepository - Singleton para almacenar EPG pre-procesado
 * 
 * Almacena los datos de EPG ya parseados y procesados desde SplashActivity
 * para acceso rápido sin necesidad de parsear en MainMenu o PlayerActivity
 */
object EpgRepository {
    
    private const val TAG = "EpgRepository"
    
    // Variables estáticas para acceso directo desde cualquier parte de la app
    // Son públicas y se asignan directamente: EpgRepository.channels = list
    var channels: MutableList<Channel> = CopyOnWriteArrayList()
    var epgData: HashMap<String, List<EpgProgram>> = HashMap()
    var mainConfig: ResponseAllInfo? = null
    
    // Map thread-safe interno para operaciones (se sincroniza con epgData)
    private val epgDataInternal: ConcurrentHashMap<String, MutableList<EpgProgram>> = ConcurrentHashMap()
    
    /**
     * Almacena los datos de EPG pre-procesados
     * Mantiene lógica adicional para sincronizar con epgDataInternal
     */
    fun setEpgData(data: Map<String, MutableList<EpgProgram>>) {
        epgDataInternal.clear()
        epgDataInternal.putAll(data)
        epgData.clear()
        epgData.putAll(data)
        Log.d(TAG, "EPG almacenado: ${data.size} canales, ${data.values.sumOf { it.size }} programas totales")
    }
    
    /**
     * Obtiene los programas de un canal específico
     */
    fun getPrograms(channelId: String?): List<EpgProgram> {
        if (channelId == null) return emptyList()
        return epgData[channelId] ?: epgDataInternal[channelId] ?: emptyList()
    }
    
    /**
     * Obtiene el programa actual de un canal
     */
    fun getCurrentProgram(channelId: String?, channelName: String?): EpgProgram? {
        val programs = getPrograms(channelId ?: channelName ?: return null)
        val currentTime = System.currentTimeMillis()
        
        return programs.firstOrNull { program ->
            currentTime >= program.start && currentTime < program.end
        }
    }
    
    /**
     * Obtiene el siguiente programa de un canal
     */
    fun getNextProgram(channelId: String?, channelName: String?): EpgProgram? {
        val programs = getPrograms(channelId ?: channelName ?: return null)
        val currentTime = System.currentTimeMillis()
        
        return programs.firstOrNull { program ->
            program.start > currentTime
        }
    }
    
    /**
     * Verifica si hay datos cargados
     */
    fun hasData(): Boolean = epgData.isNotEmpty() || epgDataInternal.isNotEmpty()
    
    /**
     * Verifica si hay canales cargados
     */
    fun hasChannels(): Boolean = channels.isNotEmpty()
    
    /**
     * Limpia todos los datos (útil para testing o reset)
     */
    fun clear() {
        epgData.clear()
        epgDataInternal.clear()
        channels.clear()
        mainConfig = null
        Log.d(TAG, "EpgRepository limpiado completamente")
    }
}

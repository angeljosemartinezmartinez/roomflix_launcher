package com.roomflix.tv.compose.epg

import com.roomflix.tv.epg.EpgProgram

/**
 * ChannelWithPrograms - Canal con sus programas EPG asociados
 */
data class ChannelWithPrograms(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val tvgId: String?,
    val url: String,
    val programs: List<Program>
)

/**
 * Program - Programa de TV con información de estado y catchup
 */
data class Program(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isLive: Boolean,
    val catchupUrl: String? = null,
    val description: String = ""
) {
    /**
     * Determina si el programa está en emisión ahora (Live)
     */
    fun isCurrentlyLive(currentTime: Long): Boolean {
        return currentTime >= startTime && currentTime < endTime
    }
    
    /**
     * Determina si el programa es pasado
     */
    fun isPast(currentTime: Long): Boolean {
        return currentTime >= endTime
    }
    
    /**
     * Determina si el programa es futuro
     */
    fun isFuture(currentTime: Long): Boolean {
        return currentTime < startTime
    }
}

/**
 * Convierte EpgProgram a Program
 */
fun EpgProgram.toProgram(currentTime: Long = System.currentTimeMillis()): Program {
    return Program(
        title = this.title,
        startTime = this.start,
        endTime = this.end,
        isLive = this.isCurrentlyLive(currentTime),
        catchupUrl = null, // TODO: Extraer catchupUrl del EPG si está disponible
        description = this.description
    )
}

/**
 * Verifica si un EpgProgram está en emisión ahora
 */
fun EpgProgram.isCurrentlyLive(currentTime: Long = System.currentTimeMillis()): Boolean {
    return currentTime >= this.start && currentTime < this.end
}

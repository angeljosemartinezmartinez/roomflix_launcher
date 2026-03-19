package com.roomflix.tv.epg

/**
 * EpgProgram - Modelo de un programa individual de la guía de programación (EPG)
 * 
 * Representa un programa de TV con su información de transmisión
 */
data class EpgProgram(
    val channelId: String,      // ID del canal (debe coincidir con tvg-id del M3U)
    val title: String,          // Título del programa
    val start: Long,            // Timestamp de inicio (milliseconds desde epoch)
    val end: Long,              // Timestamp de fin (milliseconds desde epoch)
    val description: String = "" // Descripción opcional del programa
)

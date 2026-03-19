package com.roomflix.tv.views.components

import androidx.compose.runtime.Composable
import androidx.media3.exoplayer.ExoPlayer
import com.roomflix.tv.network.response.Channel
import com.roomflix.tv.compose.epg.PlayerEPGScreenContent
import com.roomflix.tv.compose.epg.PlayerEPGTheme

/**
 * EpgViewContainer - Contenedor encapsulado para la UI de la guía de canales (EPG)
 * 
 * TODO: Re-activar en v1.1
 * Este componente encapsula toda la lógica de UI de la EPG para mantener
 * PlayerActivity limpio y desacoplado.
 */
object EpgViewContainer {
    
    /**
     * Muestra la EPG con todos sus componentes
     * 
     * TODO: Re-activar en v1.1
     * 
     * @param exoPlayer Reproductor ExoPlayer asociado
     * @param channels Lista de canales disponibles
     * @param currentPlayingChannel Canal actualmente reproduciéndose
     * @param onChannelFocused Callback cuando un canal recibe foco
     * @param onNavigateToPlayer Callback cuando el usuario selecciona un canal para reproducir
     */
    @Composable
    fun show(
        exoPlayer: ExoPlayer?,
        channels: List<Channel>,
        currentPlayingChannel: Channel?,
        onChannelFocused: (Channel) -> Unit,
        onNavigateToPlayer: (Channel) -> Unit
    ) {
        // TODO: Re-activar en v1.1 - Implementar lógica completa de EPG
        PlayerEPGTheme {
            PlayerEPGScreenContent(
                exoPlayer = exoPlayer,
                channels = channels,
                currentPlayingChannel = currentPlayingChannel,
                onChannelFocused = onChannelFocused,
                onNavigateToPlayer = onNavigateToPlayer
            )
        }
    }
}

package com.roomflix.tv.views.activities

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.exoplayer.ExoPlayer

/**
 * ExoPlayerLifecycleObserver - Observer del ciclo de vida para ExoPlayer
 * 
 * Gestiona automáticamente el ciclo de vida del reproductor:
 * - onPause: Pausa el reproductor
 * - onStop: Detiene el reproductor
 * - onDestroy: Libera los recursos del reproductor
 * 
 * Uso:
 * ```kotlin
 * val player = ExoPlayer.Builder(context).build()
 * val observer = ExoPlayerLifecycleObserver(player)
 * lifecycle.addObserver(observer)
 * ```
 */
class ExoPlayerLifecycleObserver(
    private val player: ExoPlayer?
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "ExoPlayerLifecycleObserver"
    }
    
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        player?.pause()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        player?.stop()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        player?.release()
    }
}

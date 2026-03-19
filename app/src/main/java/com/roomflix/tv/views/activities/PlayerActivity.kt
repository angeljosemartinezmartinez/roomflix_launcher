@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.roomflix.tv.views.activities

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
// EPG DESACTIVADA: Imports comentados
// import com.roomflix.tv.compose.epg.PlayerEPGScreenContent
import com.roomflix.tv.compose.epg.PlayerEPGTheme
import com.roomflix.tv.repository.ChannelsRepository
import com.roomflix.tv.repository.EpgRepository
import com.roomflix.tv.network.response.Channel
import com.roomflix.tv.network.DeviceIdProvider
import com.roomflix.tv.network.service.ApiPro
import com.roomflix.tv.network.service.Service
import com.roomflix.tv.utils.M3uParser
import com.roomflix.tv.Constants
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlayerActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url" // Para compatibilidad con MainMenu.kt
    }

    /** Referencia al player creado en Compose; usada en onPause para pausar. */
    private var player: ExoPlayer? = null
    /** URL del Intent (EXTRA_VIDEO_URL) para que el Composable cargue al iniciar. */
    private val initialVideoUrlState = mutableStateOf<String?>(null)

    fun bindPlayer(p: ExoPlayer?) { player = p }
    private val channelsState = mutableStateOf<List<Channel>>(emptyList())
    private val currentPlayingChannelState = mutableStateOf<Channel?>(null)
    private var currentChannelIndex: Int = 0
    // Usar repositorios singleton para acceder a datos pre-cargados desde SplashActivity
    // NO hacer llamadas de red o parseo pesado aquí
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    // TODO: Re-activar en v1.1 - Job para rastrear y cancelar procesos de EPG en segundo plano
    private var epgBackgroundJob: Job? = null
    // Handler para operaciones en el hilo principal
    private val mainHandler = Handler(Looper.getMainLooper())
    // TODO: Re-activar en v1.1 - Estado para controlar la visibilidad del EPG
    // VIDEO PRIMERO: Iniciar en false para mostrar video a pantalla completa al entrar
    private val isEpgVisibleState = mutableStateOf(false)
    // Estado para controlar la visibilidad de los controles del reproductor (HUD)
    private val isPlayerControlsVisibleState = mutableStateOf(false)
    // Bandera para saber si es la primera vez que se muestra el HUD
    private var isFirstTimeShowingHUD = true
    // Indicador "LIVE" temporal al pulsar centro/arriba (estilo MainMenu)
    private val showLiveIndicatorState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_DITHER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }
        overridePendingTransition(0, 0)
        initialVideoUrlState.value = intent.getStringExtra(EXTRA_VIDEO_URL)

        setContent {
            val currentPlayingChannel by currentPlayingChannelState
            val isPlayerControlsVisible by isPlayerControlsVisibleState
            val context = LocalContext.current
            val initialVideoUrl by initialVideoUrlState

            BackHandler { finish() }

            PlayerEPGTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    ExoPlayerScreen(
                        context = context,
                        currentPlayingChannel = currentPlayingChannel,
                        initialVideoUrl = initialVideoUrl,
                        showLiveIndicatorState = showLiveIndicatorState,
                        onBindPlayer = ::bindPlayer
                    )
                    if (isPlayerControlsVisible && currentPlayingChannel != null) {
                        PlayerControlsHUD(
                            channel = currentPlayingChannel!!,
                            player = player,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // TODO: Re-activar en v1.1 - EPG encapsulada en EpgViewContainer
                // if (isEpgVisible) {
                //     EpgViewContainer.show(
                //         exoPlayer = player,
                //         channels = channels,
                //         currentPlayingChannel = currentPlayingChannel,
                //         onChannelFocused = { channel ->
                //             // TODO: Re-activar en v1.1 - Callback cuando un canal recibe foco
                //             playCurrentChannel(channel, force = false)
                //         },
                //         onNavigateToPlayer = { channel ->
                //             // TODO: Re-activar en v1.1 - Callback cuando se selecciona un canal
                //             isEpgVisibleState.value = false
                //             showHUDTemporarily(Constants.UI.HUD_DISPLAY_DURATION_MS)
                //             playCurrentChannel(channel, force = true)
                //         }
                //     )
                // }
            }
        }

        if (initialVideoUrlState.value.isNullOrBlank()) {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(Constants.UI.PLAYER_INIT_DELAY_MS)
                loadChannelsFromRepository()
            }
        }
    }
    
    /**
     * Carga canales desde el repositorio singleton (pre-cargados en SplashActivity)
     * NO hace llamadas de red ni parseo pesado
     */
    private fun loadChannelsFromRepository() {
        // Intentar primero desde EpgRepository (acceso directo)
        val loadedChannels = if (EpgRepository.hasChannels()) {
            EpgRepository.channels.toList()
        } else {
            // Fallback a ChannelsRepository
            ChannelsRepository.getChannels()
        }
        
        if (loadedChannels.isNotEmpty()) {
            val sorted = sortChannelsForCurrentLanguage(loadedChannels)
            channelsState.value = sorted
            updatePlayerUIWithChannels(sorted)
            
            // VIDEO PRIMERO: Reproducir primer canal inmediatamente a pantalla completa
            val firstChannel = sorted[0]
            currentPlayingChannelState.value = firstChannel
            playCurrentChannel(firstChannel, force = true)
            
            // FEEDBACK VISUAL: Mostrar HUD al entrar por primera vez
            if (isFirstTimeShowingHUD) {
                isFirstTimeShowingHUD = false
                showHUDTemporarily(Constants.UI.HUD_DISPLAY_DURATION_MS)
            }
        } else {
            Log.e("PlayerActivity", "Error: Repositorio de canales vacío")
            // Fallback opcional: cargar de DB o volver a Splash
            loadChannelsFallback()
        }
    }

    /**
     * Orden inteligente por idioma:
     * - Dentro del grupo prioritario, respeta el orden original del M3U.
     * - El resto se agrupa por country/groupTitle y se ordena por nombre de grupo.
     * Se re-evalúa en cada entrada al Player (depende del idioma guardado en prefs).
     */
    private fun sortChannelsForCurrentLanguage(channels: List<Channel>): List<Channel> {
        val prefs = getSharedPreferences("hotelPlay", MODE_PRIVATE)
        val lang = (prefs.getString(Constants.SHARED_PREFERENCES.SELECTED_LANGUAGE_CODE, null)
            ?: prefs.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, "en"))
            ?.lowercase()
            ?: "en"

        val keywords: List<String> = when (lang) {
            "es" -> listOf("España")
            "en" -> listOf("United Kingdom", "USA")
            "de" -> listOf("Deutschland")
            "fr" -> listOf("France")
            else -> emptyList()
        }

        data class Indexed(val idx: Int, val ch: Channel)
        val indexed = channels.mapIndexed { i, ch -> Indexed(i, ch) }

        fun isPriority(ch: Channel): Boolean {
            val g = ch.groupTitle ?: return false
            return keywords.any { kw -> g.contains(kw, ignoreCase = true) }
        }

        val priority = indexed.filter { isPriority(it.ch) }.sortedBy { it.idx }
        val nonPriority = indexed.filterNot { isPriority(it.ch) }

        // Agrupar el resto por país (groupTitle); si no hay, agrupar como "Otros"
        val grouped = nonPriority.groupBy { it.ch.groupTitle?.takeIf { g -> g.isNotBlank() } ?: "Otros" }
        val restGroupsSorted = grouped.keys.sortedWith(String.CASE_INSENSITIVE_ORDER)
        val rest = restGroupsSorted.flatMap { key ->
            grouped[key].orEmpty().sortedBy { it.idx }.map { it.ch }
        }

        return priority.map { it.ch } + rest
    }
    
    /**
     * Actualiza la UI del reproductor con los canales cargados
     */
    private fun updatePlayerUIWithChannels(channels: List<Channel>) {
        // La UI se actualiza automáticamente porque channelsState es un mutableStateOf
        // que está siendo observado en el setContent de Compose
    }
    
    /**
     * Fallback: Carga canales desde red solo si el repositorio está vacío
     * Esto NO debería ejecutarse normalmente si SplashActivity funcionó correctamente
     * CORRECCIÓN: NO hacer llamadas a api/tv si los datos ya están en el repositorio
     */
    private fun loadChannelsFallback() {
        // Verificar una vez más si hay datos en el repositorio antes de hacer llamada de red
        if (EpgRepository.hasChannels() || ChannelsRepository.hasChannels()) {
            loadChannelsFromRepository()
            return
        }
        
        Log.w("PlayerActivity", "Usando fallback: cargando canales desde red (último recurso)")
        val deviceId = DeviceIdProvider.getDeviceId(this)
        val service = ApiPro.createService(Service::class.java)
        
        service.getChannels(deviceId).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    val m3uContent = response.body() ?: ""
                    if (m3uContent.isNotBlank()) {
                        // OPTIMIZACIÓN: Parsear M3U en background thread usando M3uParser
                        lifecycleScope.launch(Dispatchers.IO) {
                            val parsedChannels = M3uParser.parse(m3uContent)
                            // Volver al hilo principal para actualizar UI
                            withContext(Dispatchers.Main) {
                                val sorted = sortChannelsForCurrentLanguage(parsedChannels)
                                channelsState.value = sorted
                                // Actualizar repositorios
                                ChannelsRepository.setChannels(sorted)
                                EpgRepository.channels.clear()
                                EpgRepository.channels.addAll(sorted)
                                
                                updatePlayerUIWithChannels(sorted)
                                
                                if (sorted.isNotEmpty()) {
                                    currentPlayingChannelState.value = sorted[0]
                                    playCurrentChannel(sorted[0], force = true)
                                }
                            }
                        }
                    }
                }
            }
            
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e("PlayerActivity", "Error de red al cargar canales (fallback)", t)
            }
        })
    }

    /**
     * Reproduce un canal: actualiza el estado; el Composable ExoPlayerScreen reacciona a urlToPlay y carga el MediaItem.
     */
    private fun playCurrentChannel(channel: Channel, force: Boolean = false) {
        val channels = channelsState.value
        val idx = channels.indexOfFirst { it.url == channel.url }.takeIf { it >= 0 } ?: currentChannelIndex
        if (!force && currentPlayingChannelState.value?.url == channel.url) {
            currentPlayingChannelState.value = channel
            currentChannelIndex = idx
            return
        }
        currentPlayingChannelState.value = channel
        currentChannelIndex = idx
        // El Composable ExoPlayerScreen observa currentPlayingChannelState.value?.url y carga el MediaItem
    }

    // GESTIÓN DEL MANDO (Zapping Circular)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish() // Siempre cerrar y volver al MainMenu
            return true
        }
        
        // TODO: Re-activar en v1.1 - Gestión de teclas cuando EPG está visible
        // if (isEpgVisibleState.value) {
        //     return super.onKeyDown(keyCode, event) // Dejar que la EPG maneje las teclas
        // }
        
        // Solo manejar zapping y controles cuando la EPG no está visible (reproductor a pantalla completa)
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                showLiveIndicatorState.value = true
                mainHandler.postDelayed({ showLiveIndicatorState.value = false }, 2500)
                val channels = channelsState.value
                if (channels.isNotEmpty()) {
                    val nextIndex = (currentChannelIndex + 1) % channels.size
                    navigateToChannel(nextIndex)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Zapping hacia abajo: canal anterior (circular)
                val channels = channelsState.value
                if (channels.isNotEmpty()) {
                    val prevIndex = if (currentChannelIndex <= 0) channels.size - 1 else currentChannelIndex - 1
                    navigateToChannel(prevIndex)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                showLiveIndicatorState.value = true
                mainHandler.postDelayed({ showLiveIndicatorState.value = false }, 2500)
                if (currentPlayingChannelState.value != null) {
                    showHUDTemporarily(Constants.UI.HUD_DISPLAY_DURATION_MS)
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    // TODO: Re-activar en v1.1 - EPG encapsulada
    // handleBackPress() y showEpg() eliminados en esta versión
    // BACK siempre cierra el reproductor directamente con finish()
    
    /**
     * Navega a un canal específico por índice y lo reproduce
     * TODO: Re-activar en v1.1 - Integrar con EPG para navegación visual
     * @param index Índice del canal en la lista de canales
     */
    private fun navigateToChannel(index: Int) {
        val channels = channelsState.value
        if (channels.isEmpty() || index < 0 || index >= channels.size) {
            Log.w("PlayerActivity", "Índice de canal inválido: $index (total canales: ${channels.size})")
            return
        }
        
        // Actualizar índice actual
        currentChannelIndex = index
        
        // Obtener el canal de la lista
        val newChannel = channels[index]
        
        // Reproducir el canal (forzar recarga)
        playCurrentChannel(newChannel, force = true)
        
        // Activar HUD y configurar auto-ocultación
        showHUDTemporarily(Constants.UI.HUD_DISPLAY_DURATION_MS)
    }
    
    /**
     * Muestra el HUD temporalmente durante el tiempo especificado
     * FEEDBACK VISUAL: Informa al usuario qué canal está viendo
     */
    private fun showHUDTemporarily(durationMs: Long) {
        isPlayerControlsVisibleState.value = true
        
        // Cancelar cualquier job previo de auto-ocultación
        epgBackgroundJob?.cancel()
        
        // Iniciar temporizador para ocultar HUD después del tiempo especificado
        epgBackgroundJob = coroutineScope.launch {
            delay(durationMs)
            isPlayerControlsVisibleState.value = false
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        finish()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }
}

/** Etiqueta Logcat para depuración de integración FFmpeg Audio. */
private const val ROOMFLIX_DEBUG_TAG = "RoomflixDebug"

/**
 * Comprueba si las librerías nativas FFmpeg (.so) están disponibles.
 * Usa reflexión para no requerir la dependencia media3-exoplayer-ffmpeg en tiempo de compilación.
 */
private fun checkFfmpegNativeLibraryLoaded(): Boolean {
    return try {
        val clazz = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegLibrary")
        val method = clazz.getMethod("isAvailable")
        @Suppress("UNCHECKED_CAST")
        (method.invoke(null) as? Boolean) == true
    } catch (e: ClassNotFoundException) {
        Log.w(ROOMFLIX_DEBUG_TAG, "FfmpegLibrary no encontrada (extensión FFmpeg no incluida en el build)")
        false
    } catch (e: Exception) {
        Log.e(ROOMFLIX_DEBUG_TAG, "Error al comprobar FfmpegLibrary.isAvailable()", e)
        false
    }
}

/**
 * Recorre las capacidades de los renderers del ExoPlayer y comprueba si hay un renderer de audio
 * cuyo nombre (decoder en uso) contenga "ffmpeg". El nombre exacto del decoder solo se conoce
 * cuando se dispara onAudioDecoderInitialized, por eso se pasa lastAudioDecoderName.
 */
private fun logRendererCapabilitiesAndFfmpegCheck(
    player: ExoPlayer,
    lastAudioDecoderName: String?
) {
    try {
        val countMethod = player.javaClass.getMethod("getRendererCount")
        val count = countMethod.invoke(player) as? Int ?: 0
        val typeMethod = player.javaClass.getMethod("getRendererType", Int::class.javaPrimitiveType)
        Log.d(ROOMFLIX_DEBUG_TAG, "Renderer count: $count")
        var hasFfmpegAudio = false
        for (i in 0 until count) {
            val type = typeMethod.invoke(player, i) as? Int ?: C.TRACK_TYPE_DEFAULT
            val typeStr = when (type) {
                C.TRACK_TYPE_VIDEO -> "VIDEO"
                C.TRACK_TYPE_AUDIO -> "AUDIO"
                C.TRACK_TYPE_TEXT -> "TEXT"
                C.TRACK_TYPE_METADATA -> "METADATA"
                C.TRACK_TYPE_CAMERA_MOTION -> "CAMERA_MOTION"
                C.TRACK_TYPE_NONE -> "NONE"
                else -> "TYPE_$type"
            }
            Log.d(ROOMFLIX_DEBUG_TAG, "  Renderer[$i] type=$typeStr")
            if (type == C.TRACK_TYPE_AUDIO && lastAudioDecoderName != null) {
                if (lastAudioDecoderName.contains("ffmpeg", ignoreCase = true)) {
                    hasFfmpegAudio = true
                }
            }
        }
        val decoderInfo = lastAudioDecoderName ?: "(decoder aún no inicializado)"
        Log.d(ROOMFLIX_DEBUG_TAG, "Último decoder de audio conocido: $decoderInfo")
        Log.d(
            ROOMFLIX_DEBUG_TAG,
            if (hasFfmpegAudio) "FFmpeg Audio en uso (evita c2.android.aac.decoder / Buffer Ownership)"
            else "Decoder de audio NO es FFmpeg. Si persiste 'Buffer Ownership', añade media3-exoplayer-ffmpeg y EXTENSION_RENDERER_MODE_PREFER."
        )
    } catch (e: NoSuchMethodException) {
        Log.d(ROOMFLIX_DEBUG_TAG, "getRendererCount/getRendererType no disponibles en esta versión; decoder en uso: ${lastAudioDecoderName ?: "desconocido"}")
        Log.d(
            ROOMFLIX_DEBUG_TAG,
            if (lastAudioDecoderName?.contains("ffmpeg", ignoreCase = true) == true)
                "Decoder de audio es FFmpeg."
            else
                "Decoder de audio NO es FFmpeg: $lastAudioDecoderName"
        )
    } catch (e: Exception) {
        Log.e(ROOMFLIX_DEBUG_TAG, "Error al listar renderers", e)
    }
}

/**
 * Verifica qué decoders FFmpeg tiene realmente la app: clases en el classpath,
 * librerías nativas (.so) por ABI y si isAvailable() devuelve true.
 * Útil cuando Jellyfin (u otra extensión) no registra decoders con ExoPlayer.
 */
private fun logFfmpegDecoderVerification(context: android.content.Context) {
    Log.d(ROOMFLIX_DEBUG_TAG, "========== Verificación de decoders FFmpeg ==========")
    Log.d(ROOMFLIX_DEBUG_TAG, "ABI: ${Build.CPU_ABI}, ABI2: ${Build.CPU_ABI2}, SUPPORTED_ABIS: ${Build.SUPPORTED_ABIS?.joinToString()}")
    val libraryClassNames = listOf(
        "androidx.media3.decoder.ffmpeg.FfmpegLibrary",
        "org.jellyfin.mediadecoder.ffmpeg.FfmpegLibrary",
        "org.jellyfin.media3.decoder.ffmpeg.FfmpegLibrary"
    )
    for (className in libraryClassNames) {
        try {
            val clazz = Class.forName(className)
            Log.d(ROOMFLIX_DEBUG_TAG, "Clase encontrada: $className")
            try {
                val isAvailableMethod = clazz.getMethod("isAvailable")
                val available = isAvailableMethod.invoke(null) as? Boolean
                Log.d(ROOMFLIX_DEBUG_TAG, "  isAvailable() = $available")
            } catch (e: NoSuchMethodException) {
                Log.d(ROOMFLIX_DEBUG_TAG, "  isAvailable() no existe")
            }
            try {
                val getVersionMethod = clazz.getMethod("getVersion")
                val version = getVersionMethod.invoke(null) as? String
                Log.d(ROOMFLIX_DEBUG_TAG, "  getVersion() = $version")
            } catch (e: NoSuchMethodException) {
                // opcional
            }
        } catch (e: ClassNotFoundException) {
            Log.d(ROOMFLIX_DEBUG_TAG, "Clase NO encontrada: $className")
        } catch (e: Exception) {
            Log.e(ROOMFLIX_DEBUG_TAG, "Error al inspeccionar $className", e)
        }
    }
    val rendererClassNames = listOf(
        "androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer",
        "org.jellyfin.mediadecoder.ffmpeg.FfmpegAudioRenderer",
        "org.jellyfin.media3.decoder.ffmpeg.FfmpegAudioRenderer"
    )
    Log.d(ROOMFLIX_DEBUG_TAG, "--- Renderers de audio FFmpeg en classpath ---")
    for (className in rendererClassNames) {
        try {
            Class.forName(className)
            Log.d(ROOMFLIX_DEBUG_TAG, "  Encontrado: $className")
        } catch (e: ClassNotFoundException) {
            Log.d(ROOMFLIX_DEBUG_TAG, "  No encontrado: $className")
        }
    }
    val nativeLibDir = context.applicationInfo.nativeLibraryDir
    try {
        val dir = java.io.File(nativeLibDir)
        if (dir.exists() && dir.isDirectory) {
            val soFiles = dir.listFiles()?.filter { it.name.endsWith(".so") }?.map { it.name } ?: emptyList()
            val ffmpegRelated = soFiles.filter { name ->
                name.contains("ffmpeg", ignoreCase = true) ||
                name.contains("avcodec", ignoreCase = true) ||
                name.contains("avformat", ignoreCase = true) ||
                name.contains("avutil", ignoreCase = true)
            }
            Log.d(ROOMFLIX_DEBUG_TAG, "Librerías nativas (.so) en $nativeLibDir: ${soFiles.size} total")
            if (ffmpegRelated.isEmpty()) {
                Log.w(ROOMFLIX_DEBUG_TAG, "Ninguna .so de FFmpeg/avcodec/avformat en el APK para esta ABI. La extensión puede no incluir esta arquitectura.")
            } else {
                Log.d(ROOMFLIX_DEBUG_TAG, "FFmpeg/avcodec .so: ${ffmpegRelated.joinToString()}")
            }
        } else {
            Log.d(ROOMFLIX_DEBUG_TAG, "nativeLibraryDir no accesible: $nativeLibDir")
        }
    } catch (e: Exception) {
        Log.e(ROOMFLIX_DEBUG_TAG, "Error al listar .so", e)
    }
    Log.d(ROOMFLIX_DEBUG_TAG, "========== Fin verificación FFmpeg ==========")
}

/**
 * Pantalla del reproductor: ExoPlayer creado con remember, ciclo de vida con DisposableEffect,
 * AndroidView con PlayerView Media3. Experiencia 100% inmersiva en pantalla completa, sin textos superpuestos.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ExoPlayerScreen(
    context: android.content.Context,
    currentPlayingChannel: Channel?,
    initialVideoUrl: String?,
    @Suppress("UNUSED_PARAMETER") showLiveIndicatorState: MutableState<Boolean>,
    onBindPlayer: (ExoPlayer?) -> Unit
) {
    val exoPlayer = remember(context) {
        // Usamos el RenderersFactory por defecto (Hardware siempre primero)
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        // LoadControl estándar pero con un pequeño ajuste amigable para IPTV
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBuffer
                50000, // maxBuffer
                1500,  // bufferForPlayback
                3000   // bufferForPlaybackAfterRebuffer
            ).build()

        val player = androidx.media3.exoplayer.ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()

        player.playWhenReady = true

        // Listener para reconectar si hay cortes de red
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("RoomflixPlayer", "Error: ${error.errorCodeName}")
                // Intentar recuperar el directo automáticamente
                player.prepare()
                player.play()
            }
        })

        player
    }

    DisposableEffect(exoPlayer) {
        val analyticsListener = object : AnalyticsListener {
            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                Log.d("RoomflixDebug", "AUDIODECODER: Usando $decoderName")
            }

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                Log.d("RoomflixDebug", "VIDEODECODER: Usando $decoderName")
            }
        }
        exoPlayer.addAnalyticsListener(analyticsListener)
        onDispose { exoPlayer.removeAnalyticsListener(analyticsListener) }
    }

    DisposableEffect(Unit) {
        onBindPlayer(exoPlayer)
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
            onBindPlayer(null)
        }
    }

    val urlToPlay = currentPlayingChannel?.url ?: initialVideoUrl

    // No tocar el surface: solo stop/clearMediaItems/setMediaItem/prepare (evita vídeo negro en Amlogic)
    LaunchedEffect(urlToPlay) {
        urlToPlay?.let { url ->
            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            delay(150)

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    player = exoPlayer
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Mapea la resolución del stream (ancho x alto) a una etiqueta de calidad para la UI.
 * Rangos típicos: 4K (≥2160), Full HD (≥1080), HD (≥720), SD (≥576), 480p (≥480).
 */
private fun resolutionToQualityLabel(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "—"
    val maxSide = maxOf(width, height)
    return when {
        maxSide >= 2160 -> "4K"
        maxSide >= 1080 -> "Full HD"
        maxSide >= 720 -> "HD"
        maxSide >= 576 -> "SD"
        maxSide >= 480 -> "480p"
        else -> "SD"
    }
}

/**
 * HUD de controles del reproductor: Reconstruido según diseño de referencia
 * Muestra información del canal con gradiente vertical, número de canal, logo, programa e iconos técnicos
 */
@Composable
private fun PlayerControlsHUD(
    channel: Channel,
    @Suppress("UNUSED_PARAMETER") player: ExoPlayer?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    // Usar EpgRepository en lugar de EpgManager para acceso rápido sin parseo
    // Obtener programa actual del canal
    val channelId = if (channel.tvgId.isNullOrBlank()) null else channel.tvgId
    val channelName = channel.name
    val currentProgram = remember(channel.tvgId, channel.name) {
        EpgRepository.getCurrentProgram(channelId, channelName)
    }
    
    // Obtener siguiente programa para mostrar "Próximo: "
    val nextProgram = remember(channel.tvgId, channel.name) {
        EpgRepository.getNextProgram(channelId, channelName)
    }
    
    // Estado para tiempo actual (se actualiza cada segundo)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }
    
    // Calcular progreso del programa si está en directo
    val progress = if (currentProgram != null && currentTime >= currentProgram.start && currentTime < currentProgram.end) {
        val totalDuration = currentProgram.end - currentProgram.start
        val elapsed = currentTime - currentProgram.start
        ((elapsed.toFloat() / totalDuration) * 100).coerceIn(0f, 100f)
    } else {
        0f
    }
    
    // Calcular tiempo restante
    val remainingMinutes = if (currentProgram != null && currentTime >= currentProgram.start && currentTime < currentProgram.end) {
        val remainingMs = currentProgram.end - currentTime
        (remainingMs / (60 * 1000)).toInt()
    } else {
        0
    }
    
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startTime = currentProgram?.let { timeFormat.format(Date(it.start)) } ?: ""
    val endTime = currentProgram?.let { timeFormat.format(Date(it.end)) } ?: ""
    
    // Calidad de video según resolución real del stream (Player.getVideoSize())
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose { }
        val p = player!!
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }
        }
        p.addListener(listener)
        videoWidth = p.videoSize.width
        videoHeight = p.videoSize.height
        onDispose { p.removeListener(listener) }
    }
    val videoQuality = resolutionToQualityLabel(videoWidth, videoHeight)
    
    // Detectar calidad de audio (simplificado)
    val audioQuality = remember(channel.url) {
        when {
            channel.url.contains("dolby", ignoreCase = true) || channel.url.contains("ac3", ignoreCase = true) -> "Dolby"
            else -> "Estéreo"
        }
    }
    
    Box(
        modifier = modifier.zIndex(10f), // Z-Index alto para estar sobre el video
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.3f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Variable de espaciado simétrico constante
            val horizontalMargin = 24.dp
            
            // ROW PRINCIPAL: Estructura simétrica (Logo | Margin | Bloque Central | Margin | Iconos)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ROW IZQUIERDA: Número de canal + Logo (horizontalmente)
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // NÚMERO DE CANAL (izquierda, reducido 20%: de 41.sp a 33.sp) con sombra
                    val textShadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                    androidx.compose.material3.Text(
                        text = "${channel.id + 1}",
                        fontSize = 33.sp, // Reducido 20% desde 41.sp
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = textShadow)
                    )
                    
                    // LOGO DEL CANAL (derecha del número, aumentado 15% adicional: de 87.dp a 100.dp) sin sombra
                    if (!channel.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(channel.logo)
                                .crossfade(true)
                                .build(),
                            contentDescription = channel.name,
                            modifier = Modifier
                                .size(100.dp) // Aumentado 15% adicional desde 87.dp para mayor peso visual
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Icono por defecto si no hay logo (sin sombra)
                        Box(
                            modifier = Modifier
                                .size(100.dp) // Aumentado 15% adicional desde 87.dp para mayor peso visual
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "📺",
                                fontSize = 52.sp // Ajustado proporcionalmente
                            )
                        }
                    }
                }
                
                // Spacer simétrico izquierdo: entre Logo y Bloque Central
                Spacer(modifier = Modifier.width(horizontalMargin))
                
                // BLOQUE CENTRAL: Información del programa (con weight para evitar solapamiento)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    // ROW 1: Título del programa (sin iconos aquí, están fuera)
                    val textShadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                    androidx.compose.material3.Text(
                        text = currentProgram?.title ?: channel.name,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = textShadow)
                    )
                        
                    // ROW 2: Rating/Descripción (UNA SOLA VEZ) con sombra
                    if (currentProgram != null && currentProgram.description.isNotBlank()) {
                        val descriptionShadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                        androidx.compose.material3.Text(
                            text = currentProgram.description,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp,
                            softWrap = true, // Salto de línea automático al llegar al límite
                            modifier = Modifier.padding(top = 4.dp),
                            style = TextStyle(shadow = descriptionShadow)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // ROW 3: Hora Inicio-Fin + Barra de Progreso (100% ancho) + Minutos Restantes (UNA SOLA VEZ) con sombra
                    val timeShadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            text = "$startTime - $endTime",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal,
                            style = TextStyle(shadow = timeShadow)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Barra de progreso gris fija (100% del ancho del bloque central limitado)
                        if (progress > 0f) {
                            LinearProgressIndicator(
                                progress = progress / 100f,
                                modifier = Modifier
                                    .weight(1f) // Ocupa todo el espacio disponible hasta el margen
                                    .height(2.dp),
                                color = Color.Gray,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (remainingMinutes > 0) {
                            androidx.compose.material3.Text(
                                text = "$remainingMinutes min",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Normal,
                                style = TextStyle(shadow = timeShadow)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // ROW 4: Próximo programa (UNA SOLA VEZ) con sombra
                    if (nextProgram != null && nextProgram.title.isNotBlank()) {
                        val nextShadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Text(
                                text = "Próximo: ",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Normal,
                                style = TextStyle(shadow = nextShadow)
                            )
                            androidx.compose.material3.Text(
                                text = nextProgram.title,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(shadow = nextShadow)
                            )
                        }
                    }
                }
                
                // Spacer simétrico derecho: entre Bloque Central e Iconos Técnicos
                Spacer(modifier = Modifier.width(horizontalMargin))
                
                // ICONOS TÉCNICOS (HD, Estéreo, FPS) - Alineados verticalmente, con borde oscuro y offset vertical (sin sombras)
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .fillMaxHeight()
                        .offset(y = (-6).dp), // Desplazamiento hacia arriba para alineación visual
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.Black.copy(alpha = 0.4f), // Borde oscuro para destacar
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = videoQuality,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.Black.copy(alpha = 0.4f), // Borde oscuro para destacar
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = audioQuality,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.Black.copy(alpha = 0.4f), // Borde oscuro para destacar
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "25 FPS",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
            }
        }
    }
}

package com.roomflix.tv.compose.epg

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.roomflix.tv.epg.EpgManager
import com.roomflix.tv.epg.EpgProgram
import com.roomflix.tv.network.DeviceIdProvider
import com.roomflix.tv.network.response.Channel
import com.roomflix.tv.network.service.ApiPro
import com.roomflix.tv.network.service.Service
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * PlayerEPGViewModel - ViewModel para la pantalla EPG con reproductor
 * 
 * Gestiona:
 * - Estado del reproductor (canal actual, programas)
 * - Estado EPG (programas por canal, timeline)
 * - Debounce de reproducción (600ms)
 */
class PlayerEPGViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlayerEPGViewModel"
        private const val PLAYBACK_DEBOUNCE_MS = 600L // 600ms de debounce
    }

    // Estado del reproductor
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _currentProgram = MutableStateFlow<EpgProgram?>(null)
    val currentProgram: StateFlow<EpgProgram?> = _currentProgram.asStateFlow()

    private val _selectedProgram = MutableStateFlow<EpgProgram?>(null)
    val selectedProgram: StateFlow<EpgProgram?> = _selectedProgram.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // EPG Manager
    private val epgManager = EpgManager.getInstance()

    // Reproductor ExoPlayer
    var player: ExoPlayer? = null
        private set

    // Job para debounce de reproducción
    private var playbackJob: Job? = null

    init {
        loadChannels()
    }

    /**
     * Carga la lista de canales desde la API M3U
     */
    fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val deviceId = DeviceIdProvider.getDeviceId(getApplication())
            val service = ApiPro.createService(Service::class.java)
            val call = service.getChannels(deviceId)

            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        val m3uContent = response.body()
                        if (m3uContent.isNullOrBlank()) {
                            _error.value = "Lista de canales vacía"
                            _isLoading.value = false
                            return
                        }

                        val parsedChannels = parseM3U(m3uContent)
                        _channels.value = parsedChannels
                        _isLoading.value = false

                        // Descargar EPG después de cargar canales
                        loadEpg()

                        // Establecer primer canal como actual si hay canales
                        if (parsedChannels.isNotEmpty()) {
                            setCurrentChannel(parsedChannels[0])
                        }
                    } else {
                        _error.value = "Error al cargar canales: ${response.code()}"
                        _isLoading.value = false
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    _error.value = "Error de red: ${t.message}"
                    _isLoading.value = false
                    Log.e(TAG, "Error al cargar canales", t)
                }
            })
        }
    }

    /**
     * Descarga y parsea el EPG
     */
    private fun loadEpg() {
        viewModelScope.launch {
            try {
                epgManager.downloadEpg()?.let { xmlContent ->
                    epgManager.parseEpgXml(xmlContent)
                    Log.d(TAG, "EPG cargado correctamente")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar EPG: ${e.message}", e)
            }
        }
    }

    /**
     * Establece el canal actual con debounce de reproducción
     */
    fun setCurrentChannel(channel: Channel) {
        _currentChannel.value = channel

        // Cancelar job anterior si existe
        playbackJob?.cancel()

        // Obtener programa actual para este canal
        val program = epgManager.getCurrentProgram(channel.tvgId)
        _currentProgram.value = program

        // Debounce: esperar 600ms antes de reproducir
        playbackJob = viewModelScope.launch {
            delay(PLAYBACK_DEBOUNCE_MS)
            
            // Si el usuario no cambió de canal durante el delay, reproducir
            if (_currentChannel.value == channel && player != null) {
                playChannel(channel)
            }
        }
    }

    /**
     * Reproduce un canal en el reproductor
     */
    private fun playChannel(channel: Channel) {
        val exoPlayer = player ?: return

        try {
            val mediaItem = MediaItem.fromUri(channel.url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            Log.d(TAG, "Reproduciendo canal: ${channel.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al reproducir canal: ${e.message}", e)
            _error.value = "Error al reproducir canal"
        }
    }

    /**
     * Selecciona un programa de la EPG (para navegación)
     */
    fun selectProgram(program: EpgProgram) {
        _selectedProgram.value = program
    }
    
    /**
     * Obtiene los programas de un canal desde EpgManager
     */
    fun getProgramsForChannel(tvgId: String?): List<EpgProgram> {
        return epgManager.getProgramsForChannel(tvgId)
    }

    /**
     * Inicializa el reproductor ExoPlayer
     */
    fun initializePlayer(exoPlayer: ExoPlayer) {
        player = exoPlayer
        player?.let { p ->
            p.addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(TAG, "Error del reproductor: ${error.message}")
                    _error.value = "Error de reproducción: ${error.message}"
                }
            })
        }
    }

    /**
     * Libera el reproductor
     */
    fun releasePlayer() {
        playbackJob?.cancel()
        player?.release()
        player = null
    }

    /**
     * Parsea un archivo M3U playlist a lista de Channel
     */
    private fun parseM3U(m3uContent: String): List<Channel> {
        val channelList = mutableListOf<Channel>()
        val lines = m3uContent.lines()
        var currentChannel: Channel? = null
        var channelId = 0

        for (line in lines) {
            val trimmedLine = line.trim()

            if (trimmedLine.isEmpty() || (trimmedLine.startsWith("#") && !trimmedLine.startsWith("#EXTINF"))) {
                continue
            }

            if (trimmedLine.startsWith("#EXTINF")) {
                val logo = extractTvgLogo(trimmedLine)
                val tvgId = extractTvgId(trimmedLine)
                val name = extractChannelName(trimmedLine)

                currentChannel = Channel(
                    id = channelId++,
                    name = name,
                    url = "",
                    logo = logo,
                    tvgId = tvgId
                )
            } else if (!trimmedLine.startsWith("#") && trimmedLine.startsWith("http") && currentChannel != null) {
                val channelWithUrl = currentChannel.copy(url = trimmedLine)
                channelList.add(channelWithUrl)
                currentChannel = null
            }
        }

        return channelList
    }

    private fun extractTvgLogo(extinfLine: String): String? {
        val logoRegex = Regex("""tvg-logo="([^"]+)"""")
        return logoRegex.find(extinfLine)?.groupValues?.get(1)
    }

    private fun extractTvgId(extinfLine: String): String? {
        val tvgIdRegex = Regex("""tvg-id="([^"]+)"""")
        return tvgIdRegex.find(extinfLine)?.groupValues?.get(1)
    }

    private fun extractChannelName(extinfLine: String): String {
        val lastCommaIndex = extinfLine.lastIndexOf(',')
        return if (lastCommaIndex >= 0 && lastCommaIndex < extinfLine.length - 1) {
            extinfLine.substring(lastCommaIndex + 1).trim()
        } else {
            "Canal Desconocido"
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}

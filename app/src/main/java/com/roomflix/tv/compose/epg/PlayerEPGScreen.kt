package com.roomflix.tv.compose.epg

import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.ui.PlayerView
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.roomflix.tv.epg.EpgManager
import com.roomflix.tv.epg.EpgProgram
import com.roomflix.tv.network.response.Channel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * PlayerEPGScreen - Activity principal para la pantalla EPG con reproductor
 * 
 * Layout estilo Timeline EPG profesional:
 * - 35% superior: Video + Metadata del programa enfocado
 * - 65% inferior: Timeline EPG con canales y programas
 */

// Constante global: 8dp por minuto de duración del programa
// Esto garantiza que los programas se alineen perfectamente con las horas de la TimeRuler
private val DP_PER_MINUTE = 8.dp

// Offset de la línea roja que marca el presente (200.dp desde el borde izquierdo)
private val RED_LINE_OFFSET_DP = 200.dp

// Paleta de colores de la rejilla EPG
private val ProgramContainerColor = Color(0xFF333333) // Color de fondo de los bloques de programa
private val GridLineColor = Color(0xFF555555) // Color de las líneas de rejilla (más claro que el fondo)

class PlayerEPGScreen : ComponentActivity() {

    private val viewModel: PlayerEPGViewModel by viewModels()
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("PlayerEPGScreen", "onCreate: Iniciando pantalla EPG Timeline")
        
        // Configurar modo inmersivo para Android TV
        enableImmersiveMode()
        
        // Inicializar ExoPlayer
        initializePlayer()
        
        setContent {
            PlayerEPGTheme {
                val channels by viewModel.channels.collectAsState()
                PlayerEPGScreenContent(
                    exoPlayer = exoPlayer,
                    channels = channels
                )
            }
        }
    }

    private fun initializePlayer() {
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
        exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, true)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    val msg = error.message ?: ""
                    val isRendererOrDecoding = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
                            || error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                            || error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES
                            || error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                            || error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
                            || error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
                            || msg.contains("audio", ignoreCase = true) || msg.contains("AudioRenderer", ignoreCase = true)
                    if (isRendererOrDecoding) {
                        Log.w("PlayerEPGScreen", "ExoPlayer: posible rechazo de codec de audio (TYPE_RENDERER/decoding). errorCode=${error.errorCode}, message=$msg", error)
                    } else {
                        Log.e("PlayerEPGScreen", "ExoPlayer onPlayerError: errorCode=${error.errorCode}, message=$msg", error)
                    }
                }
            })
        }
        viewModel.initializePlayer(exoPlayer!!)
    }

    private fun enableImmersiveMode() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.post {
            val insetsController = window.insetsController
            insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}

/**
 * Tema oscuro para Roomflix EPG
 */
@Composable
fun PlayerEPGTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0F1116),
            surface = Color(0xFF1A1D24),
            primary = Color(0xFF2196F3),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerEPGScreenContent(
    exoPlayer: ExoPlayer?,
    channels: List<Channel>,
    onChannelFocused: (Channel) -> Unit = {},
    currentPlayingChannel: Channel? = null, // Canal que se está reproduciendo actualmente
    onNavigateToPlayer: (Channel) -> Unit = {} // Callback para navegar al reproductor a pantalla completa
) {
    // Estado para el programa enfocado (se actualiza cuando el foco cambia)
    var focusedProgram by remember { mutableStateOf<EpgProgram?>(null) }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    
    // Estado para forzar recarga de UI cuando el EPG se parsea
    var epgLoadAttempt by remember { mutableStateOf(0) }
    
    // Estado para carga diferida: mostrar 15 canales primero, luego todos
    var displayedChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    
    // Estado compartido para scroll horizontal sincronizado
    // Todas las filas y TimeRuler se desplazarán juntas usando un único ScrollState
    val sharedScrollState = rememberScrollState()
    
    // Calcular hora de inicio del grid: 00:00 de hoy como punto 0
    val gridStartTime = remember {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }
    
    // Calcular offset inicial para posicionar la hora actual en la línea roja (RED_LINE_OFFSET_DP)
    // El cálculo es: (currentTimeMinutes * DP_PER_MINUTE) - RED_LINE_OFFSET_DP
    val density = LocalDensity.current
    val initialScrollOffsetPx = remember {
        val currentTime = System.currentTimeMillis()
        val currentTimeMinutes = (currentTime - gridStartTime) / 60000 // Minutos desde medianoche
        // Cálculo: (currentTimeMinutes * DP_PER_MINUTE) - RED_LINE_OFFSET_DP
        val offsetDp = (currentTimeMinutes * DP_PER_MINUTE.value) - RED_LINE_OFFSET_DP.value
        with(density) {
            offsetDp.dp.toPx().toInt().coerceAtLeast(0) // No permitir scroll negativo
        }
    }
    
    // Inicializar scroll para que la hora actual quede alineada con la línea roja
    // Esto permite ver el pasado a la izquierda de la línea roja
    LaunchedEffect(Unit) {
        sharedScrollState.scrollTo(initialScrollOffsetPx)
    }
    
    // Usar singleton para acceder a los datos de EPG cargados desde MainMenu
    val epgManager = remember { EpgManager.getInstance() }
    
    val displayChannels = if (channels.isEmpty()) {
        List(5) { id -> Channel(id = id, name = "Canal de Prueba $id", url = "") }
    } else {
        channels
    }
    
    // FocusRequester para el primer programa del primer canal
    val firstItemFocusRequester = remember { FocusRequester() }
    
    // Carga en dos etapas: primero 15 canales para respuesta rápida, luego todos después de 2000ms
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            // a) Carga rápida inicial: mostrar solo 15 canales
            displayedChannels = channels.take(15)
            Log.d("EPG_LOAD", "Carga inicial: 15 canales mostrados")
            
            // b) Solicitar foco tras delay de 500ms para asegurar que los componentes estén listos
            delay(500)
            try {
                firstItemFocusRequester.requestFocus()
                Log.d("EPG_FOCUS", "Solicitando foco inicial en primer programa")
            } catch (e: IllegalStateException) {
                Log.e("EPG_FOCUS", "Error al solicitar foco inicial: ${e.message}")
            }
            
            // c) Etapa 3: Después de 1.5s, cargar todos los canales sin que el usuario note lag
            delay(1500)
            displayedChannels = channels
            Log.d("EPG_LOAD", "Carga completa: ${channels.size} canales mostrados")
        } else {
            // Si no hay canales, usar canales de prueba
            displayedChannels = displayChannels
        }
    }
    
    // Inicializar focusedProgram con el programa actual del primer canal al iniciar
    // Usar displayedChannels para que se actualize cuando se carga el primero
    // El panel DEBE mostrar automáticamente el programa actual del primer canal
    LaunchedEffect(displayedChannels) {
        if (displayedChannels.isNotEmpty()) {
            val firstChannel = displayedChannels[0]
            // Matching flexible: pasar tanto tvgId como name para buscar en ambos casos
            // Esto es útil porque los IDs en el caché pueden ser muy largos (ej: kjhsa8354...)
            val channelId = if (firstChannel.tvgId.isNullOrBlank()) null else firstChannel.tvgId
            val channelName = firstChannel.name // Siempre pasar el nombre como respaldo
            
            // Buscar programa actual o el primero si no hay programa actual
            val currentProgram = epgManager.getCurrentProgram(channelId, channelName)
            val programs = epgManager.getProgramsForChannel(channelId, channelName)
            val programToFocus = currentProgram ?: programs.firstOrNull()
            
            // Inicializar focusedProgram inmediatamente con datos disponibles
            // Esto asegura que el panel muestre el programa del primer canal al iniciar
            if (programToFocus != null) {
                focusedProgram = programToFocus
                focusedChannel = firstChannel
                Log.d("EPG_FOCUS", "Programa inicial establecido: ${programToFocus.title} (canal: ${firstChannel.name})")
            } else {
                Log.d("EPG_FOCUS", "No se encontró programa para el canal: ${firstChannel.name} (tvgId: ${channelId ?: "vacío"})")
            }
        }
    }
    
    // Recargar UI cuando el EPG se parsea (observar cambios en los canales y forzar recomposición)
    // Esto se activa cuando displayedChannels cambia o cuando el EPG se descarga
    LaunchedEffect(displayedChannels) {
        // Intentar varias veces para dar tiempo a que el EPG se descargue y parsee
        repeat(5) { attempt ->
            delay((1000 + (attempt * 500)).toLong()) // 1s, 1.5s, 2s, 2.5s, 3s
            if (displayedChannels.isNotEmpty()) {
                val firstChannel = displayedChannels[0]
                // Matching flexible: pasar tanto tvgId como name para buscar en ambos casos
                val channelId = if (firstChannel.tvgId.isNullOrBlank()) null else firstChannel.tvgId
                val channelName = firstChannel.name // Siempre pasar el nombre como respaldo
                val programs = epgManager.getProgramsForChannel(channelId, channelName)
                
                // Si encontramos programas, actualizar estado y forzar recarga
                if (programs.isNotEmpty()) {
                    epgLoadAttempt++ // Forzar recomposición de ChannelTimelineRow
                    
                    // Si focusedProgram está vacío, establecerlo
                    if (focusedProgram == null) {
                        val programToFocus = epgManager.getCurrentProgram(channelId, channelName) ?: programs.firstOrNull()
                        if (programToFocus != null) {
                            focusedProgram = programToFocus
                            focusedChannel = firstChannel
                            Log.d("EPG_FOCUS", "Programa establecido tras parseo EPG (intento ${attempt + 1}): ${programToFocus.title}")
                        }
                    }
                    
                    // Si ya encontramos programas, no necesitamos seguir intentando
                    return@LaunchedEffect
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
            .padding(top = 32.dp) // Padding superior aumentado a 32.dp para mejor separación
    ) {
        // 1. SECCIÓN SUPERIOR: Video + Metadata (42% de altura - aumentado 20%)
        TopSection(
            exoPlayer = exoPlayer,
            focusedProgram = focusedProgram,
            focusedChannel = focusedChannel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .padding(horizontal = 16.dp)
        )

        // Espacio entre video y EPG (32.dp - aumentado para mejor respiración)
        Spacer(modifier = Modifier.height(32.dp))

        // 2. SECCIÓN EPG: Row principal con columna de canales fija + área scrollable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // LADO IZQUIERDO: Columna de canales (ancho fijo 200.dp)
            Column(
                modifier = Modifier.width(200.dp)
            ) {
                // Texto superior: Fecha y hora actual encima de la columna de canales
                DateTimeHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp) // Misma altura que TimeRuler
                )
                
                // Lista de canales
                ChannelListColumn(
                    channels = displayedChannels,
                    currentPlayingChannel = currentPlayingChannel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // LADO DERECHO: Contenedor scrollable con TimeRuler y filas de programas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(sharedScrollState) // Scroll sincronizado único
                    ) {
                        // TimeRuler dentro del scroll
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            TimeRulerInScroll(
                                gridStartTime = gridStartTime,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Línea roja vertical en TimeRuler (X = RED_LINE_OFFSET_DP)
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .offset(x = RED_LINE_OFFSET_DP)
                                    .background(Color.Red.copy(alpha = 0.8f))
                            )
                        }
                        
                        // Column con todas las filas de programas
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ProgramsTimelineColumn(
                                channels = displayedChannels,
                                onChannelFocused = onChannelFocused,
                                onProgramFocused = { program, channel ->
                                    focusedProgram = program
                                    focusedChannel = channel
                                },
                                onNavigateToPlayer = onNavigateToPlayer,
                                firstItemFocusRequester = firstItemFocusRequester,
                                epgLoadAttempt = epgLoadAttempt,
                                gridStartTime = gridStartTime,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Línea roja vertical que cruza todas las filas de programas (X = RED_LINE_OFFSET_DP)
                            // Esta línea se mueve con el scroll horizontal
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .offset(x = RED_LINE_OFFSET_DP)
                                    .background(Color.Red.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header de fecha y hora: Muestra "domingo 18 enero" y hora actual
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DateTimeHeader(
    modifier: Modifier = Modifier
) {
    // Estado para la hora actual (se actualiza cada minuto)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Actualizar la hora cada minuto
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(60000)
        }
    }
    
    // Formatear fecha y hora en una sola línea: "HH:mm | EEEE d MMMM"
    val combinedFormat = remember {
        SimpleDateFormat("HH:mm | EEEE d MMMM", Locale.getDefault())
    }
    val dateTimeText = remember(currentTime) {
        combinedFormat.format(Date(currentTime))
    }
    
    Box(
        modifier = modifier
            .background(Color(0xFF1A1D24))
            .padding(horizontal = 8.dp, vertical = 4.dp), // Padding suficiente para no solaparse
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = dateTimeText,
            fontSize = 16.sp,
            color = Color(0xFF00B2FF), // Azul cian
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Columna de canales: Lista fija de canales con logos y nombres (ancho fijo 200.dp)
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelListColumn(
    channels: List<Channel>,
    currentPlayingChannel: Channel?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF0F1116))
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Lista de canales (sin Spacer, ya que DateTimeHeader está arriba)
        channels.forEach { channel ->
            ChannelInfoColumnWithPlay(
                channel = channel,
                isPlaying = currentPlayingChannel?.id == channel.id,
                modifier = Modifier.height(54.dp)
            )
        }
    }
}

/**
 * Columna de información del canal con icono de play: [Número] -> [Logo] -> [Nombre] -> [Play Icon]
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelInfoColumnWithPlay(
    channel: Channel,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número de canal (ajustado para columna de 210.dp - reducido 10%)
        Text(
            text = "${channel.id + 1}",
            fontSize = 18.sp, // Reducido de 20.sp a 18.sp para columna más estrecha
            color = Color.Gray,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(28.dp) // Reducido de 32.dp a 28.dp
        )
        
        // Logo del canal (28.dp, transparente - reducido para ajustar al nuevo ancho)
        Box(
            modifier = Modifier.size(28.dp), // Reducido de 30.dp a 28.dp
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(text = "📺", fontSize = 18.sp)
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Nombre del canal (14.sp para columna de 210.dp)
        Text(
            text = channel.name,
            fontSize = 14.sp, // 14.sp para que quepa bien en los 210.dp
            color = Color.White,
            fontWeight = FontWeight.Bold, // Negrita
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // Icono de play si es el canal que se está reproduciendo
        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproduciendo",
                tint = Color.Cyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * TimeRuler dentro del scroll: Barra de horas sin columna de canal
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimeRulerInScroll(
    gridStartTime: Long,
    modifier: Modifier = Modifier
) {
    // Estado para la hora actual (se actualiza cada minuto)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Actualizar la hora cada minuto
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(60000)
        }
    }
    
    // Generar horas desde 00:00 hasta 24:00 en intervalos de 30 minutos
    val timeSlots = remember(gridStartTime) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = gridStartTime
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        (0..47).map { index -> // 48 slots de 30 min = 24 horas
            val time = gridStartTime + (index * 30 * 60 * 1000L)
            timeFormat.format(Date(time))
        }
    }
    
    Box(
        modifier = modifier
            .background(Color(0xFF1A1D24))
            .padding(horizontal = 8.dp)
    ) {
        // Lista horizontal de horas
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(30.dp * DP_PER_MINUTE.value.toInt()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            timeSlots.forEach { timeSlot ->
                Text(
                    text = timeSlot,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        
        // Línea vertical fina para marcar la hora actual (alineada con RED_LINE_OFFSET_DP cuando está en el presente)
        // Esta línea se mueve con el scroll, pero cuando la hora actual coincide con RED_LINE_OFFSET_DP, se alineará perfectamente
        val currentLinePosition = remember(currentTime, gridStartTime) {
            val minutesSinceMidnight = (currentTime - gridStartTime) / (60 * 1000)
            (minutesSinceMidnight * DP_PER_MINUTE.value).dp
        }
        
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .offset(x = currentLinePosition - RED_LINE_OFFSET_DP) // Restar RED_LINE_OFFSET_DP para alinearse con la línea roja
                .background(Color(0xFF00AAFF).copy(alpha = 0.6f))
        )
    }
}

/**
 * Columna de filas de programas: Todas las filas alineadas verticalmente
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgramsTimelineColumn(
    channels: List<Channel>,
    onChannelFocused: (Channel) -> Unit,
    onProgramFocused: (EpgProgram, Channel) -> Unit,
    onNavigateToPlayer: (Channel) -> Unit,
    firstItemFocusRequester: FocusRequester,
    epgLoadAttempt: Int,
    gridStartTime: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        channels.forEachIndexed { channelIndex, channel ->
            ProgramTimelineRow(
                channel = channel,
                channelIndex = channelIndex,
                onChannelFocused = onChannelFocused,
                onProgramFocused = onProgramFocused,
                onNavigateToPlayer = onNavigateToPlayer,
                firstItemFocusRequester = if (channelIndex == 0) firstItemFocusRequester else null,
                epgLoadAttempt = epgLoadAttempt,
                gridStartTime = gridStartTime,
                modifier = Modifier.height(54.dp)
            )
            // Separador horizontal después de cada fila (excepto la última)
            if (channelIndex < channels.size - 1) {
                Divider(
                    color = GridLineColor,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Fila de programas para un canal: Usa offsets absolutos basados en 00:00 de hoy
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgramTimelineRow(
    channel: Channel,
    channelIndex: Int,
    onChannelFocused: (Channel) -> Unit,
    onProgramFocused: (EpgProgram, Channel) -> Unit,
    onNavigateToPlayer: (Channel) -> Unit,
    firstItemFocusRequester: FocusRequester?,
    epgLoadAttempt: Int,
    gridStartTime: Long,
    modifier: Modifier = Modifier
) {
    val epgManager = remember { EpgManager.getInstance() }
    val programs = remember(channel.tvgId, channel.name, epgLoadAttempt) {
        val channelId = if (channel.tvgId.isNullOrBlank()) null else channel.tvgId
        val channelName = channel.name
        val allPrograms = epgManager.getProgramsForChannel(channelId, channelName)
        // LOGS DE SEGURIDAD: Imprimir cantidad de programas ANTES de cualquier filtrado
        Log.d("EPG_DEBUG", "Canal: ${channel.name} - Programas totales obtenidos: ${allPrograms.size}")
        allPrograms.forEachIndexed { idx, prog ->
            val startDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(prog.start))
            val endDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(prog.end))
            Log.d("EPG_DEBUG", "  Programa[$idx]: ${prog.title} | $startDate - $endDate")
        }
        allPrograms
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1116))
    ) {
        if (programs.isEmpty()) {
            // OPTIMIZACIÓN: Un solo bloque de 24 horas con líneas verticales dibujadas con Canvas
            // Esto ahorra miles de componentes comparado con 48 bloques individuales
            var isFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = {
                    // Acción OK: navegar al reproductor a pantalla completa
                    onNavigateToPlayer(channel)
                },
                modifier = Modifier
                    .width((1440 * DP_PER_MINUTE.value).dp) // 24 horas = 1440 minutos
                    .fillMaxHeight()
                    .focusable() // CRÍTICO: permite navegación con D-Pad verticalmente
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onChannelFocused(channel)
                        }
                    }
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) Color(0xFF00B2FF) else GridLineColor,
                        shape = RoundedCornerShape(0.dp)
                    ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = ProgramContainerColor,
                    focusedContainerColor = Color(0xFF2A2D34)
                ),
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(0.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Dibujar líneas verticales cada 30 minutos usando Canvas (simula divisiones)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val lineSpacing = (30.dp * (DP_PER_MINUTE.value / 1.dp.value)).toPx() // 30 min * 8.dp en píxeles
                        val lineColor = GridLineColor // Usar GridLineColor para las líneas de rejilla
                        var x = lineSpacing
                        while (x < size.width) {
                            drawLine(
                                color = lineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx() // toPx() disponible en DrawScope
                            )
                            x += lineSpacing
                        }
                    }
                    
                    // Texto "Sin información" en el centro
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Sin información",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        } else {
            // Row con programas usando offsets absolutos desde 00:00 de hoy
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                val midnightTodayMillis = gridStartTime
                
                // Filtrar programas que ya terminaron antes del inicio del grid y calcular offsets
                val visiblePrograms = programs.filter { program ->
                    val endsAfterGridStart = program.end > midnightTodayMillis
                    if (!endsAfterGridStart) {
                        Log.d("EPG_DEBUG", "Ignorando programa '${program.title}' (terminó antes de medianoche)")
                    }
                    endsAfterGridStart
                }
                
                visiblePrograms.forEachIndexed { programIndex, program ->
                    // LÓGICA DE PROGRAMAS PASADOS (Clipping): Ajustar programas que empezaron antes del grid
                    val programStartTime = program.start
                    val programEndTime = program.end
                    
                    // Calcular el inicio visual del programa (clipping del horizonte)
                    // Usar maxOf para que los programas que empezaron antes de medianoche se dibujen desde el borde izquierdo
                    val visualStart = maxOf(programStartTime, midnightTodayMillis)
                    val visualEnd = programEndTime
                    
                    // Calcular xOffset usando visualStart y width usando (programEndTime - visualStart)
                    val xOffset = ((visualStart - midnightTodayMillis) / 60000.0) * DP_PER_MINUTE.value
                    val width = ((visualEnd - visualStart) / 60000.0) * DP_PER_MINUTE.value
                    
                    val programOffsetDp = xOffset.toFloat()
                    val programWidth = width.dp.coerceAtLeast(80.dp)
                    
                    // Calcular offset relativo desde el programa anterior para el Spacer
                    val previousProgramEndDp = if (programIndex > 0) {
                        val prevProgram = visiblePrograms[programIndex - 1]
                        val prevEnd = prevProgram.end
                        val prevEndVisible = maxOf(prevEnd, midnightTodayMillis)
                        ((prevEndVisible - midnightTodayMillis) / 60000.0 * DP_PER_MINUTE.value).toFloat()
                    } else {
                        0f
                    }
                    val gapOffset = (programOffsetDp - previousProgramEndDp).coerceAtLeast(0f)
                    
                    // Spacer para el gap (si hay) desde el programa anterior o desde gridStartTime
                    if (gapOffset > 0 || programIndex == 0) {
                        Spacer(modifier = Modifier.width((if (programIndex == 0) programOffsetDp else gapOffset).dp))
                    }
                    
                    TimelineProgramCard(
                        program = program,
                        channel = channel,
                        onProgramFocused = { onProgramFocused(program, channel) },
                        onChannelFocused = { onChannelFocused(channel) },
                        onNavigateToPlayer = { onNavigateToPlayer(channel) },
                        focusRequester = if (channelIndex == 0 && programIndex == 0) firstItemFocusRequester else null,
                        programWidth = programWidth,
                        modifier = Modifier // Sin offset, se maneja con Spacer
                    )
                }
            }
        }
    }
}

/**
 * Sección superior: Video (izquierda) + Metadata del programa (derecha)
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopSection(
    exoPlayer: ExoPlayer?,
    focusedProgram: EpgProgram?,
    focusedChannel: Channel?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Video a la izquierda (ratio 16:9, 20% más grande usando weight, esquinas rectas)
        Box(
            modifier = Modifier
                .weight(0.55f) // Aumentar proporción para hacer video 20% más grande
                .aspectRatio(16f / 9f)
                .background(Color.Black), // Sin RoundedCornerShape - esquinas rectas
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Cargando video...", color = Color.Gray)
            }
        }

        // Metadata del programa a la derecha
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            if (focusedProgram != null && focusedChannel != null) {
                ProgramMetadata(
                    program = focusedProgram,
                    channel = focusedChannel
                )
            } else if (focusedChannel != null) {
                // Cuando el foco está en un canal sin EPG, mostrar nombre del canal y mensaje
                Text(
                    text = focusedChannel.name,
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sin información de programación",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            } else {
                // Estado por defecto cuando no hay programa ni canal enfocado
                Text(
                    text = "Selecciona un programa",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Usa el D-Pad para navegar por la guía",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Metadata del programa: Título, Horario, Barra de progreso, Tiempo restante, Descripción, Próximo
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgramMetadata(
    program: EpgProgram,
    channel: Channel
) {
    // Usar singleton para acceder a los datos de EPG cargados desde MainMenu
    val epgManager = remember { EpgManager.getInstance() }
    
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startTime = remember(program.start) { timeFormat.format(Date(program.start)) }
    val endTime = remember(program.end) { timeFormat.format(Date(program.end)) }
    
    // Estado para tiempo actual (se actualiza para calcular progreso y tiempo restante)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Actualizar tiempo actual cada segundo para barra de progreso y tiempo restante
    LaunchedEffect(program.start, program.end) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000) // 1 segundo
        }
    }
    
    val isLive = currentTime >= program.start && currentTime < program.end
    val progress = if (isLive && program.end > program.start) {
        val totalDuration = program.end - program.start
        val elapsed = currentTime - program.start
        ((elapsed.toFloat() / totalDuration) * 100).coerceIn(0f, 100f)
    } else {
        0f
    }
    
    // Calcular tiempo restante en minutos
    val remainingMinutes = if (isLive && program.end > currentTime) {
        val remainingMs = program.end - currentTime
        (remainingMs / (60 * 1000)).toInt()
    } else {
        0
    }
    
    // Obtener siguiente programa (usar nombre como fallback si tvgId está vacío)
    val channelId = if (channel.tvgId.isNullOrBlank()) null else channel.tvgId
    val channelName = if (channelId == null) channel.name else null
    val nextProgram = remember(channel.tvgId, channel.name) {
        epgManager.getNextProgram(channelId, channelName)
    }
    
    val displayTitle = if (program.title.isBlank() || program.title == "Sin información") {
        channel.name
    } else {
        program.title
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. TÍTULO: MaterialTheme.typography.headlineMedium
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // 2. FILA DE TIEMPO: Horario + Barra de progreso + Tiempo restante
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Horario "HH:mm - HH:mm"
            Text(
                text = "$startTime - $endTime",
                fontSize = 16.sp,
                color = Color(0xFF00AAFF),
                fontWeight = FontWeight.Medium
            )
            
            // Barra de progreso (solo si está en directo)
            if (isLive && progress > 0f) {
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp),
                    color = Color.Red
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Tiempo restante "X min"
            if (isLive && remainingMinutes > 0) {
                Text(
                    text = "$remainingMinutes min",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        
        // 3. DESCRIPCIÓN: Máximo 3 líneas
        if (program.description.isNotBlank()) {
            Text(
                text = program.description,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
        
        // 4. PRÓXIMO: "Próximo: [Título del siguiente programa]"
        if (nextProgram != null && nextProgram.title.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Próximo:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = nextProgram.title,
                    fontSize = 14.sp,
                    color = Color(0xFF00AAFF),
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Regla de tiempo: Muestra la fecha/hora actual y horarios en intervalos de 30 min
 * Con scroll sincronizado y línea de hora actual
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimeRuler(
    scrollState: androidx.compose.foundation.ScrollState, // ScrollState compartido para sincronización
    modifier: Modifier = Modifier
) {
    // Estado para la hora actual (se actualiza cada minuto)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Actualizar la hora cada minuto
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(60000) // 1 minuto
        }
    }
    
    // Formatear fecha y hora actual
    val dateTimeFormat = remember {
        SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault())
    }
    val currentDateTime = remember(currentTime) {
        dateTimeFormat.format(Date(currentTime))
    }
    
    // Generar lista de horas en intervalos de 30 minutos (6 horas desde ahora)
    val timeSlots = remember(currentTime) {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentTime
        // Redondear hacia abajo al intervalo de 30 minutos más cercano
        val minutes = calendar.get(java.util.Calendar.MINUTE)
        calendar.set(java.util.Calendar.MINUTE, (minutes / 30) * 30)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        (0..12).map { index ->
            val time = calendar.timeInMillis + (index * 30 * 60 * 1000L)
            timeFormat.format(Date(time))
        }
    }
    
    Row(
        modifier = modifier
            .background(Color(0xFF1A1D24)),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // IZQUIERDA: Fecha y hora actual (ancho fijo 240.dp)
        Box(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = currentDateTime,
                fontSize = 12.sp, // Reducido de 14.sp a 12.sp para caber en 26.dp
                color = Color(0xFF00AAFF),
                fontWeight = FontWeight.Medium
            )
        }
        
        // Separador azul eliminado - transición limpia sin divisiones
        
        // DERECHA: Lista horizontal de horas (intervalos de 30 minutos) con scroll sincronizado
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState) // Scroll sincronizado con sharedScrollState
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp * DP_PER_MINUTE.value.toInt()), // Espaciado basado en DP_PER_MINUTE (30 min = 240.dp)
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeSlots.forEach { timeSlot ->
                    Text(
                        text = timeSlot,
                        fontSize = 11.sp, // Ligeramente reducido para espacio más compacto
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            // Línea vertical fina para marcar la hora actual (fija, no se mueve con scroll)
            // Posición: basada en minutos desde el inicio del slot actual
            val currentLinePosition = remember(currentTime) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentTime
                val minutes = calendar.get(Calendar.MINUTE)
                val roundedMinutes = (minutes / 30) * 30
                // Calcular minutos desde el inicio del slot actual (redondeado hacia abajo a 30 min)
                calendar.set(Calendar.MINUTE, roundedMinutes)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val slotStart = calendar.timeInMillis
                val minutesSinceSlot = (currentTime - slotStart) / (60 * 1000)
                (minutesSinceSlot * DP_PER_MINUTE.value).dp
            }
            
            Box(
                modifier = Modifier
                    .width(1.dp) // Línea muy fina
                    .fillMaxHeight()
                    .offset(x = (240.dp + currentLinePosition)) // Posición relativa desde el inicio del área de tiempo
                    .background(Color(0xFF00AAFF).copy(alpha = 0.6f)) // Azul claro, semi-transparente
            )
        }
    }
}

/**
 * Sección inferior: Timeline EPG con canales y programas
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimelineSection(
    channels: List<Channel>,
    onChannelFocused: (Channel) -> Unit,
    onProgramFocused: (EpgProgram, Channel) -> Unit,
    onNavigateToPlayer: (Channel) -> Unit,
    firstItemFocusRequester: FocusRequester,
    epgLoadAttempt: Int = 0, // Estado para forzar recarga cuando EPG se parsea
    scrollState: androidx.compose.foundation.ScrollState, // ScrollState compartido para sincronización
    modifier: Modifier = Modifier
) {
    TvLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        channels.forEachIndexed { channelIndex, channel ->
            item {
                ChannelTimelineRow(
                    channel = channel,
                    channelIndex = channelIndex,
                    onChannelFocused = onChannelFocused,
                    onProgramFocused = onProgramFocused,
                    onNavigateToPlayer = onNavigateToPlayer,
                    firstItemFocusRequester = if (channelIndex == 0) firstItemFocusRequester else null,
                    epgLoadAttempt = epgLoadAttempt, // Pasar estado para forzar recarga
                    scrollState = scrollState // Pasar ScrollState compartido
                )
            }
        }
    }
}

/**
 * Fila de timeline para un canal: Logo/Nombre (izquierda) + Separador + Programas (derecha)
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelTimelineRow(
    channel: Channel,
    channelIndex: Int = 0,
    onChannelFocused: (Channel) -> Unit,
    onProgramFocused: (EpgProgram, Channel) -> Unit,
    onNavigateToPlayer: (Channel) -> Unit,
    firstItemFocusRequester: FocusRequester? = null,
    epgLoadAttempt: Int = 0, // Estado para forzar recarga cuando EPG se parsea
    scrollState: androidx.compose.foundation.ScrollState // ScrollState compartido para sincronización
) {
    // Usar singleton para acceder a los datos de EPG cargados desde MainMenu
    val epgManager = remember { EpgManager.getInstance() }
    // Matching flexible: pasar tanto tvgId como name para buscar en ambos casos
    // Esto es útil porque los IDs en el caché pueden ser muy largos (ej: kjhsa8354...)
    // Usar epgLoadAttempt como clave adicional para forzar recarga cuando el EPG se parsea
    val programs = remember(channel.tvgId, channel.name, epgLoadAttempt) {
        val channelId = if (channel.tvgId.isNullOrBlank()) null else channel.tvgId
        val channelName = channel.name // Siempre pasar el nombre como respaldo
        epgManager.getProgramsForChannel(channelId, channelName)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp), // Reducido 25% adicional (de 72.dp a 54.dp - ultra-compacto)
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // a) Columna de Canal (ancho fijo 240.dp)
        ChannelInfoColumn(
            channel = channel,
            modifier = Modifier.width(240.dp)
        )
        
        // Separador azul eliminado - transición limpia sin divisiones
        
        // b) Timeline de programas (resto del espacio) - EPG REAL
        // Usa Row con horizontalScroll para sincronización perfecta con sharedScrollState
        if (programs.isEmpty()) {
            // Si no hay programas EPG, mostrar un único bloque de 24 horas (1440 min * 8.dp = 11520.dp)
            // Esto evita huecos negros a la derecha
            Box(
                modifier = Modifier
                    .width((1440 * DP_PER_MINUTE.value).dp) // 24 horas = 1440 minutos
                    .fillMaxHeight()
                    .background(Color(0xFF1A1D24))
                    .padding(horizontal = 0.dp, vertical = 4.dp), // Sin padding lateral para tocar la línea vertical
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Sin información de programación",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        } else {
            // Si hay programas EPG reales, mostrarlos con Row y horizontalScroll
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(scrollState) // Scroll sincronizado con sharedScrollState
                    .padding(horizontal = 0.dp), // Sin padding lateral para tocar la línea vertical
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                programs.forEachIndexed { programIndex, program ->
                    TimelineProgramCard(
                        program = program,
                        channel = channel,
                        onProgramFocused = { onProgramFocused(program, channel) },
                        onChannelFocused = { onChannelFocused(channel) },
                        onNavigateToPlayer = onNavigateToPlayer,
                        focusRequester = if (channelIndex == 0 && programIndex == 0) firstItemFocusRequester else null
                    )
                }
            }
        }
    }
}

/**
 * Columna de información del canal: [Número] -> [Logo] -> [Nombre]
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelInfoColumn(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número de canal (aumentado a 20.sp)
        Text(
            text = "${channel.id + 1}",
            fontSize = 20.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(32.dp)
        )
        
        // Logo del canal (30.dp, transparente, sin fondo - ajustado para 54.dp de altura)
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Icono por defecto si no hay logo
                Text(
                    text = "📺",
                    fontSize = 18.sp // Reducido proporcionalmente para 30.dp
                )
            }
        }
        
        // Espacio entre logo y nombre (20.dp)
        Spacer(modifier = Modifier.width(20.dp))
        
        // Nombre del canal (reducido ligeramente para mantener elegancia)
        Text(
            text = channel.name,
            fontSize = 11.sp, // Reducido de 12.sp a 11.sp para mejor ajuste
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Tarjeta de programa en el timeline
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimelineProgramCard(
    program: EpgProgram,
    channel: Channel,
    onProgramFocused: () -> Unit,
    onChannelFocused: () -> Unit,
    onNavigateToPlayer: (Channel) -> Unit,
    focusRequester: FocusRequester? = null,
    programWidth: androidx.compose.ui.unit.Dp = 220.dp, // Ancho calculado externamente
    modifier: Modifier = Modifier // Modifier para aplicar offset absoluto
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Cuando el programa recibe foco, notificar al componente padre inmediatamente
    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(300) // Debounce de 300ms
            onProgramFocused()
            onChannelFocused()
        }
    }
    
    val displayTitle = if (program.title.isBlank() || program.title == "Sin información") {
        channel.name
    } else {
        program.title
    }
    
    // Formatear hora de inicio del programa (HH:mm)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startTime = remember(program.start) { timeFormat.format(Date(program.start)) }
    
    // Aplicar FocusRequester si se proporciona (para el primer programa del primer canal)
    val modifierWithFocusRequester = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }
    
    Surface(
        onClick = {
            // Al hacer clic en OK, navegar al reproductor a pantalla completa
            onNavigateToPlayer(channel)
        },
        modifier = modifier
            .then(modifierWithFocusRequester)
            .width(programWidth) // Ancho calculado basado en duración
            .fillMaxHeight()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                // Actualizar focusedProgram inmediatamente cuando recibe foco
                if (focusState.isFocused) {
                    onProgramFocused()
                }
            }
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF00B2FF) else GridLineColor,
                shape = RoundedCornerShape(0.dp) // Esquinas rectas (90 grados) - sin redondeo
            ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = ProgramContainerColor,
            focusedContainerColor = Color(0xFF2A2D34)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(0.dp)), // Rectángulo perfecto - sin esquinas redondeadas
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f) // Sin efecto de agrandar - tamaño estático
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp, vertical = 4.dp) // Sin padding horizontal para tocar la línea vertical
        ) {
            // HORA DE INICIO: Esquina superior pequeña
            Text(
                text = startTime,
                fontSize = 9.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.TopStart)
            )
            
            // TÍTULO DEL PROGRAMA: Centrado (usando datos EPG reales)
            Text(
                text = displayTitle,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
            )
        }
    }
}

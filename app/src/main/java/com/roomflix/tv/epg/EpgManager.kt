package com.roomflix.tv.epg

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * EpgManager - Motor de EPG (Electronic Program Guide) - Singleton
 * 
 * Funcionalidades:
 * - Descarga EPG XML desde servidor HTTP
 * - Parsea XMLTV formato usando XmlPullParser
 * - Convierte fechas XMLTV a timestamps Java
 * - Almacena programas en caché en memoria (HashMap)
 * - Singleton para compartir datos entre actividades
 */
class EpgManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: EpgManager? = null
        
        /**
         * Obtiene la instancia singleton de EpgManager
         */
        fun getInstance(): EpgManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EpgManager().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "EpgManager"
        private const val EPG_URL = "http://epg.roomflix.tv:25461/xmltv.php?username=epg&password=Crwireless.2011"
        
        // Formato de fecha XMLTV: "20260118080000 +0100" (YYYYMMDDHHmmss + timezone offset)
        // Una sola instancia reutilizada: crear formateadores dentro del bucle es muy lento en Android.
        private val XMLTV_DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private const val EPG_DOWNLOAD_TIMEOUT_SECONDS = 30L
    }
    
    // Caché en memoria: channelId -> List<EpgProgram>
    // Compartido entre todas las instancias (singleton)
    private val epgCache: HashMap<String, MutableList<EpgProgram>> = HashMap()
    
    /**
     * Descarga el EPG XML desde el servidor en un hilo secundario
     * Usa OkHttpClient para realizar la petición HTTP
     * 
     * @return String con el contenido XML del EPG, o null si hay error
     */
    suspend fun downloadEpg(): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando descarga de EPG desde: $EPG_URL")
            
            val client = OkHttpClient.Builder()
                .connectTimeout(EPG_DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(EPG_DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(EPG_DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(EPG_URL)
                .get()
                .build()
            
            // Usar .use para cerrar automáticamente la respuesta y evitar fugas de memoria
            // Esto es crítico en Android TV donde los recursos son limitados
            return@withContext client.newCall(request).execute().use { response ->
                // 1. Acceso correcto al código mediante la función code()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error al descargar EPG: HTTP ${response.code}")
                    return@use null // Sale del bloque .use de forma segura
                }
                
                // 2. Acceso correcto al cuerpo mediante la función body()
                val responseBody = response.body
                val xmlString = responseBody?.string()
                
                if (xmlString != null && xmlString.isNotBlank()) {
                    Log.d(TAG, "EPG descargado correctamente (${xmlString.length} bytes)")
                    xmlString
                } else {
                    Log.e(TAG, "El cuerpo de la respuesta está vacío")
                    null
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar EPG: ${e.message}", e)
            null
        }
    }
    
    /**
     * Parsea el contenido XML del EPG usando XmlPullParser
     * Extrae programas desde etiquetas <programme>
     * 
     * @param xmlContent Contenido XML del EPG
     * @return HashMap con channelId como clave y lista de programas como valor
     */
    fun parseEpgXml(xmlContent: String): HashMap<String, MutableList<EpgProgram>> {
        val programs = HashMap<String, MutableList<EpgProgram>>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            
            parser.setInput(StringReader(xmlContent))
            
            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentProgram: EpgProgramBuilder? = null
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                currentChannelId = parser.getAttributeValue(null, "channel")
                                val startStr = parser.getAttributeValue(null, "start")
                                val stopStr = parser.getAttributeValue(null, "stop")
                                
                                currentProgram = EpgProgramBuilder(
                                    channelId = currentChannelId ?: "",
                                    start = parseEpgDate(startStr),
                                    end = parseEpgDate(stopStr)
                                )
                            }
                            "title" -> {
                                // Título del programa: leer texto después del tag
                                if (currentProgram != null) {
                                    parser.next()
                                    if (parser.eventType == XmlPullParser.TEXT) {
                                        currentProgram.title = parser.text?.trim() ?: ""
                                    }
                                }
                            }
                            "desc" -> {
                                // Descripción del programa: leer texto después del tag
                                if (currentProgram != null) {
                                    parser.next()
                                    if (parser.eventType == XmlPullParser.TEXT) {
                                        currentProgram.description = parser.text?.trim() ?: ""
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme" && currentProgram != null && currentChannelId != null) {
                            // Fin del programa: añadirlo a la lista
                            val program = currentProgram.build()
                            val channelPrograms = programs.getOrPut(currentChannelId) { mutableListOf() }
                            channelPrograms.add(program)
                            currentProgram = null
                        }
                    }
                }
                eventType = parser.next()
            }
            
            Log.d(TAG, "EPG parseado: ${programs.size} canales, ${programs.values.sumOf { it.size }} programas totales")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear EPG XML: ${e.message}", e)
        }
        
        return programs
    }
    
    /**
     * Convierte fecha XMLTV a timestamp Long (milliseconds desde epoch)
     * 
     * Versión robusta que utiliza UTC como base antes de aplicar el offset.
     * Xtream UI R21 suele enviar: yyyyMMddHHmmss +0000
     * 
     * @param dateString String con fecha en formato XMLTV (no null)
     * @return Long timestamp en milliseconds, o 0L si hay error
     */
    private fun parseEpgDate(dateString: String?): Long {
        if (dateString.isNullOrBlank()) return 0L
        return try {
            synchronized(XMLTV_DATE_FORMAT) {
                val date = XMLTV_DATE_FORMAT.parse(dateString)
                date?.time ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Actualiza el caché de EPG con programas parseados
     * 
     * @param programs HashMap con channelId -> List<EpgProgram>
     */
    fun updateCache(programs: HashMap<String, MutableList<EpgProgram>>) {
        epgCache.clear()
        epgCache.putAll(programs)
        Log.d(TAG, "Caché EPG actualizado: ${epgCache.size} canales")
    }
    
    /**
     * Descarga y parsea el EPG de forma asíncrona (para usar desde MainMenu)
     * Todo el trabajo (descarga + parseo XML) se ejecuta en Dispatchers.IO para no bloquear el Main.
     */
    fun downloadEpgAsync(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Iniciando descarga asíncrona de EPG...")
                val xmlContent = downloadEpg()
                if (xmlContent != null) {
                    val programs = parseEpgXml(xmlContent)
                    updateCache(programs)
                    Log.d(TAG, "EPG descargado y parseado correctamente desde MainMenu")
                } else {
                    Log.w(TAG, "No se pudo descargar el EPG (xmlContent es null)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al descargar EPG de forma asíncrona: ${e.message}", e)
            }
        }
    }
    
    /**
     * Obtiene los programas de un canal desde el caché
     * Busca primero por channelId (tvg-id), y si no encuentra nada y channelId está vacío,
     * intenta buscar por channelName
     * 
     * @param channelId ID del canal (tvg-id)
     * @param channelName Nombre del canal (usado como fallback si channelId está vacío)
     * @return Lista de programas del canal, o lista vacía si no hay datos
     */
    fun getProgramsForChannel(channelId: String?, channelName: String? = null): List<EpgProgram> {
        // Buscar primero por channelId (tvg-id) si está disponible
        if (!channelId.isNullOrBlank()) {
            val programs = epgCache[channelId]
            if (programs != null && programs.isNotEmpty()) {
                return programs
            }
        }
        
        // Como respaldo, buscar por channelName (matching flexible)
        // Esto es útil cuando los IDs en el caché son muy largos (ej: kjhsa8354...)
        // o cuando el channelId no coincide exactamente
        if (!channelName.isNullOrBlank()) {
            // Buscar en el caché usando el nombre del canal como clave
            // Matching flexible: exacto (case-insensitive) o contiene
            val matchingKey = epgCache.keys.firstOrNull { key ->
                // Coincidencia exacta (case-insensitive)
                key.equals(channelName, ignoreCase = true) ||
                // El nombre está contenido en la clave (útil para IDs largos)
                key.contains(channelName, ignoreCase = true) ||
                // La clave está contenida en el nombre (útil para variaciones)
                channelName.contains(key, ignoreCase = true) ||
                // Coincidencia parcial: normalizar y buscar partes del nombre
                channelName.lowercase().split(" ").any { word ->
                    word.isNotBlank() && key.lowercase().contains(word)
                }
            }
            if (matchingKey != null) {
                return epgCache[matchingKey] ?: emptyList()
            }
        }
        
        return emptyList()
    }
    
    /**
     * Obtiene el programa actual que se está emitiendo en un canal
     * 
     * @param channelId ID del canal (tvg-id)
     * @param channelName Nombre del canal (usado como fallback si channelId está vacío)
     * @return EpgProgram del programa actual, o null si no hay datos
     */
    fun getCurrentProgram(channelId: String?, channelName: String? = null): EpgProgram? {
        val programs = getProgramsForChannel(channelId, channelName)
        val now = System.currentTimeMillis()
        
        return programs.firstOrNull { program ->
            now >= program.start && now < program.end
        }
    }
    
    /**
     * Obtiene el siguiente programa después del actual
     * 
     * @param channelId ID del canal (tvg-id)
     * @param channelName Nombre del canal (usado como fallback si channelId está vacío)
     * @return EpgProgram del siguiente programa, o null si no hay datos
     */
    fun getNextProgram(channelId: String?, channelName: String? = null): EpgProgram? {
        val currentProgram = getCurrentProgram(channelId, channelName) ?: return null
        val programs = getProgramsForChannel(channelId, channelName)
        
        // Buscar el programa que empieza justo después del actual
        return programs.firstOrNull { program ->
            program.start >= currentProgram.end
        }
    }
    
    /**
     * Clase helper para construir EpgProgram durante el parsing
     */
    private data class EpgProgramBuilder(
        val channelId: String,
        val start: Long,
        val end: Long,
        var title: String = "",
        var description: String = ""
    ) {
        fun build(): EpgProgram {
            return EpgProgram(
                channelId = channelId,
                title = title.ifBlank { "Sin título" },
                start = start,
                end = end,
                description = description
            )
        }
    }
}

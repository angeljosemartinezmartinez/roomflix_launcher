package com.roomflix.tv.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roomflix.tv.Constants
import com.roomflix.tv.dragger.MySharedPreferences
import com.roomflix.tv.managers.DBManager
import com.roomflix.tv.epg.EpgManager
import com.roomflix.tv.epg.EpgProgram
import com.roomflix.tv.network.DeviceIdProvider
import com.roomflix.tv.repository.EpgRepository
import com.roomflix.tv.repository.ChannelsRepository
import com.roomflix.tv.utils.M3uParser
import coil.ImageLoader
import coil.request.ImageRequest
import com.roomflix.tv.listener.CallBackSaveData
import com.roomflix.tv.network.response.Channel
import com.roomflix.tv.network.response.ResponseAllInfo
import com.roomflix.tv.network.response.ResponseConfiguration
import com.roomflix.tv.network.response.ResponseUpdate
import com.roomflix.tv.network.service.ApiPro
import com.roomflix.tv.network.service.Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * SplashViewModel - ViewModel para la pantalla de carga inicial
 * 
 * Gestiona la carga asíncrona de:
 * 1. API Main (configuración principal)
 * 2. API Update (actualizaciones)
 * 3. Canales (lista M3U)
 * 4. EPG (guía de programación)
 */
class SplashViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "SplashViewModel"
    }
    
    // Acceso a SharedPreferences y DBManager a través del Application
    private val mySharedPreferences: MySharedPreferences by lazy {
        // Obtener SharedPreferences con el mismo nombre que usa SharedPreferencesModule
        val sharedPrefs = application.getSharedPreferences(
            "hotelPlay",
            android.content.Context.MODE_PRIVATE
        )
        MySharedPreferences(sharedPrefs)
    }
    
    private val dbManager: DBManager by lazy {
        DBManager()
    }
    
    // CompletableDeferred para sincronizar el guardado en base de datos
    private var dbSaveDeferred: CompletableDeferred<Boolean>? = null
    
    sealed class LoadingState {
        object Idle : LoadingState()
        object Loading : LoadingState()
        data class Success(
            val mainConfig: ResponseAllInfo?,
            val updateConfig: ResponseUpdate?,
            val channels: List<Channel>,
            val epgLoaded: Boolean
        ) : LoadingState() {
            // Los datos ya están almacenados en EpgRepository y ChannelsRepository
            // para acceso rápido sin necesidad de parsear en MainMenu o PlayerActivity
        }
        data class Error(val message: String) : LoadingState()
    }
    
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()
    
    private val _loadingMessage = MutableStateFlow<String>("")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _registrationIdToShow = MutableStateFlow<String?>(null)
    val registrationIdToShow: StateFlow<String?> = _registrationIdToShow.asStateFlow()
    
    /**
     * Carga todos los datos necesarios de forma concurrente
     */
    fun loadAllData() {
        if (_loadingState.value is LoadingState.Loading) {
            Log.w(TAG, "La carga ya está en progreso")
            return
        }
        
        _loadingState.value = LoadingState.Loading
        _loadingMessage.value = "Iniciando carga..."
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Iniciando carga de datos en segundo plano")
                
                // PARALELIZACIÓN TOTAL: Ejecutar las tres tareas principales simultáneamente
                val mainConfigDeferred = async(Dispatchers.IO) { fetchMainConfig() }
                val channelsDeferred = async(Dispatchers.IO) { fetchChannels() }
                val epgDeferred = async(Dispatchers.IO) { fetchAndParseEpg() }
                val updateConfigDeferred = async(Dispatchers.IO) { fetchUpdateConfig() }
                
                // Esperar a que todas terminen
                val results = awaitAll(mainConfigDeferred, channelsDeferred, epgDeferred, updateConfigDeferred)

                // Si el equipo no está registrado (403/500), quedarnos en Splash mostrando el ID
                if (!_registrationIdToShow.value.isNullOrBlank()) {
                    _loadingState.value = LoadingState.Error("EQUIPO_NO_REGISTRADO")
                    _loadingMessage.value = ""
                    return@launch
                }
                
                val mainConfig = results[0] as? ResponseAllInfo
                val channels = results[1] as? List<Channel> ?: emptyList()
                val epgData = results[2] as? Map<String, MutableList<EpgProgram>>?
                val updateConfig = results[3] as? ResponseUpdate
                val epgLoaded = epgData != null
                
                Log.d(TAG, "Carga completada: ${channels.size} canales, EPG: $epgLoaded")
                
                // ALMACENAR EN REPOSITORIOS SINGLETON para acceso rápido
                // Asignación directa a propiedades públicas (sin métodos setter manuales)
                if (channels.isNotEmpty()) {
                    ChannelsRepository.setChannels(channels)
                    EpgRepository.channels.clear()
                    EpgRepository.channels.addAll(channels)
                    Log.d(TAG, "Canales almacenados en repositorios")
                }
                
                if (epgData != null) {
                    EpgRepository.setEpgData(epgData)
                    Log.d(TAG, "EPG almacenado en EpgRepository")
                }
                
                if (mainConfig != null) {
                    EpgRepository.mainConfig = mainConfig
                    Log.d(TAG, "MainConfig almacenado en EpgRepository")
                }
                
                // PRE-CARGA DE IMÁGENES (Cache Warming): Pre-cargar logos de los primeros 10 canales
                if (channels.isNotEmpty()) {
                    _loadingMessage.value = "Pre-cargando imágenes..."
                    preloadChannelLogos(channels.take(10))
                }
                
                // Guardar datos en SharedPreferences y base de datos si tenemos mainConfig
                // IMPORTANTE: Esperar a que el guardado termine antes de emitir Success
                if (mainConfig != null) {
                    _loadingMessage.value = "Guardando configuración..."
                    try {
                        // Crear nuevo CompletableDeferred para esta operación
                        dbSaveDeferred = CompletableDeferred()
                        
                        // Iniciar guardado (esto es asíncrono)
                        saveDataToStorage(mainConfig, updateConfig)
                        
                        // BLOQUEAR hasta que el guardado en BD termine realmente
                        val saveSuccess = dbSaveDeferred?.await() ?: false
                        
                        if (saveSuccess) {
                            Log.d(TAG, "Guardado de datos completado exitosamente, datos listos para MainMenu")
                        } else {
                            Log.w(TAG, "Guardado de datos completado con errores, pero continuando")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al guardar datos, continuando de todas formas", e)
                        // Asegurar que el deferred se complete incluso si hay error
                        dbSaveDeferred?.complete(false)
                    } finally {
                        dbSaveDeferred = null
                    }
                }
                
                // Solo emitir Success después de que el guardado haya terminado
                // Los datos ya están en los repositorios singleton, listos para MainMenu y PlayerActivity
                _loadingState.value = LoadingState.Success(mainConfig, updateConfig, channels, epgLoaded)
                _loadingMessage.value = "Carga completada"
                
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la carga de datos", e)
                _loadingState.value = LoadingState.Error(e.message ?: "Error desconocido")
                _loadingMessage.value = "Error: ${e.message}"
            }
        }
    }
    
    /**
     * Obtiene la configuración principal desde api/main
     */
    private suspend fun fetchMainConfig(): ResponseAllInfo? = withContext(Dispatchers.IO) {
        try {
            _loadingMessage.value = "Cargando configuración..."
            Log.d(TAG, "Iniciando carga de configuración principal (api/main)")
            
            val deviceId = DeviceIdProvider.getDeviceId(getApplication())
            val service = ApiPro.createService(Service::class.java)
            
            // Usar CountDownLatch para convertir Callback a suspend
            val latch = CountDownLatch(1)
            val resultRef = AtomicReference<ResponseAllInfo?>(null)
            var errorMessage: String? = null
            
            service.getData(deviceId).enqueue(object : Callback<ResponseAllInfo> {
                override fun onResponse(call: Call<ResponseAllInfo>, response: Response<ResponseAllInfo>) {
                    if (response.isSuccessful) {
                        resultRef.set(response.body())
                        Log.d(TAG, "Configuración principal cargada exitosamente")
                    } else {
                        errorMessage = "HTTP ${response.code()}"
                        Log.e(TAG, "Error HTTP al cargar configuración: ${response.code()}")
                        if (response.code() == 403 || response.code() == 500) {
                            _registrationIdToShow.value = deviceId
                        }
                    }
                    latch.countDown()
                }
                
                override fun onFailure(call: Call<ResponseAllInfo>, t: Throwable) {
                    errorMessage = t.message ?: "Error de red"
                    Log.e(TAG, "Error de red al cargar configuración", t)
                    latch.countDown()
                }
            })
            
            latch.await()
            
            if (errorMessage != null) {
                Log.w(TAG, "Error al cargar configuración: $errorMessage (continuando sin ella)")
            }
            
            resultRef.get()
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al cargar configuración principal", e)
            null // Continuar sin configuración principal si falla
        }
    }
    
    /**
     * Obtiene la configuración de actualizaciones desde api/launcher
     */
    private suspend fun fetchUpdateConfig(): ResponseUpdate? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando carga de configuración de actualizaciones (api/launcher)")
            
            val deviceId = DeviceIdProvider.getDeviceId(getApplication())
            val service = ApiPro.createService(Service::class.java)
            
            val latch = CountDownLatch(1)
            val resultRef = AtomicReference<ResponseUpdate?>(null)
            var errorMessage: String? = null
            
            service.getUpdate(deviceId).enqueue(object : Callback<ResponseUpdate> {
                override fun onResponse(call: Call<ResponseUpdate>, response: Response<ResponseUpdate>) {
                    if (response.isSuccessful) {
                        resultRef.set(response.body())
                        Log.d(TAG, "Configuración de actualizaciones cargada exitosamente")
                    } else {
                        errorMessage = "HTTP ${response.code()}"
                        Log.e(TAG, "Error HTTP al cargar actualizaciones: ${response.code()}")
                        if (response.code() == 403 || response.code() == 500) {
                            _registrationIdToShow.value = deviceId
                        }
                    }
                    latch.countDown()
                }
                
                override fun onFailure(call: Call<ResponseUpdate>, t: Throwable) {
                    errorMessage = t.message ?: "Error de red"
                    Log.e(TAG, "Error de red al cargar actualizaciones", t)
                    latch.countDown()
                }
            })
            
            latch.await()
            
            if (errorMessage != null) {
                Log.w(TAG, "Error al cargar actualizaciones: $errorMessage (continuando sin ellas)")
            }
            
            resultRef.get()
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al cargar configuración de actualizaciones", e)
            null // Continuar sin actualizaciones si falla
        }
    }
    
    /**
     * Obtiene la lista de canales desde api/tv
     */
    private suspend fun fetchChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            _loadingMessage.value = "Cargando canales..."
            Log.d(TAG, "Iniciando carga de canales")
            
            val deviceId = DeviceIdProvider.getDeviceId(getApplication())
            val service = ApiPro.createService(Service::class.java)
            
            val latch = CountDownLatch(1)
            val resultRef = AtomicReference<String?>(null)
            var errorMessage: String? = null
            
            service.getChannels(deviceId).enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        resultRef.set(response.body())
                        Log.d(TAG, "Canales descargados exitosamente")
                    } else {
                        errorMessage = "HTTP ${response.code()}"
                        Log.e(TAG, "Error HTTP al cargar canales: ${response.code()}")
                        if (response.code() == 403 || response.code() == 500) {
                            _registrationIdToShow.value = deviceId
                        }
                    }
                    latch.countDown()
                }
                
                override fun onFailure(call: Call<String>, t: Throwable) {
                    errorMessage = t.message ?: "Error de red"
                    Log.e(TAG, "Error de red al cargar canales", t)
                    latch.countDown()
                }
            })
            
            latch.await()
            
            val m3uContent = resultRef.get()
            if (m3uContent.isNullOrBlank()) {
                // No crashear por errores HTTP (403/500) o contenido vacío: continuar con caché/DB
                val friendly = "Error de conexión con el servidor"
                val err = errorMessage ?: "Contenido M3U vacío"
                Log.e(TAG, "fetchChannels falló: $err. Usando datos locales si existen.")
                _loadingMessage.value = friendly

                val cached = when {
                    ChannelsRepository.hasChannels() -> ChannelsRepository.getChannels()
                    EpgRepository.hasChannels() -> EpgRepository.channels.toList()
                    else -> emptyList()
                }
                return@withContext cached
            }
            
            // Parsear M3U usando M3uParser
            val channels = M3uParser.parse(m3uContent)
            Log.d(TAG, "Canales parseados: ${channels.size}")
            
            channels
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar canales", e)
            // Evitar crash en Splash por 403/500 u otros: continuar con caché
            _loadingMessage.value = "Error de conexión con el servidor"
            val cached = when {
                ChannelsRepository.hasChannels() -> ChannelsRepository.getChannels()
                EpgRepository.hasChannels() -> EpgRepository.channels.toList()
                else -> emptyList()
            }
            cached
        }
    }
    
    /**
     * Descarga y parsea el EPG completo
     * PRE-PROCESADO: Convierte el XML en Map y calcula programas actuales/siguientes
     */
    private suspend fun fetchAndParseEpg(): Map<String, MutableList<EpgProgram>>? = withContext(Dispatchers.IO) {
        try {
            _loadingMessage.value = "Sincronizando guía de programación..."
            Log.d(TAG, "Iniciando descarga y parseo de EPG")
            
            val epgManager = EpgManager.getInstance()
            
            // Descargar EPG
            val xmlContent = epgManager.downloadEpg()
            if (xmlContent == null) {
                Log.w(TAG, "No se pudo descargar el EPG (continuando sin él)")
                return@withContext null
            }
            
            // Parsear EPG con corrección de zona horaria
            val programs = epgManager.parseEpgXml(xmlContent)
            
            // Corregir zonas horarias usando ZoneId.systemDefault()
            val systemZoneId = ZoneId.systemDefault()
            val correctedPrograms = HashMap<String, MutableList<EpgProgram>>()
            
            programs.forEach { (channelId, programList) ->
                val correctedList = programList.map { program ->
                    // Convertir timestamps a la zona horaria del sistema
                    val startInstant = Instant.ofEpochMilli(program.start)
                    val endInstant = Instant.ofEpochMilli(program.end)
                    
                    val startZoned = ZonedDateTime.ofInstant(startInstant, systemZoneId)
                    val endZoned = ZonedDateTime.ofInstant(endInstant, systemZoneId)
                    
                    // Crear nuevo programa con timestamps corregidos
                    program.copy(
                        start = startZoned.toInstant().toEpochMilli(),
                        end = endZoned.toInstant().toEpochMilli()
                    )
                }.toMutableList()
                
                correctedPrograms[channelId] = correctedList
            }
            
            // Actualizar caché del EpgManager (para compatibilidad)
            epgManager.updateCache(correctedPrograms)
            
            Log.d(TAG, "EPG parseado y corregido: ${correctedPrograms.values.sumOf { it.size }} programas")
            correctedPrograms
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar EPG", e)
            null // Continuar sin EPG si falla
        }
    }
    
    /**
     * PRE-CARGA DE IMÁGENES (Cache Warming): Pre-carga logos de canales usando Coil
     * Agiliza la fluidez visual al tener las imágenes ya en caché
     */
    private suspend fun preloadChannelLogos(channels: List<Channel>) = withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader(getApplication())
            var loadedCount = 0
            
            channels.forEach { channel ->
                val logoUrl = channel.logo
                if (!logoUrl.isNullOrBlank()) {
                    try {
                        val request = ImageRequest.Builder(getApplication())
                            .data(logoUrl)
                            .build()
                        imageLoader.enqueue(request)
                        loadedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Error al pre-cargar logo de ${channel.name}: ${e.message}")
                    }
                }
            }
            
            Log.d(TAG, "Pre-carga de imágenes completada: $loadedCount logos en caché")
        } catch (e: Exception) {
            Log.e(TAG, "Error en pre-carga de imágenes", e)
        }
    }
    
    /**
     * Guarda los datos de configuración en SharedPreferences y base de datos
     * Esta función inicia el guardado asíncrono y el callback completará el CompletableDeferred
     */
    private fun saveDataToStorage(mainConfig: ResponseAllInfo, updateConfig: ResponseUpdate?) {
        try {
            Log.d(TAG, "Guardando datos en SharedPreferences y base de datos")
            
            // Guardar baseUrl
            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.BASE_URL, mainConfig.baseUrl)
            
            // Guardar background
            if (mainConfig.templates?.background != null) {
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK, mainConfig.templates.background)
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK_LANG, mainConfig.templates.background)
            }
            
            // Guardar logos
            if (mainConfig.templates?.logo != null) {
                val logo = mainConfig.baseUrl + mainConfig.templates.logo
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LOGO, logo)
            }
            if (mainConfig.templates?.miniLogo != null) {
                val miniLogo = mainConfig.baseUrl + mainConfig.templates.miniLogo
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.MINI_LOGO, miniLogo)
            }
            
            // Guardar ADB server (legacy)
            if (mainConfig.configuration?.adbServer != null) {
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.ADB_SERVER, mainConfig.configuration.adbServer)
            }

            // Guardar Control API config (reemplaza adbServer)
            mainConfig.configuration?.controlApiUrl?.let {
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.CONTROL_API_URL, it)
            }
            mainConfig.configuration?.controlApiToken?.let {
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.CONTROL_API_TOKEN, it)
            }
            mainConfig.configuration?.controlDeviceId?.let {
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.CONTROL_DEVICE_ID, it)
            }

            // Guardar timezone
            if (mainConfig.configuration?.timeZone != null) {
                val timezone = when (mainConfig.configuration.timeZone.toString()) {
                    "0" -> "Europe/London"
                    "1" -> "Europe/Madrid"
                    "2" -> "Europe/Paris"
                    "3" -> "America/New_York"
                    else -> "Europe/Madrid"
                }
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.TIMEZONE, timezone)
            }
            
            // Guardar idioma por defecto
            mainConfig.languages?.forEach { language ->
                if (language.isDefault) {
                    // No sobreescribir el idioma elegido por el usuario (regresión al volver de submenús)
                    val userSelected = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.SELECTED_LANGUAGE_CODE) ?: ""
                    if (userSelected.isBlank()) {
                        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, language.code)
                    }
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANG_DEFAULT, language.code)
                    if (language.channel != null) {
                        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL, language.channel)
                    }
                }
            }
            
            Log.d(TAG, "Datos guardados en SharedPreferences exitosamente")
            
            // Guardar en base de datos usando CompletableDeferred para sincronizar
            // El callback completará el deferred cuando termine
            dbManager.saveData(mainConfig, getApplication(), object : CallBackSaveData {
                override fun finish() {
                    Log.d(TAG, "Datos guardados en base de datos exitosamente")
                    // Completar el deferred con éxito
                    dbSaveDeferred?.complete(true)
                }
                
                override fun error(s: String) {
                    Log.e(TAG, "Error al guardar datos en base de datos: $s")
                    // Completar el deferred con error (false) para no bloquear
                    dbSaveDeferred?.complete(false)
                }
            }, updateConfig)
            
            Log.d(TAG, "Guardado iniciado, esperando callback de DBManager...")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar datos en almacenamiento", e)
            // Asegurar que el deferred se complete incluso si hay error antes del callback
            dbSaveDeferred?.complete(false)
            // No crashear Splash: continuar flujo con caché/DB existente
        }
    }
}

package com.roomflix.tv.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roomflix.tv.model.InstalledApp
import com.roomflix.tv.services.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

/**
 * Estado de clima para la TopInfoBar. La UI lo observa y pinta cuando llega.
 */
data class WeatherUiState(
    val temp: String,
    val weatherCode: Int
)

class MainMenuViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainMenuViewModel"
        private const val OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true"
        private const val DEFAULT_LAT = 40.41
        private const val DEFAULT_LON = -3.70
        private const val REFRESH_WEATHER_INTERVAL_MS = 60 * 60 * 1000L // 60 minutos
    }

    private val locationService = LocationService(application)

    private val _weatherState = MutableStateFlow<WeatherUiState?>(null)
    val weatherState: StateFlow<WeatherUiState?> = _weatherState.asStateFlow()

    /** Lista de apps instaladas (no sistema) para el carrusel "Más apps". Cargada en IO. */
    private val _installedAppsState = MutableStateFlow<List<InstalledApp>>(emptyList<InstalledApp>())
    val installedAppsState: StateFlow<List<InstalledApp>> = _installedAppsState.asStateFlow()

    /** IDs de submenús del JSON (rejilla de botones). Si el target coincide, se abre MoreAppsSubmenuActivity. */
    private val submenuIds = mutableSetOf<String>()

    private var weatherRefreshJob: Job? = null

    init {
        loadWeather()
        startPeriodicWeatherRefresh()
    }

    /**
     * Carga ubicación y clima en segundo plano. No bloquea el arranque.
     * La UI observa weatherState y se actualiza cuando hay datos.
     */
    fun loadWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (lat, lon) = locationService.getCurrentLocation() ?: (DEFAULT_LAT to DEFAULT_LON)
                val weather = fetchOpenMeteo(lat, lon)
                if (weather != null) {
                    _weatherState.value = weather
                } else {
                    _weatherState.value = WeatherUiState("—°", 3)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar clima", e)
                _weatherState.value = WeatherUiState("—°", 3)
            }
        }
    }

    private suspend fun fetchOpenMeteo(lat: Double, lon: Double): WeatherUiState? = withContext(Dispatchers.IO) {
        try {
            // Usar Locale.US para que las coordenadas usen punto decimal (Open-Meteo devuelve 404 con coma)
            val latStr = String.format(Locale.US, "%.6f", lat)
            val lonStr = String.format(Locale.US, "%.6f", lon)
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$latStr&longitude=$lonStr&current_weather=true"
            val json = URL(url).readText()
            val root = JSONObject(json)
            val current = root.getJSONObject("current_weather")
            val temp = current.getDouble("temperature").toInt()
            val weathercode = current.getInt("weathercode")
            WeatherUiState("${temp}°", weathercode)
        } catch (e: Exception) {
            Log.e(TAG, "Error Open-Meteo", e)
            null
        }
    }

    private fun startPeriodicWeatherRefresh() {
        weatherRefreshJob?.cancel()
        weatherRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(REFRESH_WEATHER_INTERVAL_MS)
                loadWeather()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        weatherRefreshJob?.cancel()
    }

    /**
     * Actualiza la lista de IDs de submenús (desde la BD, tras cargar el JSON).
     * Usado para decidir si un botón con type 3 debe abrir la rejilla de submenú o el carrusel de apps.
     */
    fun setSubmenuIds(ids: Collection<String>) {
        submenuIds.clear()
        submenuIds.addAll(ids.map { it.trim() }.filter { it.isNotEmpty() })
    }

    /**
     * Indica si el target corresponde a un submenú del JSON (rejilla de 8 botones).
     */
    fun isTargetSubmenu(target: String?): Boolean {
        if (target.isNullOrBlank()) return false
        return target.trim() in submenuIds
    }

    /**
     * Carga en segundo plano la lista de apps instaladas (lanzables, no sistema).
     * Filtro: ACTION_MAIN + LEANBACK_LAUNCHER o LAUNCHER; excluye FLAG_SYSTEM.
     * Banner: loadBanner() si existe, si no loadIcon().
     */
    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = getInstalledAppsInternal(getApplication<Application>().packageManager)
                _installedAppsState.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar apps instaladas", e)
                _installedAppsState.value = emptyList<InstalledApp>()
            }
        }
    }

    private fun getInstalledAppsInternal(pm: PackageManager): List<InstalledApp> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<InstalledApp>()

        // TV: LEANBACK_LAUNCHER primero; luego LAUNCHER para apps de móvil instaladas
        val mainIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) }
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

        val leanback = pm.queryIntentActivities(mainIntent, 0)
        val launcher = pm.queryIntentActivities(launcherIntent, 0)

        for (info in leanback + launcher) {
            val pkg = info.activityInfo.packageName
            if (pkg in seen) continue
            seen.add(pkg)
            // Excluir la propia app (launcher)
            if (pkg == getApplication<Application>().packageName) continue

            val appInfo = info.activityInfo.applicationInfo
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            if ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) continue

            val name = info.loadLabel(pm)?.toString() ?: pkg
            val banner = try {
                info.activityInfo.loadBanner(pm) ?: info.activityInfo.loadIcon(pm)
            } catch (_: Exception) {
                info.activityInfo.loadIcon(pm)
            }
            result.add(InstalledApp(name = name, packageName = pkg, banner = banner))
        }

        return result.sortedBy { it.name.lowercase() }
    }
}

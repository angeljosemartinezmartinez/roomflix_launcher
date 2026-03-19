package com.roomflix.tv.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Servicio para obtener datos de clima desde API
 * Usa OpenWeatherMap como placeholder (requiere API key)
 * Por ahora usa datos mock para demostración
 */
class WeatherService {
    
    companion object {
        private const val TAG = "WeatherService"
        // API Key de OpenWeatherMap (debe configurarse)
        private const val OPENWEATHER_API_KEY = "YOUR_API_KEY_HERE"
        private const val OPENWEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
    }
    
    /**
     * Obtiene datos de clima para una ubicación específica
     * @param lat Latitud
     * @param lon Longitud
     * @return Pair<Temperatura en Celsius, Icono de clima> o null si falla
     */
    suspend fun getWeatherData(lat: Double, lon: Double): Pair<Int, String>? = withContext(Dispatchers.IO) {
        try {
            // Por ahora, usar datos mock hasta configurar API key
            // TODO: Implementar llamada real a OpenWeatherMap cuando se tenga API key
            /*
            val url = "$OPENWEATHER_BASE_URL?lat=$lat&lon=$lon&appid=$OPENWEATHER_API_KEY&units=metric"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            val temp = json.getJSONObject("main").getDouble("temp").toInt()
            val weatherIcon = json.getJSONArray("weather").getJSONObject(0).getString("icon")
            
            return@withContext Pair(temp, weatherIcon)
            */
            
            // Datos mock por ahora
            Log.d(TAG, "Usando datos mock de clima (lat: $lat, lon: $lon)")
            Pair(18, "cloud") // 18°C, icono de nube
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener datos de clima", e)
            // Fallback: Retornar datos mock
            Pair(18, "cloud")
        }
    }
    
    /**
     * Obtiene datos de clima usando el nombre de la ciudad
     * @param cityName Nombre de la ciudad
     * @return Pair<Temperatura en Celsius, Icono de clima> o null si falla
     */
    suspend fun getWeatherDataByCity(cityName: String): Pair<Int, String>? = withContext(Dispatchers.IO) {
        try {
            // Por ahora, usar datos mock
            // TODO: Implementar llamada real a OpenWeatherMap cuando se tenga API key
            /*
            val url = "$OPENWEATHER_BASE_URL?q=$cityName&appid=$OPENWEATHER_API_KEY&units=metric"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            val temp = json.getJSONObject("main").getDouble("temp").toInt()
            val weatherIcon = json.getJSONArray("weather").getJSONObject(0).getString("icon")
            
            return@withContext Pair(temp, weatherIcon)
            */
            
            // Datos mock por ahora
            Log.d(TAG, "Usando datos mock de clima para ciudad: $cityName")
            Pair(18, "cloud") // 18°C, icono de nube
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener datos de clima por ciudad", e)
            // Fallback: Retornar datos mock
            Pair(18, "cloud")
        }
    }
}

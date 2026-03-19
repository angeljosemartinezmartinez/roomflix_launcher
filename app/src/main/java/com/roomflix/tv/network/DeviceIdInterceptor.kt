package com.roomflix.tv.network

import android.content.Context
import android.util.Log
import com.roomflix.tv.Constants
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor para inyectar el Device ID en las peticiones HTTP
 * 
 * Este interceptor:
 * 1. Intercepta URLs que contengan '/api/main/' o '/api/launcher/'
 * 2. Reemplaza automáticamente el parámetro {mac} (ya procesado por Retrofit) 
 *    con el Device ID obtenido mediante DeviceIdProvider
 * 3. Asegura que la URL final sea: https://panel.hotelplay.tv/api/main/[ID_DEL_DISPOSITIVO]
 * 
 * Esto elimina la necesidad de pasar manualmente el MAC/Device ID desde CallManager
 */
class DeviceIdInterceptor(
    private val context: Context
) : Interceptor {

    companion object {
        private const val TAG = "ROOMFLIX_DEBUG"
        private const val ENDPOINT_MAIN = "/api/main/"
        private const val ENDPOINT_LAUNCHER = "/api/launcher/"
        private const val ENDPOINT_TV = "/api/tv/"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val prefs = context.getSharedPreferences("hotelPlay", Context.MODE_PRIVATE)
        val selectedLang = prefs.getString(Constants.SHARED_PREFERENCES.SELECTED_LANGUAGE_CODE, null)
        val legacyLang = prefs.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, null)
        val lang = (selectedLang?.takeIf { it.isNotBlank() } ?: legacyLang?.takeIf { it.isNotBlank() } ?: "en")
            .lowercase()
        
        // Obtener Device ID usando la lógica "Espejo"
        val deviceId = DeviceIdProvider.getDeviceId(context)
        
        // Verificar si la URL contiene los endpoints que necesitan Device ID
        val urlPath = originalUrl.encodedPath
        Log.d(TAG, "[DeviceIdInterceptor] URL original: ${originalUrl}")
        Log.d(TAG, "[DeviceIdInterceptor] Path original: $urlPath")
        Log.d(TAG, "[DeviceIdInterceptor] Device ID obtenido: $deviceId")
        
        val modifiedUrlWithoutLang: HttpUrl = when {
            urlPath.contains(ENDPOINT_MAIN) -> {
                // Reemplazar todo después de /api/main/ con el Device ID real
                val newUrl = replaceEndpointPath(originalUrl, ENDPOINT_MAIN, deviceId)
                Log.d(TAG, "[DeviceIdInterceptor] URL modificada para /api/main/: ${newUrl}")
                newUrl
            }
            urlPath.contains(ENDPOINT_LAUNCHER) -> {
                // Reemplazar todo después de /api/launcher/ con el Device ID real
                val newUrl = replaceEndpointPath(originalUrl, ENDPOINT_LAUNCHER, deviceId)
                Log.d(TAG, "[DeviceIdInterceptor] URL modificada para /api/launcher/: ${newUrl}")
                newUrl
            }
            urlPath.contains(ENDPOINT_TV) -> {
                // Reemplazar todo después de /api/tv/ con el Device ID real
                val newUrl = replaceEndpointPath(originalUrl, ENDPOINT_TV, deviceId)
                Log.d(TAG, "[DeviceIdInterceptor] URL modificada para /api/tv/: ${newUrl}")
                newUrl
            }
            else -> {
                // Para otras URLs, no modificamos nada
                Log.d(TAG, "[DeviceIdInterceptor] URL no modificada (no contiene endpoints conocidos)")
                originalUrl
            }
        }

        // Inyectar idioma en endpoints que devuelven UI (main/launcher) para que el servidor responda en el idioma correcto.
        // Usamos query + headers para máxima compatibilidad backend.
        val modifiedUrl: HttpUrl = when {
            urlPath.contains(ENDPOINT_MAIN) || urlPath.contains(ENDPOINT_LAUNCHER) -> {
                modifiedUrlWithoutLang.newBuilder()
                    .setQueryParameter("lang", lang)
                    .build()
            }
            else -> modifiedUrlWithoutLang
        }
        
        // Crear nueva petición con la URL modificada y el header del Device ID
        val newRequest = originalRequest.newBuilder()
            .url(modifiedUrl)
            .header("X-Device-Id", deviceId)
            .header("Accept-Language", lang)
            .header("X-Language", lang)
            .build()
        
        // Log de confirmación final del identificador que se envía al servidor
        Log.d(TAG, "[DeviceIdInterceptor] ========================================")
        Log.d(TAG, "[DeviceIdInterceptor] ✅ IDENTIFICADOR FORZADO ENVIADO: $deviceId")
        Log.d(TAG, "[DeviceIdInterceptor] ✅ URL Final enviada al servidor: ${modifiedUrl}")
        Log.d(TAG, "[DeviceIdInterceptor] ✅ URL Final (con query) = ${modifiedUrl.toString()}")
        Log.d(TAG, "[DeviceIdInterceptor] ========================================")
        
        return chain.proceed(newRequest)
    }
    
    /**
     * Reemplaza el segmento del path después del endpoint con el Device ID
     * 
     * Ejemplo: /api/main/placeholder123 -> /api/main/A1B2C3D4E5F6
     */
    private fun replaceEndpointPath(url: HttpUrl, endpoint: String, deviceId: String): HttpUrl {
        val urlBuilder = url.newBuilder()
        
        // Construir el nuevo path: endpoint + deviceId
        val newPath = endpoint + deviceId
        
        // Reemplazar el path completo
        urlBuilder.encodedPath(newPath)
        
        return urlBuilder.build()
    }
}

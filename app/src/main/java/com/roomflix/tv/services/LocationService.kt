package com.roomflix.tv.services

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import java.net.URL
import java.util.Locale

/**
 * Servicio para obtener la ubicaci?n del dispositivo
 * Usa FusedLocationProviderClient con fallback a geolocalizaci?n por IP
 */
class LocationService(private val context: Context) {
    
    companion object {
        private const val TAG = "LocationService"
    }
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    /** Solo una petici?n de ubicaci?n activa a la vez para evitar "register multiple handlers". */
    private val locationMutex = Mutex()
    private val locationScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    
    /** Callback activo para poder cancelar peticiones anteriores y evitar "multiple handlers" */
    @Volatile
    private var activeLocationCallback: LocationCallback? = null
    
    /**
     * Obtiene la ubicaci?n del dispositivo (GPS o IP fallback)
     * @return Pair<Latitud, Longitud> o null si falla
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            // Intentar obtener ubicaci?n por GPS primero
            val location = getLocationFromGPS()
            if (location != null) {
                Log.d(TAG, "Ubicaci?n obtenida por GPS: ${location.first}, ${location.second}")
                return@withContext location
            }
            
            // Fallback: Obtener ubicaci?n por IP
            val locationFromIP = getLocationFromIP()
            if (locationFromIP != null) {
                Log.d(TAG, "Ubicaci?n obtenida por IP: ${locationFromIP.first}, ${locationFromIP.second}")
                return@withContext locationFromIP
            }
            
            Log.w(TAG, "No se pudo obtener ubicaci?n (GPS ni IP)")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ubicaci?n", e)
            null
        }
    }
    
    /**
     * Obtiene la provincia desde la ubicaci?n (GPS o IP)
     * @return Nombre de la provincia o null si falla
     */
    suspend fun getCurrentCity(): String? = withContext(Dispatchers.IO) {
        try {
            val location = getCurrentLocation()
            if (location != null) {
                // Intentar obtener provincia desde coordenadas usando Geocoder
                val province = getProvinceFromCoordinates(location.first, location.second)
                if (province != null) {
                    return@withContext province
                }
            }
            
            // Fallback: Obtener provincia por IP
            getProvinceFromIP()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener provincia", e)
            null
        }
    }
    
    /**
     * Obtiene la provincia usando geocodificaci?n inversa desde coordenadas GPS
     */
    private suspend fun getProvinceFromCoordinates(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                Log.w(TAG, "Geocoder no disponible")
                return@withContext null
            }
            
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // Usar subAdminArea (provincia) con fallback a adminArea (regi?n)
                val province = address.subAdminArea ?: address.adminArea
                if (province != null && province.isNotEmpty()) {
                    Log.d(TAG, "Provincia obtenida desde coordenadas: $province")
                    return@withContext province
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener provincia desde coordenadas", e)
            null
        }
    }
    
    /**
     * Obtiene ubicaci?n usando GPS (FusedLocationProviderClient).
     * Serializa peticiones con Mutex para evitar "prohibited to register multiple handlers".
     */
    private suspend fun getLocationFromGPS(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    if (continuation.isActive) continuation.resume(Pair(location.latitude, location.longitude))
                    return@addOnSuccessListener
                }
                locationScope.launch {
                    val result = locationMutex.withLock {
                        suspendCancellableCoroutine<Pair<Double, Double>?> { inner ->
                            requestLocationUpdate(inner)
                        }
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
            }.addOnFailureListener {
                locationScope.launch {
                    val result = locationMutex.withLock {
                        suspendCancellableCoroutine<Pair<Double, Double>?> { inner ->
                            requestLocationUpdate(inner)
                        }
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permisos de ubicaci?n no concedidos", e)
            if (continuation.isActive) continuation.resume(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ubicaci?n GPS", e)
            if (continuation.isActive) continuation.resume(null)
        }
    }
    
    /**
     * Solicita una actualizaci?n de ubicaci?n.
     * Cancela cualquier petici?n anterior para evitar IllegalStateException "multiple handlers".
     */
    private fun requestLocationUpdate(continuation: CancellableContinuation<Pair<Double, Double>?>) {
        try {
            activeLocationCallback?.let { callback ->
                fusedLocationClient.removeLocationUpdates(callback)
                activeLocationCallback = null
            }
            
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                interval = 10000
                fastestInterval = 5000
                numUpdates = 1
            }
            
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null && continuation.isActive) {
                        activeLocationCallback = null
                        fusedLocationClient.removeLocationUpdates(this)
                        continuation.resume(Pair(location.latitude, location.longitude))
                    } else if (continuation.isActive) {
                        activeLocationCallback = null
                        fusedLocationClient.removeLocationUpdates(this)
                        continuation.resume(null)
                    }
                }
            }
            activeLocationCallback = locationCallback
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
            
            continuation.invokeOnCancellation {
                activeLocationCallback = null
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
            
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (continuation.isActive) {
                    activeLocationCallback = null
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    continuation.resume(null)
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 10000)
            
            continuation.invokeOnCancellation {
                timeoutHandler.removeCallbacks(timeoutRunnable)
            }
            
        } catch (e: SecurityException) {
            Log.w(TAG, "Permisos de ubicaci?n no concedidos", e)
            if (continuation.isActive) {
                continuation.resume(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al solicitar actualizaci?n de ubicaci?n", e)
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Obtiene ubicaci?n aproximada usando geolocalizaci?n por IP (ip-api.com)
     */
    private suspend fun getLocationFromIP(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val url = "http://ip-api.com/json/?fields=lat,lon"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            val lat = json.optDouble("lat", Double.NaN)
            val lon = json.optDouble("lon", Double.NaN)
            
            if (!lat.isNaN() && !lon.isNaN()) {
                return@withContext Pair(lat, lon)
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ubicaci?n por IP", e)
            null
        }
    }
    
    /**
     * Obtiene la provincia usando geolocalizaci?n por IP (ip-api.com)
     * Usa regionName que corresponde a la provincia/estado
     */
    private suspend fun getProvinceFromIP(): String? = withContext(Dispatchers.IO) {
        try {
            val url = "http://ip-api.com/json/?fields=regionName,region"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            // Intentar obtener regionName primero (nombre completo de la provincia)
            val regionName = json.optString("regionName", "")
            if (regionName.isNotEmpty()) {
                Log.d(TAG, "Provincia obtenida por IP: $regionName")
                return@withContext regionName
            }
            
            // Fallback: usar region (c?digo de regi?n)
            val region = json.optString("region", "")
            if (region.isNotEmpty()) {
                Log.d(TAG, "Regi?n obtenida por IP: $region")
                return@withContext region
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener provincia por IP", e)
            null
        }
    }
}

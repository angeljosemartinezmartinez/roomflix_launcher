package com.roomflix.tv.network

import android.content.Context
import android.util.Log
import com.roomflix.tv.device.MacAddressProvider

object DeviceIdProvider {

    private const val TAG = "DEVICE_ID"

    /**
     * Obtiene el identificador del dispositivo (12 chars).
     * Usa MAC real como fuente primaria (estable ante factory reset).
     * Fallback a ANDROID_ID si MAC no disponible.
     *
     * Devuelve UPPERCASE para compatibilidad con Panel API.
     */
    @JvmStatic
    fun getDeviceId(context: Context): String {
        val mac = MacAddressProvider.getMac(context).uppercase()
        Log.d(TAG, "Device ID (MAC): $mac")
        return mac
    }
}

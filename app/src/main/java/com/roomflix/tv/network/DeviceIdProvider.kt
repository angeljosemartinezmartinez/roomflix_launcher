package com.roomflix.tv.network

import android.content.Context
import android.provider.Settings
import android.util.Log

object DeviceIdProvider {

    @JvmStatic
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        val last12 = androidId.takeLast(12).padStart(12, '0').uppercase()
        Log.d("DEVICE_ID", "Virtual ID (12) desde ANDROID_ID: $last12")
        return last12
    }
}

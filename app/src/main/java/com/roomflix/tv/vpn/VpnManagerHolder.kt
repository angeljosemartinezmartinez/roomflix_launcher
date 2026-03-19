package com.roomflix.tv.vpn

import android.content.Context

object VpnManagerHolder {
    @Volatile
    private var instance: VpnManager? = null

    fun getInstance(context: Context): VpnManager {
        return instance ?: synchronized(this) {
            instance ?: VpnManager(context.applicationContext).also { instance = it }
        }
    }
}

package com.roomflix.tv.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log

class VpnPermissionActivity : Activity() {

    companion object {
        private const val TAG = "RoomFlix.VPN"
        const val REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = VpnService.prepare(this)
        if (intent != null) {
            Log.i(TAG, "Solicitando permiso VPN al usuario")
            startActivityForResult(intent, REQUEST_CODE)
        } else {
            Log.i(TAG, "Permiso VPN ya concedido")
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onActivityResult(req: Int, result: Int, data: Intent?) {
        super.onActivityResult(req, result, data)
        if (req == REQUEST_CODE) {
            Log.i(TAG, "Permiso VPN: ${if (result == RESULT_OK) "CONCEDIDO" else "DENEGADO"}")
            setResult(if (result == RESULT_OK) RESULT_OK else RESULT_CANCELED)
            finish()
        }
    }
}

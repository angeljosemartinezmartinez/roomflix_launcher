package com.roomflix.tv.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.roomflix.tv.views.activities.MainMenu

/**
 * BootReceiver - BroadcastReceiver para auto-lanzar la app al encender la TV
 * 
 * Escucha ACTION_BOOT_COMPLETED y ACTION_LOCKED_BOOT_COMPLETED para asegurar
 * que la app se lance automáticamente al arrancar el dispositivo
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.ACTION_LOCKED_BOOT_COMPLETED" -> {
                Log.d(TAG, "Boot completado. Lanzando MainMenu...")
                launchMainMenu(context)
            }
            else -> {
                Log.d(TAG, "Acción desconocida: ${intent.action}")
            }
        }
    }

    /**
     * Lanza MainMenu al arrancar el dispositivo
     */
    private fun launchMainMenu(context: Context) {
        try {
            val intent = Intent(context, MainMenu::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            context.startActivity(intent)
            Log.d(TAG, "MainMenu lanzada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar MainMenu: ${e.message}", e)
        }
    }
}
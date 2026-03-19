package com.roomflix.tv.mdm

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class RoomflixDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "RoomflixDPC"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, RoomflixDeviceAdminReceiver::class.java)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Log.d(TAG, "Device Admin habilitado")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        Log.d(TAG, "Provisioning completado — configurando dispositivo RoomFlix")
        setupDevice(context)
    }

    private fun setupDevice(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as DevicePolicyManager
        val adminComponent = getComponentName(context)

        try {
            // 1. Establecer launcher RoomFlix como home screen permanente
            val launcherIntent = android.content.IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(
                adminComponent,
                launcherIntent,
                ComponentName(context.packageName, "com.roomflix.tv.views.activities.MainMenu")
            )

            // 2. Activar kiosk mode — solo RoomFlix accesible
            dpm.setLockTaskPackages(
                adminComponent,
                arrayOf(context.packageName)
            )

            // 3. Mantener pantalla encendida siempre
            dpm.setGlobalSetting(
                adminComponent,
                android.provider.Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                "3"
            )

            // 4. Deshabilitar barra de estado
            dpm.setStatusBarDisabled(adminComponent, true)

            Log.d(TAG, "Dispositivo configurado correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error configurando dispositivo: ${e.message}")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "Acción recibida: ${intent.action}")
    }
}

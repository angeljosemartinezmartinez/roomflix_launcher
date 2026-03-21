package com.roomflix.tv.remote

import android.content.Context
import android.util.Log
import android.view.InputDevice

object BluetoothRemoteManager {

    private const val TAG = "RoomFlix.Remote"

    fun hasRemoteConnected(context: Context): Boolean {
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            if (isExternalRemote(device)) {
                Log.i(TAG, "Mando detectado: ${device.name}")
                return true
            }
        }
        Log.w(TAG, "No se detecto ningun mando conectado")
        return false
    }

    fun getConnectedRemotes(context: Context): List<String> {
        val remotes = mutableListOf<String>()
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            if (isExternalRemote(device)) {
                remotes.add(device.name)
            }
        }
        return remotes
    }

    fun recordRemoteActivity(context: Context) {
        context.getSharedPreferences("roomflix_remote", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_activity_ms", System.currentTimeMillis())
            .apply()
    }

    fun daysSinceLastActivity(context: Context): Int {
        val lastMs = context.getSharedPreferences("roomflix_remote", Context.MODE_PRIVATE)
            .getLong("last_activity_ms", -1L)
        if (lastMs == -1L) return -1
        return ((System.currentTimeMillis() - lastMs) / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun isExternalRemote(device: InputDevice): Boolean {
        val sources = device.sources
        val hasDpad = (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        val hasKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
        val hasGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD

        val name = device.name.lowercase()
        val isBuiltIn = name.contains("hdmi") || name.contains("cec") ||
            name.contains("amlogic") || name.contains("virtual") ||
            name.contains("gpio") || name.contains("adc") ||
            name.contains("ir_keypad")

        return !isBuiltIn && (hasDpad || hasKeyboard || hasGamepad)
    }
}

package com.roomflix.tv.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.net.NetworkInterface

object MacAddressProvider {

    private const val TAG = "RoomFlix.MAC"

    /**
     * Obtiene la MAC address del dispositivo.
     * Prioridad: wlan0 → cualquier interfaz → WifiManager → fallback ANDROID_ID.
     * Devuelve 12 chars lowercase sin separadores. Ejemplo: "703e97f4efe0"
     */
    fun getMac(context: Context): String {
        // Intento 1: leer /sys/class/net/wlan0/address (funciona en muchos TV)
        getMacFromSysfs("wlan0")?.let {
            Log.i(TAG, "MAC de sysfs wlan0: $it")
            return it
        }

        getMacFromSysfs("eth0")?.let {
            Log.i(TAG, "MAC de sysfs eth0: $it")
            return it
        }

        // Intento 2: NetworkInterface Java API
        getMacFromInterface("wlan0")?.let {
            Log.i(TAG, "MAC de NetworkInterface wlan0: $it")
            return it
        }

        getMacFromAnyInterface()?.let {
            Log.i(TAG, "MAC de interfaz de red: $it")
            return it
        }

        // Intento 3: WifiManager (deprecated pero funciona en algunos dispositivos)
        getMacFromWifiManager(context)?.let {
            Log.i(TAG, "MAC de WifiManager: $it")
            return it
        }

        // Intento 4: ejecutar ip addr show (funciona en Android TV)
        getMacFromIpCommand("wlan0")?.let {
            Log.i(TAG, "MAC de ip addr wlan0: $it")
            return it
        }

        getMacFromIpCommand("eth0")?.let {
            Log.i(TAG, "MAC de ip addr eth0: $it")
            return it
        }

        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
        ) ?: "000000000000"
        val fallback = androidId.takeLast(12).padStart(12, '0').lowercase()
        Log.w(TAG, "MAC no disponible, fallback ANDROID_ID: $fallback")
        return fallback
    }

    private fun getMacFromInterface(name: String): String? {
        return try {
            val nif = NetworkInterface.getByName(name) ?: return null
            val hw = nif.hardwareAddress ?: return null
            if (hw.size != 6) return null
            val mac = hw.joinToString("") { String.format("%02x", it) }
            if (mac == "000000000000" || mac == "ffffffffffff") null else mac
        } catch (e: Exception) {
            Log.w(TAG, "Error $name: ${e.message}")
            null
        }
    }

    private fun getMacFromAnyInterface(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (nif in interfaces.asSequence()) {
                if (nif.isLoopback || nif.name.startsWith("lo") || nif.name.startsWith("dummy") || nif.name.startsWith("tun")) continue
                val hw = nif.hardwareAddress ?: continue
                if (hw.size != 6) continue
                val mac = hw.joinToString("") { String.format("%02x", it) }
                if (mac == "000000000000" || mac == "ffffffffffff") continue
                return mac
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error interfaces: ${e.message}")
            null
        }
    }

    private fun getMacFromSysfs(interfaceName: String): String? {
        return try {
            val reader = BufferedReader(FileReader("/sys/class/net/$interfaceName/address"))
            val line = reader.readLine()?.trim()?.lowercase() ?: return null
            reader.close()
            val mac = line.replace(":", "").replace("-", "")
            if (mac.length != 12 || mac == "000000000000" || mac == "ffffffffffff") null else mac
        } catch (e: Exception) {
            Log.d(TAG, "sysfs $interfaceName no disponible: ${e.message}")
            null
        }
    }

    private fun getMacFromIpCommand(interfaceName: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/ip", "addr", "show", interfaceName))
            val output = process.inputStream.bufferedReader().readText()
            val errOutput = process.errorStream.bufferedReader().readText()
            process.waitFor()
            Log.d(TAG, "ip addr $interfaceName exit=${process.exitValue()} out=${output.take(200)} err=${errOutput.take(100)}")
            val regex = Regex("link/ether\\s+([0-9a-fA-F:]{17})")
            val match = regex.find(output) ?: return null
            val mac = match.groupValues[1].replace(":", "").lowercase()
            if (mac.length != 12 || mac == "000000000000" || mac == "ffffffffffff") null else mac
        } catch (e: Exception) {
            Log.w(TAG, "ip addr $interfaceName exception: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getMacFromWifiManager(context: Context): String? {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            val info = wm.connectionInfo ?: return null
            val macStr = info.macAddress ?: return null
            if (macStr == "02:00:00:00:00:00") return null
            val mac = macStr.replace(":", "").replace("-", "").lowercase()
            if (mac.length != 12 || mac == "000000000000") null else mac
        } catch (e: Exception) {
            Log.w(TAG, "Error WifiManager: ${e.message}")
            null
        }
    }
}

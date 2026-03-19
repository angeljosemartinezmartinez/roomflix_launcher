package com.roomflix.tv.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface as WgInterface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class VpnManager(private val context: Context) {

    companion object {
        private const val TAG = "RoomFlix.VPN"
        private const val TUNNEL_NAME = "roomflix-control"
        private const val PREFS_NAME = "roomflix_vpn"
        private const val KEY_PRIVATE = "vpn_private_key"
        private const val KEY_ADDRESS = "vpn_address"
        private const val KEY_SRV_PUB = "vpn_server_pubkey"
        private const val KEY_SRV_EP = "vpn_server_endpoint"
        private const val KEY_CONFIGURED = "vpn_configured"

        // SPLIT TUNNEL: solo esta subred va por VPN
        private const val VPN_SUBNET = "10.10.0.0/24"

        // IP VPN del servidor Control
        const val CONTROL_VPN_IP = "10.10.0.1"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var backend: GoBackend? = null
    private var activeTunnel: Tunnel? = null

    var isConnected = false
        private set

    fun configure(
        privateKey: String,
        address: String,
        serverPublicKey: String,
        serverEndpoint: String
    ) {
        prefs.edit()
            .putString(KEY_PRIVATE, privateKey)
            .putString(KEY_ADDRESS, address)
            .putString(KEY_SRV_PUB, serverPublicKey)
            .putString(KEY_SRV_EP, serverEndpoint)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
        Log.i(TAG, "VPN configurada: $address -> $serverEndpoint")
    }

    fun isConfigured() = prefs.getBoolean(KEY_CONFIGURED, false)

    fun prepareVpnPermission(): Intent? = VpnService.prepare(context)

    suspend fun connect(): VpnResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.w(TAG, "VPN no configurada")
            return@withContext VpnResult.NotConfigured
        }

        try {
            val privateKeyStr = prefs.getString(KEY_PRIVATE, "")!!
            val addressStr = prefs.getString(KEY_ADDRESS, "")!!
            val srvPubStr = prefs.getString(KEY_SRV_PUB, "")!!
            val srvEpStr = prefs.getString(KEY_SRV_EP, "")!!

            val keyPair = KeyPair(Key.fromBase64(privateKeyStr))
            val wgIface = WgInterface.Builder()
                .setKeyPair(keyPair)
                .addAddress(InetNetwork.parse(addressStr))
                // Sin DNS forzado — TV usa DNS del hotel
                .build()

            val peer = Peer.Builder()
                .setPublicKey(Key.fromBase64(srvPubStr))
                .setEndpoint(InetEndpoint.parse(srvEpStr))
                .addAllowedIp(InetNetwork.parse(VPN_SUBNET))
                .setPersistentKeepalive(25)
                .build()

            val config = Config.Builder()
                .setInterface(wgIface)
                .addPeer(peer)
                .build()

            if (backend == null) {
                backend = GoBackend(context)
            }

            val tunnel = object : Tunnel {
                override fun getName() = TUNNEL_NAME
                override fun onStateChange(state: Tunnel.State) {
                    isConnected = state == Tunnel.State.UP
                    Log.i(TAG, "Tunel VPN: $state")
                }
            }

            backend!!.setState(tunnel, Tunnel.State.UP, config)
            activeTunnel = tunnel
            isConnected = true

            Log.i(TAG, "VPN activa: $addressStr (split tunnel: $VPN_SUBNET)")
            VpnResult.Connected(addressStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error VPN: ${e.message}", e)
            isConnected = false
            VpnResult.Error(e.message ?: "Error desconocido")
        }
    }

    fun disconnect() {
        try {
            activeTunnel?.let { backend?.setState(it, Tunnel.State.DOWN, null) }
            isConnected = false
            Log.i(TAG, "VPN desconectada")
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando: ${e.message}")
        }
    }

    suspend fun pingServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(CONTROL_VPN_IP).isReachable(3000)
        } catch (e: Exception) {
            false
        }
    }

    sealed class VpnResult {
        data class Connected(val vpnIp: String) : VpnResult()
        data class Error(val message: String) : VpnResult()
        object NotConfigured : VpnResult()
    }
}

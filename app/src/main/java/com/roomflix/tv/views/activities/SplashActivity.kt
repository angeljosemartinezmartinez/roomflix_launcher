package com.roomflix.tv.views.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.roomflix.tv.BuildConfig
import com.roomflix.tv.R
import com.roomflix.tv.remote.BluetoothRemoteManager
import com.roomflix.tv.remote.RemotePairingActivity
import com.roomflix.tv.viewmodel.SplashViewModel
import com.roomflix.tv.vpn.VpnPermissionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest

/**
 * SplashActivity - Pantalla de carga inicial
 *
 * Flujo: Splash -> carga de datos (ViewModel) -> MainMenu.
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val DELAY_ON_ERROR_MS = 2000L
    }

    private val viewModel: SplashViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var versionText: TextView
    private lateinit var statusText: TextView
    private lateinit var registrationText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar = findViewById(R.id.progressBar)
        versionText = findViewById(R.id.versionText)
        statusText = findViewById(R.id.statusText)
        registrationText = findViewById(R.id.registrationText)

        versionText.text = "v${BuildConfig.VERSION_NAME}"
        statusText.text = ""
        statusText.visibility = View.GONE
        registrationText.visibility = View.GONE

        observeLoadingState()
        observeVpnState()

        lifecycleScope.launch {
            try {
                if (!isFinishing) viewModel.loadAllData()
            } catch (e: Exception) {
                Log.e(TAG, "Error en carga inicial", e)
                withContext(Dispatchers.Main) {
                    delay(DELAY_ON_ERROR_MS)
                    // Si hay ID de registro, quedarse en Splash mostrando el mensaje
                    if (!isFinishing && viewModel.registrationIdToShow.value.isNullOrBlank()) {
                        navigateToMainMenu()
                    }
                }
            }
        }
    }

    private fun observeLoadingState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registrationIdToShow.collectLatest { id ->
                    if (!id.isNullOrBlank()) {
                        progressBar.visibility = View.GONE
                        statusText.visibility = View.GONE
                        registrationText.text = "EQUIPO NO REGISTRADO - ID: $id"
                        registrationText.visibility = View.VISIBLE
                    } else {
                        registrationText.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingMessage.collectLatest { message ->
                    if (message.isNotBlank()) {
                        statusText.text = message
                        statusText.visibility = View.VISIBLE
                    } else {
                        statusText.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingState.collectLatest { state ->
                    // Si el equipo no está registrado, NO avanzar al Main Menu
                    if (!viewModel.registrationIdToShow.value.isNullOrBlank()) return@collectLatest
                    when (state) {
                        is SplashViewModel.LoadingState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            progressBar.isIndeterminate = true
                            statusText.visibility = View.VISIBLE
                        }
                        is SplashViewModel.LoadingState.Success -> navigateToMainMenu()
                        is SplashViewModel.LoadingState.Error -> {
                            progressBar.visibility = View.GONE
                            statusText.text = "Error de conexión"
                            statusText.visibility = View.VISIBLE
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (!isFinishing && viewModel.registrationIdToShow.value.isNullOrBlank()) {
                                    navigateToMainMenu()
                                }
                            }, 5000)
                        }
                        is SplashViewModel.LoadingState.Idle -> statusText.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeVpnState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.needsVpnPermission.collectLatest { needs ->
                    if (needs) {
                        startActivityForResult(
                            Intent(this@SplashActivity, VpnPermissionActivity::class.java),
                            VpnPermissionActivity.REQUEST_CODE
                        )
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vpnState.collectLatest { result ->
                    if (result != null) {
                        Log.i(TAG, "VPN state: $result")
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            VpnPermissionActivity.REQUEST_CODE -> {
                if (resultCode == RESULT_OK) viewModel.connectVpn()
                else Log.w(TAG, "Permiso VPN denegado - funcionando sin VPN")
            }
            RemotePairingActivity.REQUEST_CODE -> {
                // Tanto si emparejo como si omitio -> ir al menu
                Log.i(TAG, "Pairing result: ${if (resultCode == RESULT_OK) "OK" else "SKIP"}")
                goToMainMenu()
            }
        }
    }

    private fun navigateToMainMenu() {
        // Verificar si hay mando BT conectado antes de ir al menu
        if (!BluetoothRemoteManager.hasRemoteConnected(this)) {
            Log.w(TAG, "Sin mando detectado -> lanzando asistente")
            startActivityForResult(
                Intent(this, RemotePairingActivity::class.java),
                RemotePairingActivity.REQUEST_CODE
            )
        } else {
            Log.i(TAG, "Mando OK -> ir al menu")
            goToMainMenu()
        }
    }

    private fun goToMainMenu() {
        val intent = Intent(this, MainMenu::class.java)
        intent.putExtra("from_splash", true)
        startActivity(intent)
        finish()
    }
}

package com.roomflix.tv.remote

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class RemotePairingActivity : Activity() {

    companion object {
        private const val TAG = "RoomFlix.Pairing"
        const val REQUEST_CODE = 2001
        private const val CHECK_MS = 2000L
        private const val MAX_WAIT_MS = 60000L
    }

    private var elapsedMs = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvTitle: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSkip: Button
    private lateinit var btnRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUI()

        val bt = BluetoothAdapter.getDefaultAdapter()
        if (bt == null) {
            setResult(RESULT_CANCELED); finish(); return
        }
        @Suppress("DEPRECATION")
        if (!bt.isEnabled) bt.enable()

        showSearching()
        startLoop()
    }

    private fun buildUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 60, 80, 60)
            setBackgroundColor(0xFF1A1F2E.toInt())
        }

        tvTitle = TextView(this).apply { textSize = 28f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER; setPadding(0, 0, 0, 20) }
        tvInstructions = TextView(this).apply { textSize = 18f; setTextColor(0xFF94A3B8.toInt()); gravity = Gravity.CENTER; setPadding(0, 0, 0, 40) }
        tvStatus = TextView(this).apply { textSize = 16f; setTextColor(0xFFFF6B35.toInt()); gravity = Gravity.CENTER; setPadding(0, 0, 0, 40) }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(600, LinearLayout.LayoutParams.WRAP_CONTENT)
            max = (MAX_WAIT_MS / 1000).toInt()
            visibility = View.GONE
        }

        btnSkip = Button(this).apply {
            text = "Continuar sin mando"
            setBackgroundColor(0xFF32394E.toInt()); setTextColor(0xFF94A3B8.toInt())
            setPadding(40, 20, 40, 20)
            setOnClickListener { setResult(RESULT_CANCELED); finish() }
        }

        btnRetry = Button(this).apply {
            text = "Buscar de nuevo"
            setBackgroundColor(0xFFFF6B35.toInt()); setTextColor(0xFFFFFFFF.toInt())
            setPadding(40, 20, 40, 20); visibility = View.GONE
            setOnClickListener { elapsedMs = 0; showSearching(); startLoop() }
        }

        layout.addView(tvTitle); layout.addView(tvInstructions); layout.addView(tvStatus)
        layout.addView(progressBar); layout.addView(btnSkip); layout.addView(btnRetry)
        setContentView(layout)
    }

    private fun showSearching() {
        tvTitle.text = "Conectar mando"
        tvInstructions.text = "No se detecta ningun mando conectado.\n\nPara conectar tu mando G10 o G20:\n\n1. Enciende el mando\n2. Manten pulsado el boton de emparejamiento\n   hasta que el LED parpadee rapido\n3. Espera a que aparezca el dialogo\n4. Acepta la conexion"
        tvStatus.text = "Buscando mando..."
        progressBar.visibility = View.VISIBLE; progressBar.progress = 0
        btnRetry.visibility = View.GONE
    }

    private fun startLoop() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(::check, CHECK_MS)
    }

    private fun check() {
        elapsedMs += CHECK_MS
        progressBar.progress = (elapsedMs / 1000).toInt()

        if (BluetoothRemoteManager.hasRemoteConnected(this)) {
            progressBar.visibility = View.GONE
            tvTitle.text = "Mando conectado!"
            tvStatus.text = "El mando esta listo para usar"
            tvInstructions.text = ""
            btnSkip.visibility = View.GONE
            handler.postDelayed({ setResult(RESULT_OK); finish() }, 2000)
            return
        }

        tvStatus.text = when {
            elapsedMs < 15000 -> "Buscando mando..."
            elapsedMs < 30000 -> "Asegurate de que el LED parpadea rapido"
            elapsedMs < 45000 -> "Necesitas ayuda? Llama a recepcion"
            else -> "Tiempo agotado"
        }

        if (elapsedMs >= MAX_WAIT_MS) {
            progressBar.visibility = View.GONE
            tvTitle.text = "Sin respuesta del mando"
            tvInstructions.text = "Puedes intentarlo de nuevo o continuar sin mando."
            tvStatus.text = ""; btnRetry.visibility = View.VISIBLE
        } else {
            handler.postDelayed(::check, CHECK_MS)
        }
    }

    override fun onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null) }
}

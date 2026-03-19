package com.roomflix.tv.views.activities

import android.app.AlarmManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.databinding.DataBindingUtil
import com.roomflix.tv.BuildConfig
import com.roomflix.tv.Constants
import com.roomflix.tv.R
import com.roomflix.tv.database.tables.Translations
import com.roomflix.tv.database.tables.Languages
import com.roomflix.tv.databinding.ActivityMainBinding
import com.roomflix.tv.listener.CallBackAllInfoCheck
import com.roomflix.tv.listener.CallBackArrayList
import com.roomflix.tv.listener.CallBackCheckConnection
import com.roomflix.tv.model.Button as RoomflixButton
import com.roomflix.tv.model.FunctionType
import com.roomflix.tv.utils.Utils
import com.roomflix.tv.viewmodel.MainMenuViewModel
import com.roomflix.tv.network.DeviceIdProvider
import com.roomflix.tv.epg.EpgManager
import com.roomflix.tv.repository.EpgRepository
import com.roomflix.tv.utils.WeatherIconMapper
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.typeface.library.weathericons.WeatherIcons
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import java.util.Locale
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import android.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.transition.TransitionManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.view.KeyEvent
import android.view.ViewTreeObserver
import android.content.SharedPreferences
import com.roomflix.tv.views.fragment.MoreAppsCarouselDialogFragment

/**
 * MainMenu - Activity principal del launcher Roomflix para Android TV
 * 
 * Gestiona el menú principal con botones dinámicos y video de fondo
 */
class MainMenu : NetworkBaseActivity() {

    companion object {
        private const val TAG = "MainMenu"
        @JvmStatic
        var context: Context? = null
            private set
    }

    /**
     * FORZAR LOCALE EN EL CONTEXTO: Aplica el idioma guardado ANTES de que la Activity se cree
     * Esto garantiza que el idioma persista al cambiar de pantalla (ej: volver del Player)
     */
    override fun attachBaseContext(newBase: Context) {
        // Mismo archivo que mySharedPreferences (SharedPreferencesModule provee "hotelPlay")
        val prefs = newBase.getSharedPreferences("hotelPlay", Context.MODE_PRIVATE)
        val lang = prefs.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, "en") ?: "en"
        val locale = Locale(lang.lowercase())
        val context = Utils.createContextWithLocale(newBase, locale)
        super.attachBaseContext(context)
    }

    private lateinit var binding: ActivityMainBinding
    private var buttons: ArrayList<RoomflixButton> = ArrayList()
    private var baseUrl: String = ""
    private var background: String = ""
    private var langID: String = ""
    private var mPicturesList: ArrayList<Translations> = ArrayList()
    private var lastKeyClick: Long = 0
    private var code: String = "" // Código numérico para secuencias de teclas
    private var player: ExoPlayer? = null
    private var currentStreamUrl: String = ""
    private var timeSet: Boolean = false // if timeformat is change or not
    private var executingCall: Boolean = false
    private var lastTime: Long = 0
    private val waitForNetwork = Handler(Looper.getMainLooper())
    private var restart: Boolean = false
    private var isOfflineMode: Boolean = false
    private var retryJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isLanguagePillExpanded: Boolean = false
    private var pillLanguages: List<Languages> = emptyList()
    private var languagesPillAdapter: LanguagesPillAdapter? = null
    
    // ViewModel para limpieza de datos y clima (TopInfoBar). Usamos ViewModelProvider para que el mismo
    // instance pueda usarse en MoreAppsCarouselDialogFragment (activityViewModels).
    private val viewModel: MainMenuViewModel by lazy {
        ViewModelProvider(this).get(MainMenuViewModel::class.java)
    }
    
    // Job para actualización periódica de hora en TopInfoBar
    private var topInfoBarUpdateJob: Job? = null
    
    // Secuencia Backdoor Ajustes (ARRIBA, ARRIBA, ARRIBA, ABAJO, DERECHA, OK)
    private val backdoorSequence = listOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER
    )
    
    // Buffer para secuencia backdoor
    private val backdoorBuffer = mutableListOf<Int>()
    private var lastKeyTime: Long = 0
    private val SEQUENCE_TIMEOUT_MS = 3000L // 3 segundos para completar la secuencia
    private val BUFFER_CLEAR_DELAY_MS = 2000L // 2 segundos para limpiar buffer si no hay actividad
    private var bufferClearHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        langID = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID) ?: ""
        context = this
        Utils.changeAppLanguage(Locale(langID.lowercase()), this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        executingCall = false
        restart = false
        // RELOJ ANTIGUO ELIMINADO - Ahora se usa TopInfoBar dinámico
        buttons = ArrayList()
        
        // Inicializar ViewModel (compartido con MoreAppsCarouselDialogFragment vía ViewModelProvider)
        // viewModel se accede por primera vez aquí o en observeWeatherState
        
        // ELIMINAR CARGA DUPLICADA: Solo iniciar descarga de EPG si EpgRepository está vacío
        // Si SplashActivity ya cargó los datos, NO hacer nada para evitar parpadeo a negro
        if (!EpgRepository.hasData()) {
            val epgManager = EpgManager.getInstance()
            epgManager.downloadEpgAsync(coroutineScope)
        }
        
        // Configurar formato de hora si tenemos permisos
        if (mPermissionHelper.hasWriteSettingsPermission(this)) {
            timeFormat()
        }
        
        // Inicializar servicios para TopInfoBar dinámico
        initializeTopInfoBar()
        
        // Configurar barra superior (logo, selector de idioma, botón Send)
        setupTopBar()
        
        // Foco inicial en la mini pantalla de TV (myVideo); post para que se aplique tras el layout
        binding.myVideo.post { binding.myVideo.requestFocus() }
    }
    
    /**
     * Configura la barra superior con logo, selector de idioma y botón Send
     */
    private fun setupTopBar() {
        // Cargar logo en la barra superior
        val miniLogo = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.MINI_LOGO) ?: ""
        if (miniLogo.isNotEmpty()) {
            imageHelper.loadRoundCorner(miniLogo, binding.topBarLogo)
        }
        
        // Configurar botón de selector de idioma
        setupLanguageSelector()
        
        // Configurar botón Send
        setupSendButton()
    }
    
    /**
     * Configura el botón selector de idioma con bandera y código
     */
    private fun setupLanguageSelector() {
        // Preparar RecyclerView horizontal de la píldora (una vez)
        if (languagesPillAdapter == null) {
            binding.languagesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            languagesPillAdapter = LanguagesPillAdapter()
            binding.languagesRecyclerView.adapter = languagesPillAdapter
        }

        // Estado inicial: contraído; RecyclerView no recibe foco (bloqueo en XML: focusable=false, blocksDescendants)
        isLanguagePillExpanded = false
        binding.languagesRecyclerView.isFocusable = false
        binding.languagesRecyclerView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        applyLanguagePillExpandedState(false)
        loadLanguagesIntoPill()

        // Clic en el contenedor (círculo) expande la píldora cuando está colapsada
        binding.languagePillContainer.setOnClickListener {
            if (!isLanguagePillExpanded) {
                if (pillLanguages.isEmpty()) {
                    loadLanguagesIntoPill {
                        toggleLanguagePill(forceExpand = true)
                        Unit
                    }
                } else {
                    toggleLanguagePill(forceExpand = true)
                }
            }
        }

        // Foco colapsado: bandera crece hasta el tamaño de la píldora (40dp); sin pintar blanco
        binding.languagePillContainer.setOnFocusChangeListener { _, hasFocus ->
            if (!isLanguagePillExpanded) {
                val scale = if (hasFocus) 40f / 30f else 1f
                val runScale: () -> Unit = {
                    val idx = languagesPillAdapter?.getSelectedLanguagePosition() ?: 0
                    binding.languagesRecyclerView.findViewHolderForAdapterPosition(idx)?.itemView
                        ?.findViewById<ImageView>(R.id.languageFlag)
                        ?.let { flag ->
                            flag.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
                        }
                    Unit
                }
                if (hasFocus) binding.languagesRecyclerView.post(runScale) else runScale()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Auto-colapso: cuando el foco sale de la píldora, contraer
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener(globalFocusChangeListener)
    }

    override fun onStop() {
        try {
            window.decorView.viewTreeObserver.removeOnGlobalFocusChangeListener(globalFocusChangeListener)
        } catch (_: Exception) { }
        super.onStop()
    }

    private val globalFocusChangeListener = ViewTreeObserver.OnGlobalFocusChangeListener { _: View?, newFocus: View? ->
        if (isLanguagePillExpanded && newFocus != null) {
            if (!isViewInLanguageContainer(newFocus)) {
                toggleLanguagePill(forceExpand = false)
            }
        }
    }

    /**
     * Verifica si una vista está dentro del contenedor de idiomas (incluyendo hijos)
     * Usa comparación por ID para evitar ambigüedades de tipo
     */
    private fun isViewInLanguageContainer(view: View?): Boolean {
        if (view == null) return false
        var current: View? = view
        val containerId = binding.languagePillContainer.id
        while (current != null) {
            if (current.id == containerId) return true
            val parent = current.parent
            current = if (parent is View) parent else null
        }
        return false
    }

    private fun toggleLanguagePill(forceExpand: Boolean? = null) {
        val shouldExpand = forceExpand ?: !isLanguagePillExpanded
        TransitionManager.beginDelayedTransition(binding.topBar)

        isLanguagePillExpanded = shouldExpand
        applyLanguagePillExpandedState(shouldExpand)

        if (shouldExpand) {
            binding.topBar.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            val idx = languagesPillAdapter?.getSelectedLanguagePosition() ?: 0
            binding.languagesRecyclerView.findViewHolderForAdapterPosition(idx)?.itemView
                ?.findViewById<ImageView>(R.id.languageFlag)
                ?.let { it.animate().scaleX(1f).scaleY(1f).setDuration(150).start() }
            binding.languagesRecyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            binding.languagePillContainer.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            binding.languagesRecyclerView.isFocusable = true
            binding.languagesRecyclerView.isFocusableInTouchMode = true
            focusSelectedLanguageFlag()
        } else {
            binding.topBar.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            binding.languagesRecyclerView.isFocusable = false
            binding.languagesRecyclerView.isFocusableInTouchMode = false
            binding.languagesRecyclerView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            binding.languagePillContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            binding.languagePillContainer.clearFocus()
            binding.languagesRecyclerView.clearFocus()
            binding.btn10.requestFocus()
        }
    }

    /** Aplica ancho del container: 40dp (círculo) colapsado, WRAP_CONTENT expandido. Animado con TransitionManager en toggleLanguagePill. */
    private fun applyLanguagePillExpandedState(expanded: Boolean) {
        val dp40 = (40 * resources.displayMetrics.density).toInt()
        binding.languagePillContainer.layoutParams = binding.languagePillContainer.layoutParams?.apply {
            width = if (expanded) ViewGroup.LayoutParams.WRAP_CONTENT else dp40
        } ?: return
        binding.languagesRecyclerView.isHorizontalScrollBarEnabled = expanded
        binding.languagesRecyclerView.overScrollMode = if (expanded) View.OVER_SCROLL_ALWAYS else View.OVER_SCROLL_NEVER
        if (!expanded) {
            val selectedIndex = languagesPillAdapter?.getSelectedLanguagePosition() ?: 0
            binding.languagesRecyclerView.post {
                (binding.languagesRecyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(selectedIndex, 0)
            }
        }
    }

    private fun loadLanguagesIntoPill(onDone: (() -> Unit)? = null) {
        mDBManager.getLanguages(object : CallBackArrayList<Languages> {
            override fun finish(s: ArrayList<Languages>?) {
                runOnUiThread {
                    pillLanguages = (s ?: arrayListOf()).sortedBy { it.id ?: 0 }
                    languagesPillAdapter?.submit(pillLanguages)
                    languagesPillAdapter?.notifyDataSetChanged()
                    val idx = languagesPillAdapter?.getSelectedLanguagePosition() ?: 0
                    binding.languagesRecyclerView.scrollToPosition(idx)
                    binding.languagesRecyclerView.post { binding.languagesRecyclerView.scrollToPosition(idx) }
                    onDone?.invoke()
                }
            }

            override fun error(localizedMessage: String) {
                runOnUiThread {
                    Log.e(TAG, "Error al obtener idiomas (pill): $localizedMessage")
                    pillLanguages = emptyList()
                    languagesPillAdapter?.submit(pillLanguages)
                    onDone?.invoke()
                }
            }
        }, this)
    }

    /** Desplaza al idioma guardado con scroll de precisión y pide foco en esa bandera (la ya visible). */
    private fun focusSelectedLanguageFlag() {
        val selectedPos = languagesPillAdapter?.getSelectedLanguagePosition() ?: 0
        binding.languagesRecyclerView.post {
            (binding.languagesRecyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(selectedPos, 0)
            binding.languagesRecyclerView.postDelayed({
                binding.languagesRecyclerView.findViewHolderForAdapterPosition(selectedPos)?.itemView?.requestFocus()
            }, 80)
        }
    }

    private inner class LanguagesPillAdapter : RecyclerView.Adapter<LanguagesPillAdapter.VH>() {
        private var items: List<Languages> = emptyList()

        /** Lista completa sin duplicados y orden fijo por id para que nunca cambien de sitio. */
        fun submit(newItems: List<Languages>) {
            items = newItems.distinctBy { (it.code ?: "").lowercase() }.sortedBy { it.id ?: 0 }
            notifyDataSetChanged()
        }

        /** Índice del idioma guardado en prefs; si no está en la lista, 0 (bind usa URL_LANG como fallback visual). */
        fun getSelectedLanguagePosition(): Int {
            val current = (mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID) ?: "en").lowercase()
            val idx = items.indexOfFirst { (it.code ?: "").lowercase() == current }
            return if (idx >= 0) idx else 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.view_language_flag, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items.getOrNull(position))
        }

        override fun getItemCount(): Int = items.size

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val flag: ImageView = itemView.findViewById(R.id.languageFlag)

            fun bind(item: Languages?) {
                // Click: contraído -> expandir; expandido -> solo entonces seleccionar idioma (evita re-expansión por contenedor)
                itemView.setOnClickListener {
                    if (!isLanguagePillExpanded) {
                        if (pillLanguages.isEmpty()) {
                            loadLanguagesIntoPill {
                                toggleLanguagePill(forceExpand = true)
                                Unit
                            }
                        } else {
                            toggleLanguagePill(forceExpand = true)
                        }
                        return@setOnClickListener
                    }

                    // Solo ejecutar selección cuando la píldora está expandida (llegamos aquí solo si ya estaba expandida)
                    val selected = item ?: return@setOnClickListener
                    val langCode = (selected.code ?: "en").lowercase()
                    val picture = selected.picture ?: ""
                    val channel = selected.channel ?: ""

                    binding.languagesRecyclerView.clearFocus()
                    binding.languagePillContainer.requestFocus()
                    selectLanguage(langCode, picture, channel)
                    toggleLanguagePill(forceExpand = false)
                }

                // Render: circular flag
                val baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL) ?: ""
                val url = item?.picture?.let { if (it.isNotEmpty()) baseUrl + it else "" } ?: ""

                if (url.isNotEmpty()) {
                    Glide.with(this@MainMenu)
                        .load(url)
                        .apply(RequestOptions().circleCrop())
                        .into(flag)
                } else {
                    // Fallback: usar URL_LANG (el icono actual persistido) cuando no hay lista
                    val persisted = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_LANG) ?: ""
                    if (persisted.isNotEmpty()) {
                        Glide.with(this@MainMenu)
                            .load(persisted)
                            .apply(RequestOptions().circleCrop())
                            .into(flag)
                    } else {
                        flag.setImageDrawable(null)
                    }
                }

                // Muros de foco: evitar que IZQ/DER salgan de la píldora; ABAJO colapsa y deja bajar
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (bindingAdapterPosition == 0) return@setOnKeyListener true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (bindingAdapterPosition == itemCount - 1) return@setOnKeyListener true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (isLanguagePillExpanded) {
                                    toggleLanguagePill(forceExpand = false)
                                    binding.btn10.requestFocus()
                                }
                                return@setOnKeyListener true
                            }
                        }
                    }
                    false
                }
            }
        }
    }
    
    /**
     * Actualiza el icono circular del idioma actual desde la URL almacenada en SharedPreferences
     */
    private fun updateLanguageFlagIcon(langCode: String) {
        val pos = languagesPillAdapter?.getSelectedLanguagePosition() ?: 0
        languagesPillAdapter?.notifyItemChanged(pos)
    }
    
    /**
     * Selecciona un idioma y actualiza la app (solo desde la píldora expandible)
     */
    private fun selectLanguage(langCode: String, langPicture: String, defaultChannel: String) {
        val baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL) ?: ""
        
        // Guardar preferencias
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, langCode)
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.SELECTED_LANGUAGE_CODE, langCode)
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_LANG, baseUrl + langPicture)
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL, defaultChannel)
        
        // Actualizar Locale
        try {
            Utils.changeAppLanguage(Locale(langCode.lowercase()), this)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cambiar idioma", e)
        }
        
        updateLanguageFlagIcon(langCode)

        player?.stop()
        binding.video.player = null
        // Pequeña pausa para que termine la animación de contraer antes de recreate
        binding.languagePillContainer.postDelayed({ recreate() }, 250)
    }
    
    /**
     * Configura el botón Send / Compartir (Chromecast): animación de foco solo en el icono interno
     */
    private fun setupSendButton() {
        binding.sendButton.clipToOutline = true
        binding.sendButton.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.15f else 1.0f
            binding.sendButtonIcon.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(200)
                .start()
        }
        binding.sendButton.setOnClickListener {
            showSendDialog()
        }
    }
    
    /**
     * Muestra el diálogo informativo sobre cómo compartir contenido
     */
    private fun showSendDialog() {
        AlertDialog.Builder(this)
            .setTitle("Compartir Contenido")
            .setMessage("Usa esta función para compartir contenido desde tu dispositivo móvil a la TV.\n\n" +
                    "Asegúrate de que tu dispositivo y la TV estén en la misma red WiFi.")
            .setPositiveButton("Entendido", null)
            .setCancelable(true)
            .show()
    }
    
    /**
     * Inicializa la barra de información superior con datos dinámicos.
     * El clima se carga de forma asíncrona en MainMenuViewModel y se observa en observeWeatherState().
     */
    private fun initializeTopInfoBar() {
        observeWeatherState()
        
        // Cargar hora/fecha inmediatamente; clima lo hace el ViewModel en segundo plano
        coroutineScope.launch {
            loadTopInfoBarData()
        }
        
        topInfoBarUpdateJob = coroutineScope.launch {
            while (isActive) {
                updateTimeAndDate()
                delay(60000)
            }
        }
    }
    
    /**
     * Observa el estado de clima del ViewModel y actualiza la UI cuando llega.
     * No bloquea el arranque: el menú se dibuja de inmediato.
     */
    private fun observeWeatherState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weatherState.collect { state ->
                    updateWeatherUi(state)
                }
            }
        }
    }
    
    /**
     * Actualiza los TextView/ImageView del tiempo en la barra superior según el estado del ViewModel.
     * Si state es null, muestra un placeholder (—° y icono por defecto).
     */
    private fun updateWeatherUi(state: com.roomflix.tv.viewmodel.WeatherUiState?) {
        if (isFinishing || isDestroyed) return
        try {
            if (state != null) {
                binding.weatherTemp.text = state.temp
                val icon = WeatherIconMapper.getWeatherIcon(state.weatherCode)
                val drawable = IconicsDrawable(this, icon).apply {
                    sizeDp = 24
                    colorInt = Color.WHITE
                }
                binding.weatherIcon.setImageDrawable(drawable)
            } else {
                binding.weatherTemp.text = "—°"
                val drawable = IconicsDrawable(this, WeatherIconMapper.getWeatherIcon(3)).apply {
                    sizeDp = 24
                    colorInt = Color.WHITE
                }
                binding.weatherIcon.setImageDrawable(drawable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar UI de clima", e)
        }
    }
    
    /**
     * Carga los datos iniciales de la TopInfoBar (solo hora/fecha; el clima lo carga el ViewModel en segundo plano).
     */
    private suspend fun loadTopInfoBarData() = withContext(Dispatchers.Main) {
        try {
            updateTimeAndDate()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos de TopInfoBar", e)
        }
    }
    
    /**
     * Actualiza la hora y fecha en la TopInfoBar (infoClock + infoDate).
     *
     * Garantiza un formato compacto y sin saltos de línea extra para evitar
     * que la fecha se desborde a una tercera línea o duplique el mes.
     */
    private fun updateTimeAndDate() {
        try {
            val calendar = Calendar.getInstance()
            val locale = Locale.getDefault()

            // Hora: formato 24h "13:35"
            val timeFormat = SimpleDateFormat("HH:mm", locale)
            binding.infoClock.text = timeFormat.format(calendar.time)

            // Fecha: "MAR, 17 MAR" / "JUE, 29 ENE" según locale, sin saltos de línea
            val rawDate = formatTopBarDate(calendar.time, locale)
            binding.infoDate.text = rawDate

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar hora y fecha", e)
        }
    }

    /**
     * Devuelve una fecha compacta para la TopInfoBar.
     */
    private fun formatTopBarDate(date: Date, locale: Locale): String {
        val formatter = SimpleDateFormat("EEE, d MMM", locale)
        return formatter.format(date).uppercase()
    }
    
    /**
     * Verifica si el sistema usa formato de 24 horas
     */
    private fun is24HourFormat(): Boolean {
        return android.text.format.DateFormat.is24HourFormat(this)
    }

    private fun timeFormat() {
        val timezone = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.TIMEZONE) ?: ""

        if (timeSet) return
        try {
            Settings.System.getInt(contentResolver, Settings.System.TIME_12_24)
            Settings.System.putString(this.contentResolver, Settings.System.TIME_12_24, "12")
            timeSet = true
            val am = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (timezone.isEmpty()) {
                am.setTimeZone("Europe/Madrid")
            } else {
                am.setTimeZone(timezone)
            }
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.myVideo.invalidate()
        // SINCRONIZACIÓN DE LOCALE: Leer idioma de SharedPreferences (no de langID que puede estar desactualizado)
        val currentLangCode = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID) ?: "en"
        Utils.changeAppLanguage(Locale(currentLangCode.lowercase()), this)
        
        // Actualizar langID para mantener consistencia
        langID = currentLangCode

        // Refrescar píldora con el idioma actual
        updateLanguageFlagIcon(currentLangCode)

        // Actualizar clima al volver a la pantalla (ViewModel actualiza en segundo plano)
        viewModel.loadWeather()

        // Si venimos de SplashActivity, los datos ya están cargados en SharedPreferences y BD
        // Solo necesitamos generar el menú directamente
        if (intent.getBooleanExtra("from_splash", false)) {
            // Los datos ya están disponibles, generar el menú directamente
            generationMain()
            // NO solicitar foco en botones - el video debe mantener el foco
        } else {
            // Compatibilidad hacia atrás: cargar datos normalmente
            checkCasesConnection()
        }
        // Video player removed - handled by button functionType == 6
    }
    

    override fun onPause() {
        super.onPause()
        player?.pause()
        executingCall = false
    }

    private fun checkCasesConnection() {
        checkCasesConnection(object : CallBackCheckConnection {
            override fun success() {
                executeCall()
            }

            override fun noPing() {
                // No action needed
            }

            override fun noConnection() {
                // No action needed
            }
        })
    }

    fun executeCall() {
        // Nota: La carga inicial ahora se hace en SplashActivity
        // Este método se mantiene para compatibilidad cuando MainMenu se abre desde otras actividades
        callUpdate()
        callAllInfo(object : CallBackAllInfoCheck {
            override fun dataChange() {
                runOnUiThread {
                    // Datos cargados exitosamente desde la API: salir del modo offline
                    if (isOfflineMode) {
                        stopSilentRetry()
                    }
                    generationMain()
                    // La limpieza de privacidad se ejecuta automáticamente desde getTranslations()
                    // después de que mPicturesList esté poblada
                }
            }

            override fun dataNoChange() {
                // Datos cargados (puede ser desde caché o API): verificar si salimos del modo offline
                val isUsingCache = mySharedPreferences.getBoolean(Constants.SHARED_PREFERENCES.IS_USING_CACHE, false)
                if (isOfflineMode && !isUsingCache) {
                    // Ya no estamos usando caché: salir del modo offline
                    stopSilentRetry()
                    Log.d(TAG, "Conexión restaurada. Saliendo del modo offline.")
                }
                generationMain()
                // La limpieza de privacidad se ejecuta automáticamente desde getTranslations()
                // después de que mPicturesList esté poblada
            }

            override fun error(macAddress: String) {
                if (macAddress == "403") {
                    runOnUiThread {
                        imageHelper.loadRoundCorner(R.drawable.dispositivo_alta_es, binding.background)
                    }
                } else {
                    // Error de red o HTTP: verificar si hay caché disponible
                    val isUsingCache = mySharedPreferences.getBoolean(Constants.SHARED_PREFERENCES.IS_USING_CACHE, false)
                    if (!isUsingCache) {
                        // No hay caché disponible: mostrar diálogo de error
                        runOnUiThread {
                            showNetworkErrorDialog()
                        }
                    } else {
                        // Hay caché: activar modo offline y reintento silencioso
                        isOfflineMode = true
                        startSilentRetry()
                    }
                }
            }
        })
    }

    private fun generationMain() {
        // setDay(binding.help.day) - ELIMINADO: El reloj antiguo ya no existe, la fecha se muestra en TopInfoBar
        setMySharedPreferencesData()
        setStreaming()
        setBackground()
        setPictures()
    }

    private fun setMySharedPreferencesData() {
        langID = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID) ?: ""
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL) ?: ""
        background = baseUrl + (mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK) ?: "")
    }

    private fun setStreaming() {
        val url = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL) ?: ""
        currentStreamUrl = url
        initPlayerIfNeeded()
        setStreamUrl(url)
        setVideoView()
    }

    private fun initPlayerIfNeeded() {
        if (player == null) {
            val renderersFactory = DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000, 50000, 1500, 3000)
                .build()
            player = ExoPlayer.Builder(this)
                .setRenderersFactory(renderersFactory)
                .setLoadControl(loadControl)
                .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> runOnUiThread { binding.video.visibility = View.VISIBLE }
                            Player.STATE_ENDED -> runOnUiThread { setStreamUrl(currentStreamUrl) }
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        val msg = error.message ?: ""
                        val isRendererOrDecoding = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
                                || error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                                || error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES
                                || error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                                || error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
                                || error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
                                || msg.contains("audio", ignoreCase = true) || msg.contains("AudioRenderer", ignoreCase = true)
                        if (isRendererOrDecoding) {
                            Log.w(TAG, "ExoPlayer: posible rechazo de codec de audio (TYPE_RENDERER/decoding). errorCode=${error.errorCode}, message=$msg", error)
                        } else {
                            Log.e(TAG, "ExoPlayer onPlayerError: errorCode=${error.errorCode}, message=$msg", error)
                        }
                        runOnUiThread { if (currentStreamUrl.isNotEmpty()) setStreamUrl(currentStreamUrl) }
                    }
                })
            }
        }
    }

    private fun setStreamUrl(url: String) {
        if (url.isBlank()) return
        player?.apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = true
        }
    }

    private fun setBackground() {
        try {
            imageHelper.loadRoundCorner(background, binding.background)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setPictures() {
        // Logo eliminado de aquí - ya se carga en setupTopBar() hacia binding.topBarLogo
        getTranslations(mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID) ?: "")
    }

    private fun getTranslations(language: String) {
        val context = applicationContext
        mDBManager.getTranslationsFromLanguage(object : CallBackArrayList<Translations> {
            override fun finish(s: ArrayList<Translations>?) {
                if (s != null) {
                    mPicturesList = s
                }
                // Cargar IDs de submenús para decisión dinámica (submenú vs carrusel de apps)
                mDBManager.getSubmenuIds(object : CallBackArrayList<String> {
                    override fun finish(ids: ArrayList<String>?) {
                        viewModel.setSubmenuIds(ids ?: arrayListOf())
                        setBtnImages(mPicturesList)
                    }
                    override fun error(localizedMessage: String) {
                        setBtnImages(mPicturesList)
                    }
                }, context)
            }

            override fun error(localizedMessage: String) {
                // No action needed
            }
        }, context, language)
    }

    override fun onRestart() {
        super.onRestart()
        restart = true
        checkCasesConnection()
        player?.playWhenReady = true
        binding.video.player = player
    }

    // setClock() eliminado - ahora se usa setupClock() de BaseTVActivity

    override fun onBackPressed() {
        // Bloqueado - El usuario no puede salir de la app
        // No se ejecuta ninguna acción
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Backdoor: Detectar secuencia de teclas (UP, UP, UP, DOWN, RIGHT, OK)
        if (handleBackdoorSequence(keyCode)) {
            return true // Secuencia completada, abrir ajustes
        }
        return super.onKeyDown(keyCode, event)
    }

    fun openDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.fragment_settings)
        dialog.setTitle(R.string.app_name)
        val inputText = dialog.findViewById<EditText>(R.id.input_text)
        val btnInput = dialog.findViewById<TextView>(R.id.btn)
        btnInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                btnInput.setTextColor(getResources().getColor(R.color.orange, theme))
            } else {
                btnInput.setTextColor(getResources().getColor(R.color.md_grey_500, theme))
            }
        }
        btnInput.setOnLongClickListener { _ ->
            val text = inputText.text?.toString() ?: ""
            if (text.isNotEmpty()) {
                launchCode(text)
            }
            dialog.dismiss()
            true
        }
        dialog.show()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode
        
        // Backdoor: Detectar secuencia de teclas (ARRIBA, ARRIBA, ARRIBA, ABAJO, DERECHA, OK)
        if (action == KeyEvent.ACTION_DOWN) {
            if (handleBackdoorSequence(keyCode)) {
                return true // Secuencia completada, abrir ajustes
            }
        }
        
        // Manejar tecla CENTER/ENTER cuando el video preview tiene el foco
        if (action == KeyEvent.ACTION_DOWN && 
            (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
            // Si el video preview tiene el foco, abrir PlayerActivity
            if (binding.video.hasFocus()) {
                openFullScreenPlayer()
                return true
            }
        }
        
        // Manejo de códigos numéricos (funcionalidad existente)
        if (action == KeyEvent.ACTION_DOWN && (keyCode == 19 || keyCode == 20 || keyCode == 21 || keyCode == 22)) {
            setCode(keyCode)
            return false
        }
        return super.dispatchKeyEvent(event)
    }

    fun setCode(number: Int): Boolean {
        if (System.currentTimeMillis() - lastKeyClick < 1000) {
            code += number.toString()
            if (code.length == 14) {
                launchCode(code)
                code = ""
                return true
            }
        } else {
            code = number.toString()
        }
        lastKeyClick = System.currentTimeMillis()
        return false
    }

    private fun launchCode(value: String) {
        when (value.trim()) {
            Constants.Codes.SETTINGS -> {
                startPackage("com.android.tv.settings")
            }
            Constants.Codes.TEAMVIEWER -> {
                // FUNCIONALIDAD DE LANZAR APPS ELIMINADA
                Toast.makeText(this, "Funcionalidad de lanzar apps deshabilitada", Toast.LENGTH_SHORT).show()
            }
            Constants.Codes.MAC -> {
                Toast.makeText(this, macAddress, Toast.LENGTH_LONG).show()
            }
            Constants.Codes.SHOW_IP -> {
                val ip = Utils.getIPAddress(true)
                Toast.makeText(this, ip, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setVideoView() {
        binding.video.player = player
        binding.video.visibility = View.VISIBLE
        binding.myVideo.setOnFocusChangeListener { _, hasFocus ->
            binding.videoLiveText.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }
        binding.myVideo.setOnClickListener { _ ->
            openFullScreenPlayer()
        }
        binding.video.setOnClickListener { _ ->
            openFullScreenPlayer()
        }
        binding.video.setOnTouchListener { v, _ ->
            v.performClick()
            false
        }
        binding.myVideo.requestFocus()
    }
    
    /**
     * Muestra el carrusel "Más apps" con aplicaciones instaladas (no sistema).
     * Usa un DialogFragment con Compose Dialog para tener ViewTreeLifecycleOwner correcto.
     */
    private fun showMoreAppsCarousel() {
        MoreAppsCarouselDialogFragment()
            .show(supportFragmentManager, "MoreAppsCarousel")
    }

    /**
     * Lanza una app por su package name. Llamado desde MoreAppsCarouselDialogFragment.
     */
    fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) startActivity(intent)
    }
    
    /**
     * Abre PlayerActivity a pantalla completa
     * Pausa el ExoPlayer del preview antes de abrir el reproductor para evitar conflictos de codec
     */
    private fun openFullScreenPlayer() {
        try {
            player?.pause()
            binding.video.player = null
            // Lanzar PlayerActivity sin URL específica (cargará lista de canales)
            val intent = Intent(this, PlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir PlayerActivity: ${e.message}", e)
            // Fallback: intentar con URL del canal por defecto si está disponible
            val defaultChannel = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL) ?: ""
            if (defaultChannel.isNotEmpty()) {
                val fullUrl = if (defaultChannel.startsWith("http")) {
                    defaultChannel
                } else {
                    baseUrl + defaultChannel
                }
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(PlayerActivity.EXTRA_VIDEO_URL, fullUrl)
                }
                startActivity(intent)
            }
        }
    }

    fun setBtnImages(translations: ArrayList<Translations>) {
        if (translations.isEmpty()) return
        for (translation in translations) {
            val i = translations.indexOf(translation)
            val btnID = "btn$i"
            val resID = resources.getIdentifier(btnID, "id", packageName)
            val btnFor = binding.root.findViewById<ImageButton>(resID) ?: continue
            btnFor.visibility = View.VISIBLE
            // Capturar por iteración para que el listener use los valores de ESTE botón, no del último del bucle
            val btnFunctionType = translation.functionType ?: 0
            val btnFunctionTarget = translation.functionTarget ?: ""
            runOnUiThread {
                // Configurar navegación de foco: ARRIBA desde botones de apps -> video (ya configurado en XML)
                // El XML tiene prioridad, pero mantenemos esto como fallback
                if (btnFor.nextFocusUpId == 0) {
                    btnFor.nextFocusUpId = R.id.video
                }
                
                btnFor.setOnFocusChangeListener { _, hasFocus ->
                    if (!isFinishing && !isDestroyed) {
                        if (hasFocus) {
                            // Escalado desactivado - solo cambiar a imagen focused cuando recibe foco
                            imageHelper.loadRoundCorner(
                                baseUrl + translation.pictureFocused,
                                btnFor,
                                this@MainMenu
                            )
                        } else {
                            // Escalado desactivado - solo cambiar a imagen normal cuando pierde foco
                            imageHelper.loadRoundCorner(
                                baseUrl + translation.picture,
                                btnFor,
                                this@MainMenu
                            )
                        }
                    }
                }
                if (!isFinishing && !isDestroyed) {
                    imageHelper.loadRoundCorner(
                        baseUrl + translation.picture,
                        btnFor,
                        this@MainMenu
                    )
                }
                btnFor.setOnClickListener { _ ->
                    Log.d(TAG, "Clic en botón: type=$btnFunctionType, target=$btnFunctionTarget")

                    // 1. ¿El target existe en la lista de submenús del JSON? → abrir rejilla (MoreAppsSubmenuActivity)
                    val isSubmenu = viewModel.isTargetSubmenu(btnFunctionTarget)
                    if (isSubmenu || btnFunctionType == 5) {
                        Log.d(TAG, "Abriendo SubmenuActivity para target: $btnFunctionTarget")
                        goFunction(5, btnFunctionTarget)
                        return@setOnClickListener
                    }

                    // 2. Tipo 3 y NO es submenú → carrusel de apps instaladas
                    if (btnFunctionType == 3) {
                        showMoreAppsCarousel()
                        return@setOnClickListener
                    }

                    // 3. Resto (Tipo 1, 4, 6, etc.): lógica estándar
                    goFunction(btnFunctionType, btnFunctionTarget)
                }
            }
        }
    }

    fun setBtnImagesFromDevice(translations: ArrayList<RoomflixButton>) {
        if (translations.isEmpty()) return
        for (translation in translations) {
            val i = translations.indexOf(translation)
            val btnID = "btn$i"
            val resID = resources.getIdentifier(btnID, "id", packageName)
            val btnFor = binding.root.findViewById<ImageButton>(resID) ?: continue
            btnFor.visibility = View.VISIBLE
            runOnUiThread {
                // Configurar navegación de foco: ARRIBA desde botones de apps -> video (ya configurado en XML)
                // El XML tiene prioridad, pero mantenemos esto como fallback
                if (btnFor.nextFocusUpId == 0) {
                    btnFor.nextFocusUpId = R.id.video
                }
                
                btnFor.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        // Escalado desactivado - solo cambiar a imagen focused cuando recibe foco
                        // view.scaleX = 1.1f
                        // view.scaleY = 1.1f
                        imageHelper.loadRoundCorner(translation.imgFocused, btnFor)
                    } else {
                        // Escalado desactivado - solo cambiar a imagen normal cuando pierde foco
                        // view.scaleX = 1.0f
                        // view.scaleY = 1.0f
                        imageHelper.loadRoundCorner(translation.img, btnFor)
                    }
                }
                imageHelper.loadRoundCorner(translation.img, btnFor)
            }
        }
    }


    // Weather functionality removed - no longer needed for Roomflix



    /**
     * Muestra un diálogo elegante cuando no hay conexión ni caché disponible
     */
    private fun showNetworkErrorDialog() {
        if (isFinishing || isDestroyed) return
        
        try {
            val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.dialog_network_error)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCancelable(false)
            
            val btnRetry = dialog.findViewById<Button>(R.id.btn_retry)
            val btnSettings = dialog.findViewById<Button>(R.id.btn_settings)
            
            btnRetry?.setOnClickListener { _ ->
                dialog.dismiss()
                executeCall()
            }
            
            btnSettings?.setOnClickListener { _ ->
                dialog.dismiss()
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            
            dialog.show()
            
            // Asegurar foco en el botón de reintento
            dialog.window?.decorView?.post {
                btnRetry?.requestFocus()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar diálogo de error de red: ${e.message}", e)
        }
    }
    
    /**
     * Inicia un sistema de reintento silencioso cada 60 segundos cuando está offline
     * Solo se ejecuta si la app está usando caché (modo offline)
     */
    private fun startSilentRetry() {
        // Cancelar reintento anterior si existe
        retryJob?.cancel()
        
        if (!isOfflineMode) return
        
        retryJob = coroutineScope.launch {
            while (isActive && isOfflineMode) {
                delay(60000) // 60 segundos
            
                if (!isActive || isFinishing || isDestroyed) break
            
                
                // Intentar llamar a la API en segundo plano
                executeCall()
            
                // Si la llamada fue exitosa, el flag isOfflineMode se actualizará automáticamente
                // cuando se reciban datos nuevos (ya no se usará caché)
            }
        }
    }
    
    /**
     * Detiene el sistema de reintento silencioso
     */
    private fun stopSilentRetry() {
        retryJob?.cancel()
        retryJob = null
        isOfflineMode = false
    }

    override fun onDestroy() {
        // Limpiar handler de buffer
        bufferClearHandler?.removeCallbacksAndMessages(null)
        bufferClearHandler = null
        clearAllBuffers()
        
        // 2. Limpieza antigua (lo que ya tenías en MainMenu)
        stopSilentRetry()
        
        // Limpiar jobs de TopInfoBar (solo hora; el clima lo gestiona el ViewModel)
        topInfoBarUpdateJob?.cancel()
        topInfoBarUpdateJob = null
        
        coroutineScope.cancel()
        context = null
        
        try {
            player?.release()
            player = null
            binding.video.player = null
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar ExoPlayer: ${e.message}", e)
        }
        
        // 3. Llamada al padre (IMPORTANTE: Solo una vez)
        super.onDestroy()
    }
    
    // ========================================================================
    // BACKDOOR DE SEGURIDAD
    // ========================================================================
    
    /**
     * Maneja las secuencias de teclas para accesos ocultos
     * Secuencia Backdoor: ARRIBA, ARRIBA, ARRIBA, ABAJO, DERECHA, OK → Abre ajustes del sistema
     */
    private fun handleBackdoorSequence(keyCode: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Cancelar handler de limpieza de buffer anterior
        bufferClearHandler?.removeCallbacksAndMessages(null)
        
        // Si ha pasado mucho tiempo desde la última tecla, resetear buffer
        if (currentTime - lastKeyTime > SEQUENCE_TIMEOUT_MS) {
            clearAllBuffers()
        }
        
        lastKeyTime = currentTime
        
        // Intentar secuencia Backdoor: ARRIBA, ARRIBA, ARRIBA, ABAJO, DERECHA, OK
        val backdoorResult = checkSequence(keyCode, backdoorSequence, backdoorBuffer) { sequence ->
            if (sequence == backdoorSequence) {
                Log.d(TAG, "Backdoor activado: Abriendo ajustes del sistema")
                clearAllBuffers()
                openSystemSettings()
                return@checkSequence true
            }
            false
        }
        if (backdoorResult) return true
        
        // Si ninguna secuencia coincide, programar limpieza de buffer después de 2 segundos
        scheduleBufferClear()
        
        return false
    }
    
    /**
     * Verifica si una tecla coincide con una secuencia y actualiza el buffer
     * @return true si la secuencia se completó y se ejecutó la acción
     */
    private fun checkSequence(
        keyCode: Int,
        sequence: List<Int>,
        buffer: MutableList<Int>,
        onComplete: (List<Int>) -> Boolean
    ): Boolean {
        val expectedIndex = buffer.size
        
        if (expectedIndex < sequence.size && keyCode == sequence[expectedIndex]) {
            // Tecla correcta, añadir al buffer
            buffer.add(keyCode)
            
            // Si la secuencia está completa, ejecutar acción
            if (buffer.size == sequence.size) {
                return onComplete(buffer.toList())
            }
        } else {
            // Tecla incorrecta, resetear este buffer
            buffer.clear()
            // Si la primera tecla es correcta, comenzar la secuencia
            if (keyCode == sequence[0]) {
                buffer.add(keyCode)
            }
        }
        
        return false
    }
    
    /**
     * Limpia todos los buffers de secuencias
     */
    private fun clearAllBuffers() {
        backdoorBuffer.clear()
        bufferClearHandler?.removeCallbacksAndMessages(null)
    }
    
    /**
     * Programa la limpieza del buffer después de 2 segundos de inactividad
     */
    private fun scheduleBufferClear() {
        bufferClearHandler?.removeCallbacksAndMessages(null)
        bufferClearHandler = Handler(Looper.getMainLooper())
        bufferClearHandler?.postDelayed({
            clearAllBuffers()
        }, BUFFER_CLEAR_DELAY_MS)
    }
    
    /**
     * Abre los ajustes del sistema (backdoor)
     */
    private fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir ajustes del sistema: ${e.message}", e)
        }
    }
    
    
}

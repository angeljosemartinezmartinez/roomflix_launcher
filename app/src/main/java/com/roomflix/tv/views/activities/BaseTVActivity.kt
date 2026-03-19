package com.roomflix.tv.views.activities

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.TextClock
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.roomflix.tv.Constants
import com.roomflix.tv.dragger.LauncherApplication
import com.roomflix.tv.dragger.MySharedPreferences
import com.roomflix.tv.helpers.ImageHelper
import com.roomflix.tv.dragger.AppComponent
import javax.inject.Inject

/**
 * BaseTVActivity - Clase base para actividades de Android TV
 * 
 * Proporciona funcionalidad común:
 * - Gestión del reloj (TextClock)
 * - Limpieza de memoria de ImageHelper (Glide)
 * - Gestión básica del mando a distancia
 */
abstract class BaseTVActivity : AppCompatActivity(), DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "BaseTVActivity"
    }
    
    @Inject
    lateinit var mySharedPreferences: MySharedPreferences
    
    @Inject
    lateinit var imageHelper: ImageHelper
    
    /**
     * Configura el reloj con formato 24 horas y timezone
     * @param clock TextClock a configurar
     */
    protected open fun setupClock(clock: TextClock) {
        clock.format12Hour = null
        clock.format24Hour = "HH:mm"
        val timezone = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.TIMEZONE) ?: ""
        if (timezone.isNotEmpty()) {
            clock.timeZone = timezone
        }
    }
    
    /**
     * Limpia la memoria de Glide para liberar recursos
     * Se llama automáticamente en onStop y onDestroy
     */
    protected fun clearImageCache() {
        try {
            // Limpiar memoria de Glide
            Glide.get(this).clearMemory()
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar memoria de Glide", e)
        }
    }
    
    /**
     * Gestión básica de teclas del mando a distancia
     * Las actividades hijas pueden sobrescribir este método para añadir lógica específica
     */
    open fun handleKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        // Lógica común de teclas (si es necesaria)
        return false
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super<AppCompatActivity>.onCreate(savedInstanceState)
        // Inyectar dependencias de Dagger
        (application as LauncherApplication).appComponent.inject(this)
        // Registrar observer del ciclo de vida (this implementa DefaultLifecycleObserver)
        lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
    }
    
    override fun onResume(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onResume(owner)
    }
    
    override fun onPause(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onPause(owner)
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        // Limpiar memoria de imágenes cuando la actividad se detiene
        clearImageCache()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onDestroy(owner)
        // Limpieza final de recursos
        clearImageCache()
    }
    
    // Sobrescribir también el onDestroy de AppCompatActivity para asegurar limpieza completa
    override fun onDestroy() {
        clearImageCache()
        super<AppCompatActivity>.onDestroy()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Delegar a handleKeyEvent para permitir sobrescritura en clases hijas
        if (handleKeyEvent(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

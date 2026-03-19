package com.roomflix.tv.model

import android.graphics.drawable.Drawable

/**
 * Modelo de una app instalada para el carrusel "Más apps".
 * @param name Nombre visible de la app
 * @param packageName Package name para lanzarla
 * @param banner Icono landscape (TV) o icono normal si no tiene banner
 */
data class InstalledApp(
    val name: String,
    val packageName: String,
    val banner: Drawable?
)

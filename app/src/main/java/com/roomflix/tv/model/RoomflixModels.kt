package com.roomflix.tv.model

import com.google.gson.annotations.SerializedName

/**
 * RoomflixModels.kt
 * 
 * Data Classes para mapear el JSON de respuesta de la API de Roomflix
 * Todas las rutas son relativas y deben concatenarse con baseUrl
 */

/**
 * Respuesta base del API
 */
data class BaseResponse(
    @SerializedName("baseUrl")
    val baseUrl: String,
    
    @SerializedName("languages")
    val languages: List<Language>,
    
    @SerializedName("template")
    val template: Template,
    
    @SerializedName("submenus")
    val submenus: List<Submenu>? = null,
    
    @SerializedName("infoCards")
    val infoCards: List<InfoCard>? = null,
    
    @SerializedName("configuration")
    val configuration: Configuration? = null
)

/**
 * Idioma/Language del sistema
 */
data class Language(
    @SerializedName("nativeName")
    val nativeName: String,
    
    @SerializedName("code")
    val code: String,
    
    @SerializedName("picture")
    val picture: String,
    
    @SerializedName("isDefault")
    val isDefault: Boolean,
    
    @SerializedName("channel")
    val channel: String  // URL del .m3u8 para el reproductor
)

/**
 * Template con branding (logo, background, botones)
 */
data class Template(
    @SerializedName("logo")
    val logo: String,  // Ruta relativa: /uploads/...
    
    @SerializedName("miniatureLogo")
    val miniLogo: String,  // Ruta relativa: /uploads/...
    
    @SerializedName("background")
    val background: String,  // Ruta relativa: /uploads/...
    
    @SerializedName("buttons")
    val buttons: List<TemplateButton>
)

/**
 * Botón del template (menú principal)
 */
data class TemplateButton(
    @SerializedName("position")
    val position: Int,
    
    @SerializedName("translations")
    val translations: List<ButtonTranslation>
)

/**
 * Traducción del botón (imágenes y funciones)
 */
data class ButtonTranslation(
    @SerializedName("language")
    val language: String,
    
    @SerializedName("picture")
    val picture: String,  // Ruta relativa: /uploads/...
    
    @SerializedName("focusPicture")
    val focusPicture: String,  // Ruta relativa: /uploads/...
    
    @SerializedName("functionType")
    val functionType: Int,  // 1=App externa, 4=InfoCard, 6=Reproductor interno
    
    @SerializedName("functionTarget")
    val functionTarget: String  // Package name (si functionType=1) o ID (si functionType=4 o 6)
)

/**
 * Submenú
 */
data class Submenu(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("translations")
    val translations: List<SubmenuTranslation>,
    
    @SerializedName("buttons")
    val buttons: List<TemplateButton>? = null
)

/**
 * Traducción del submenú
 */
data class SubmenuTranslation(
    @SerializedName("language")
    val language: String,
    
    @SerializedName("title")
    val title: String
)

/**
 * InfoCard
 */
data class InfoCard(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("translations")
    val translations: List<InfoCardTranslation>,
    
    @SerializedName("childs")
    val childs: List<InfoCardChild>? = null
)

/**
 * Traducción de InfoCard
 */
data class InfoCardTranslation(
    @SerializedName("language")
    val language: String,
    
    @SerializedName("picture")
    val picture: String
)

/**
 * Hijo de InfoCard
 */
data class InfoCardChild(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("translations")
    val translations: List<InfoCardTranslation>
)

/**
 * Configuración del sistema
 */
data class Configuration(
    @SerializedName("timezone")
    val timezone: String? = null,
    
    @SerializedName("adbServer")
    val adbServer: String? = null
)

/**
 * Constantes para functionType
 */
object FunctionType {
    const val EXTERNAL_APP = 1  // Abrir app externa (Netflix, YouTube, etc.)
    const val SUBMENU = 3       // Más apps: carrusel de apps instaladas (antes MoreAppsActivity)
    const val INFO_CARD = 4     // Mostrar InfoCard
    const val INTERNAL_PLAYER = 6  // Abrir reproductor interno (.m3u8)
}

package com.roomflix.tv.utils

import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.weathericons.WeatherIcons

/**
 * Mapea códigos o texto de condición meteorológica a iconos WeatherIcons (Iconics).
 * Soporta:
 * - Código WMO (Open-Meteo) 0-99
 * - Texto de condición (p. ej. "clear", "rain") para otras APIs
 */
object WeatherIconMapper {

    /**
     * Convierte el código WMO de Open-Meteo (current_weather.weathercode) en un IIcon.
     * Referencia: https://open-meteo.com/en/docs#api_form
     */
    @JvmStatic
    fun getWeatherIcon(weathercode: Int): IIcon = when (weathercode) {
        0 -> WeatherIcons.Icon.wic_day_sunny
        1 -> WeatherIcons.Icon.wic_day_sunny_overcast
        2 -> WeatherIcons.Icon.wic_day_cloudy
        3 -> WeatherIcons.Icon.wic_cloudy
        45, 48 -> WeatherIcons.Icon.wic_fog
        51, 53, 55 -> WeatherIcons.Icon.wic_sprinkle
        56, 57 -> WeatherIcons.Icon.wic_rain_mix
        61, 63, 65 -> WeatherIcons.Icon.wic_rain
        66, 67 -> WeatherIcons.Icon.wic_rain_mix
        71, 73, 75, 77 -> WeatherIcons.Icon.wic_snow
        80, 81, 82 -> WeatherIcons.Icon.wic_showers
        85, 86 -> WeatherIcons.Icon.wic_snow
        95 -> WeatherIcons.Icon.wic_thunderstorm
        96, 99 -> WeatherIcons.Icon.wic_thunderstorm
        else -> WeatherIcons.Icon.wic_cloudy
    }

    /**
     * Convierte una descripción textual de la condición en un IIcon.
     * Útil para APIs que devuelven "main" o "description" como string.
     */
    @JvmStatic
    fun getWeatherIcon(condition: String): IIcon {
        return when (condition.lowercase()) {
            "clear", "sunny", "clear sky" -> WeatherIcons.Icon.wic_day_sunny
            "rain", "light rain", "moderate rain", "heavy rain" -> WeatherIcons.Icon.wic_rain
            "drizzle", "light intensity drizzle" -> WeatherIcons.Icon.wic_sprinkle
            "clouds", "cloudy", "overcast" -> WeatherIcons.Icon.wic_cloudy
            "partly cloudy", "partly cloud" -> WeatherIcons.Icon.wic_day_cloudy
            "snow", "light snow", "heavy snow" -> WeatherIcons.Icon.wic_snow
            "thunderstorm" -> WeatherIcons.Icon.wic_thunderstorm
            "fog", "mist", "haze" -> WeatherIcons.Icon.wic_fog
            "showers", "shower rain" -> WeatherIcons.Icon.wic_showers
            else -> WeatherIcons.Icon.wic_cloudy
        }
    }
}

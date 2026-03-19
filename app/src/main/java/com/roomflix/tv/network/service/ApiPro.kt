package com.roomflix.tv.network.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.roomflix.tv.BuildConfig
import com.roomflix.tv.Constants
import com.roomflix.tv.network.DeviceIdInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * ApiPro - Gestor de configuración de Retrofit para Roomflix
 * 
 * URL base de la API - NO CAMBIAR el dominio, solo el protocolo si es necesario
 * Para cambiar a HTTPS, simplemente cambia "http" por "https" en BASE_URL
 */
object ApiPro {

    // URL base de la API - HTTPS para seguridad
    private const val BASE_URL = "https://panel.hotelplay.tv"

    // Context global para DeviceIdInterceptor
    private var globalContext: Context? = null

    /**
     * Inicializa el Context global (llamar desde Application.onCreate)
     */
    @JvmStatic
    fun init(context: Context) {
        globalContext = context.applicationContext
    }

    /**
     * Crea una instancia del servicio Retrofit sin Context (sin DeviceIdInterceptor)
     * 
     * @param serviceClass La interfaz del servicio Retrofit
     * @return Instancia del servicio configurado
     */
    @JvmStatic
    fun <S> createService(serviceClass: Class<S>): S {
        return createService(serviceClass, globalContext)
    }

    /**
     * Crea una instancia del servicio Retrofit con Context (con DeviceIdInterceptor)
     * 
     * @param serviceClass La interfaz del servicio Retrofit
     * @param context Context para obtener Device ID (opcional, si es null usa globalContext)
     * @return Instancia del servicio configurado
     */
    @JvmStatic
    fun <S> createService(serviceClass: Class<S>, context: Context?): S {
        val finalContext = context ?: globalContext
        val httpClient = OkHttpClient.Builder()

        val gson = GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create()

        val builder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            // ScalarsConverterFactory primero para manejar String (M3U)
            // GsonConverterFactory después para JSON
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))

        // Nivel BASIC: peticiones y códigos HTTP sin volcar cuerpos (M3U/XML de MB colapsan I/O en TV)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENVIRONMENT.equals(Constants.ENVIRONMENT.DEVELOP, ignoreCase = true)) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Interceptor para headers comunes
        httpClient.addInterceptor { chain ->
            val original: Request = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .method(original.method, original.body)
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        // Añadir DeviceIdInterceptor si tenemos Context
        finalContext?.let {
            httpClient.addInterceptor(DeviceIdInterceptor(it))
        }

        // Añadir logging interceptor
        httpClient.addInterceptor(loggingInterceptor)

        val client = httpClient.build()
        val retrofit = builder.client(client).build()
        return retrofit.create(serviceClass)
    }
}

package com.roomflix.tv.views.activities;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.logger.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.roomflix.tv.BuildConfig;
import com.roomflix.tv.Constants;
import com.roomflix.tv.R;
import com.roomflix.tv.listener.CallBackAllInfoCheck;
import com.roomflix.tv.listener.CallBackCheckConnection;
import com.roomflix.tv.listener.CallBackSaveData;
import com.roomflix.tv.network.DeviceIdProvider;
import com.roomflix.tv.network.callbacks.CallBackData;
import com.roomflix.tv.network.response.ResponseAllInfo;
import com.roomflix.tv.network.response.ResponseConfiguration;
import com.roomflix.tv.network.response.ResponseUpdate;
import com.roomflix.tv.utils.NetWorkUtils;
import com.roomflix.tv.vpn.VpnManagerHolder;

public class NetworkBaseActivity extends BaseActivity {

    private static final String TAG = "NetworkBaseActivity";
    private String dateNow;
    private boolean inserting;
    private boolean dataSave;
    private String baseUrl;
    private AlertDialog loadingDialog;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable loadingSafetyRunnable;
    Type typeOfObjectsListNew = new TypeToken<ResponseUpdate>() {
    }.getType();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inserting = false;
        dataSave = false;
    }

    public void checkCasesConnection(CallBackCheckConnection callBackCheckConnection) {
        do {
            if (NetWorkUtils.isOnline()) {
                callBackCheckConnection.success();
                break;
            } else {
                Log.d(TAG, "doing process");
                if (!checkConnection(getApplicationContext()))
                    callBackCheckConnection.noConnection();
                else
                    callBackCheckConnection.noPing();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
        while (true);

    }

    public boolean checkConnection(Context c) {
        try {
            return (NetWorkUtils.isNetworkAvailable(c));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void callUpdate() {
        call.getUpdate(macAddress, new CallBackData<ResponseUpdate>() {
            @Override
            public void finishAction(ResponseUpdate body) {
                mySharedPreferences.putStringObject(Constants.SHARED_PREFERENCES.UPDATE_OBJECT, body, typeOfObjectsListNew);
                if (isUpdateDifferent(body))
                    saveDateApk(body.date);
            }

            @Override
            public void error(String s) {

            }
        });
    }

    private boolean isUpdateDifferent(ResponseUpdate body) {
        int actual = putCallUpdate(body);
        int last = mySharedPreferences.getInt(Constants.SHARED_PREFERENCES.HASH_UPDATE);
        if (last == (actual)) {
            Logger.d("UPDATE NOT CHANGED");
            return false;
        } else {
            Logger.d("UPDATE CHANGED");
            // Launcher ya no gestiona instalación/desinstalación de APKs; solo persistir hash y objeto de update
            mySharedPreferences.putInt(Constants.SHARED_PREFERENCES.HASH_UPDATE, actual);
            mySharedPreferences.putStringObject(Constants.SHARED_PREFERENCES.UPDATE_OBJECT, body, typeOfObjectsListNew);
            return true;
        }
    }

    private int putCallUpdate(ResponseUpdate update) {
        Gson g = new Gson();
        Type typeOfObjectsListNew = new TypeToken<ResponseUpdate>() {
        }.getType();
        String jsonResponse = g.toJson(update, typeOfObjectsListNew);
        int hash = jsonResponse.hashCode();
        return hash;
    }

    public void callAllInfo(CallBackAllInfoCheck callBackAllInfoCheck) {
        call.getDataFromServer(macAddress, new CallBackData<ResponseAllInfo>() {
            @Override
            public void finishAction(ResponseAllInfo body) {
                // Guardar en caché después de recibir datos exitosamente
                saveCacheData(body);
                mySharedPreferences.putBoolean(Constants.SHARED_PREFERENCES.IS_USING_CACHE, false);
                
                if (isDataDifferent(body)) {
                    saveData(body, callBackAllInfoCheck,
                            (ResponseUpdate) mySharedPreferences.getObject(Constants.SHARED_PREFERENCES.UPDATE_OBJECT, typeOfObjectsListNew));
                } else {
                    Logger.d("DATA NOT DIFFERENT");
                    if (callBackAllInfoCheck != null) {
                        callBackAllInfoCheck.dataNoChange();
                    }
                }
            }

            @Override
            public void error(String s) {
                if (s.equals("403")) {
                    callBackAllInfoCheck.error(s);
                } else if (s.equals("503")) {
                    callBackAllInfoCheck.dataNoChange();
                } else {
                    // Error de red o HTTP: intentar cargar desde caché
                    Log.d(TAG, "API Error Code: " + s + ". Intentando cargar desde caché...");
                    loadFromCache(callBackAllInfoCheck);
                }
            }
        });
    }
    
    /**
     * Guarda los datos en caché local (SharedPreferences)
     */
    private void saveCacheData(ResponseAllInfo body) {
        try {
            Type typeOfResponseAllInfo = new TypeToken<ResponseAllInfo>() {}.getType();
            mySharedPreferences.putStringObject(Constants.SHARED_PREFERENCES.CACHED_ALL_INFO, body, typeOfResponseAllInfo);
            Log.d(TAG, "Datos guardados en caché correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al guardar datos en caché: " + e.getMessage(), e);
        }
    }
    
    /**
     * Carga datos desde caché local si están disponibles
     * Retorna true si se cargaron datos desde caché, false si no hay caché disponible
     */
    private boolean loadFromCache(CallBackAllInfoCheck callBackAllInfoCheck) {
        try {
            Type typeOfResponseAllInfo = new TypeToken<ResponseAllInfo>() {}.getType();
            ResponseAllInfo cachedData = (ResponseAllInfo) mySharedPreferences.getObject(
                    Constants.SHARED_PREFERENCES.CACHED_ALL_INFO, typeOfResponseAllInfo);
            
            if (cachedData != null) {
                // Marcar que estamos usando caché
                mySharedPreferences.putBoolean(Constants.SHARED_PREFERENCES.IS_USING_CACHE, true);
                
                Log.d(TAG, "Datos cargados desde caché. Usando datos offline.");
                
                // Usar los datos cacheados como si no hubiera cambios (para no reinsertar en BD)
                if (callBackAllInfoCheck != null) {
                    // Primero restablecer datos desde caché en SharedPreferences
                    restoreDataFromCache(cachedData);
                    callBackAllInfoCheck.dataNoChange();
                }
                return true;
            } else {
                Log.d(TAG, "No hay datos en caché disponibles");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar datos desde caché: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Restaura los datos desde caché en SharedPreferences (baseUrl, background, etc.)
     */
    private void restoreDataFromCache(ResponseAllInfo cachedData) {
        if (cachedData == null) return;
        
        try {
            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.BASE_URL, cachedData.baseUrl);
            baseUrl = cachedData.baseUrl;
            
            if (cachedData.templates != null) {
                if (cachedData.templates.background != null) {
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK, cachedData.templates.background);
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK_LANG, cachedData.templates.background);
                }
                if (cachedData.templates.logo != null) {
                    String logo = cachedData.baseUrl + cachedData.templates.logo;
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LOGO, logo);
                }
                if (cachedData.templates.miniLogo != null) {
                    String miniLogo = cachedData.baseUrl + cachedData.templates.miniLogo;
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.MINI_LOGO, miniLogo);
                }
            }
            
            if (cachedData.configuration != null) {
                if (cachedData.configuration.adbServer != null) {
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.ADB_SERVER, cachedData.configuration.adbServer);
                }
                if (cachedData.configuration.controlApiUrl != null) {
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.CONTROL_API_URL, cachedData.configuration.controlApiUrl);
                }
                if (cachedData.configuration.controlApiToken != null) {
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.CONTROL_API_TOKEN, cachedData.configuration.controlApiToken);
                }
                if (cachedData.configuration.controlDeviceId != null) {
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.CONTROL_DEVICE_ID, cachedData.configuration.controlDeviceId);
                }
            }
            
            // Restaurar idioma por defecto
            if (cachedData.languages != null && !cachedData.languages.isEmpty()) {
                String selectedLang = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.SELECTED_LANGUAGE_CODE);
                for (int i = 0; i < cachedData.languages.size(); i++) {
                    if (cachedData.languages.get(i).isDefault) {
                        String codeLangDefault = cachedData.languages.get(i).code;
                        String defaultChannel = cachedData.languages.get(i).channel;
                        if (selectedLang == null || selectedLang.isEmpty()) {
                            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, codeLangDefault);
                        }
                        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANG_DEFAULT, codeLangDefault);
                        if (defaultChannel != null) {
                            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL, defaultChannel);
                        }
                        break;
                    }
                }
            }
            
            Log.d(TAG, "Datos restaurados desde caché en SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error al restaurar datos desde caché: " + e.getMessage(), e);
        }
    }

    private void saveDateApk(String fecha) {
        if (fecha != null)
            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.APK_DATE, fecha);
    }

    private void saveBackground(ResponseAllInfo body) {
        String background = body.templates.background;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK, background);
    }

    private void saveAdbServer(ResponseAllInfo body) {
        String adbServer = body.configuration.adbServer;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.ADB_SERVER, adbServer);
    }

    private void saveBackgroundLanguages(ResponseAllInfo body) {
        String backgroundLanguages = body.templates.background;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK_LANG, backgroundLanguages);
    }

    private void saveDefaultLanguage(ResponseAllInfo body) {
        String codeLangDefault;
        String defaultChannel;
        // Idioma elegido por el usuario (no debe ser sobreescrito por sync)
        String selectedLang = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.SELECTED_LANGUAGE_CODE);
        for (int i = 0; i < body.languages.size(); i++) {
            if (body.languages.get(i).isDefault) {
                codeLangDefault = body.languages.get(i).code;
                defaultChannel = body.languages.get(i).channel;
                if (selectedLang == null || selectedLang.isEmpty()) {
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, codeLangDefault);
                }
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANG_DEFAULT, codeLangDefault);
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL, defaultChannel);
            }
        }
    }

    private void saveLogo(ResponseAllInfo body) {
        String logo = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL) + body.templates.logo;
        String miniLogo = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL) + body.templates.miniLogo;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LOGO, logo);
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.MINI_LOGO, miniLogo);
    }

    private void saveData(ResponseAllInfo body, CallBackAllInfoCheck callBackAllInfoCheck, ResponseUpdate update) {
        Logger.d("DATA DIFFERENT");
        saveBackground(body);
        saveBackgroundLanguages(body);
        saveAdbServer(body);
        saveDefaultLanguage(body);
        saveTimezone(body.configuration);
        saveHotSpotValues(body);
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.BASE_URL, body.baseUrl);
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        saveLogo(body);
        insertDataBaseData(body, new CallBackSaveData() {
            @Override
            public void finish() {
                callBackAllInfoCheck.dataChange();
            }

            @Override
            public void error(String s) {
                callBackAllInfoCheck.error(s);
            }
        }, update);
    }

    public synchronized void insertDataBaseData(final ResponseAllInfo body, CallBackSaveData callBackSaveData, ResponseUpdate update) {
        if (inserting) return;
        inserting = true;
        mDBManager.saveData(body, getApplicationContext(), new CallBackSaveData() {
            @Override
            public void finish() {
                dataSave = true;
                inserting = false;
                callBackSaveData.finish();
            }

            @Override
            public void error(String s) {
                callBackSaveData.error(s);
            }
        }, update);
    }


    private int putHashCall(ResponseAllInfo body) {
        Gson g = new Gson();
        Type typeOfObjectsListNew = new TypeToken<ResponseAllInfo>() {
        }.getType();
        String jsonResponse = g.toJson(body, typeOfObjectsListNew);
        int hash = jsonResponse.hashCode();
        return hash;
    }

    public boolean isDataDifferent(ResponseAllInfo body) {
        int actual = putHashCall(body);
        int last = mySharedPreferences.getInt(Constants.SHARED_PREFERENCES.HASH_ALL_INFO);
        if (last == (actual)) {
            dataSave = true;
            inserting = false;
            return false;
        } else {
            mDBManager.delete(this);
            mySharedPreferences.putInt(Constants.SHARED_PREFERENCES.HASH_ALL_INFO, actual);
            return true;
        }
    }

    public synchronized void goFunction(int function, String args) {
        Logger.d("RoomFlix_Nav", "Function: " + function + " Args: " + args);
        switch (function) {
            case 1:
                // ACTIVACIÓN UNIVERSAL: abrir apps externas (VOD / streaming)
                cleanAndStartApp(args);
                break;
            case 2:
                openWeb(args);
                break;
            case 3:
                startSubmenu(args);
                break;
            case 4:
                // functionType == 4: Mostrar InfoCard
                infoCard(args);
                break;
            case 5:
                startMoreAppsSubmenu(args);
                break;
            case 6:
                // functionType == 6: Abrir reproductor interno (.m3u8)
                openInternalPlayer(args);
                break;
            case 7:
                openCastTutorial(this);
                break;
            case 8:
                // Usar el contexto de la Activity actual en lugar de MainMenu.context estático
                // Verificar que la Activity esté viva para evitar BadTokenException en Android TV
                if (!isFinishing()) {
                    openParentalControlDialog(this);
                }
                break;
            // case 9: Weather removed
            default:
                comingSoon(this);
        }
    }

    /**
     * Limpia datos de la app via Control API (o legacy adbServer) y abre la app externa.
     * Fail-open: si la API falla, igualmente abrimos la app.
     */
    private void cleanAndStartApp(final String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return;
        }

        // Anti-double click
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }

        final String controlApiUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.CONTROL_API_URL);
        final String controlApiToken = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.CONTROL_API_TOKEN);
        final String controlDeviceId = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.CONTROL_DEVICE_ID);

        // Si tenemos Control API configurada, usar la nueva ruta
        if (controlApiUrl != null && !controlApiUrl.trim().isEmpty()
                && controlApiToken != null && !controlApiToken.trim().isEmpty()
                && controlDeviceId != null && !controlDeviceId.trim().isEmpty()) {
            cleanViaControlApi(packageName, controlApiUrl, controlApiToken, controlDeviceId);
            return;
        }

        // Fallback: usar legacy adbServer
        cleanViaLegacyAdb(packageName);
    }

    /**
     * Clear data via Control API REST.
     * Si VPN activa: usa 10.10.0.1 (trafico por tunel WireGuard).
     * Si VPN inactiva: usa el dominio control.roomflix.tv (fallback).
     */
    private void cleanViaControlApi(String packageName, String apiUrl, String token, String deviceId) {
        showLoadingDialog(getFriendlyAppName(packageName));

        // Determinar URL base segun estado VPN
        boolean vpnActive = VpnManagerHolder.INSTANCE.getInstance(this).isConnected();
        String baseUrl;
        if (vpnActive) {
            baseUrl = "http://10.10.0.1/";
            Log.d(TAG, "Clear data via VPN tunnel (10.10.0.1) device=" + deviceId);
        } else {
            baseUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
            Log.d(TAG, "Clear data via domain (" + apiUrl + ") device=" + deviceId);
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();

        String json = "{\"package\":\"" + packageName + "\"}";
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                json, okhttp3.MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(baseUrl + "api/v1/devices/" + deviceId + "/clear-data")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.w(TAG, "Control API clear-data failed: " + e.getMessage());
                // fail-open: abrir app inmediatamente
                uiHandler.post(() -> { dismissLoadingDialog(); startPackage(packageName); });
            }

            @Override
            public void onResponse(Call call, Response response) {
                boolean clearOk = false;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String bodyStr = response.body().string();
                        clearOk = bodyStr.contains("\"success\":true");
                        Log.d(TAG, "Control API clear-data: " + (clearOk ? "OK" : "FAIL") + " " + bodyStr);
                    } else {
                        Log.w(TAG, "Control API clear-data HTTP " + response.code());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error leyendo respuesta clear: " + e.getMessage());
                } finally {
                    try { response.close(); } catch (Exception ignored) {}
                }

                // Delay solo si clear exitoso (dar tiempo a que surta efecto)
                long delay = clearOk ? 800L : 0L;
                uiHandler.postDelayed(() -> { dismissLoadingDialog(); startPackage(packageName); }, delay);
            }
        });
    }

    /**
     * Legacy: clear data via adbServer directo (deprecado)
     */
    private void cleanViaLegacyAdb(String packageName) {
        final String adbServer = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.ADB_SERVER);
        final String deviceId12 = DeviceIdProvider.getDeviceId(getApplicationContext());
        final String localIp = getLocalIpAddress();

        if (adbServer == null || adbServer.trim().isEmpty()) {
            runOnUiThread(() -> startPackage(packageName));
            return;
        }

        showLoadingDialog(getFriendlyAppName(packageName));
        Log.d(TAG, "Clear data via legacy adbServer: " + adbServer + " pkg=" + packageName);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build();

        FormBody formBody = new FormBody.Builder()
                .add("package", packageName)
                .add("mac", deviceId12)
                .add("ip", localIp != null ? localIp : "")
                .build();

        Request request = new Request.Builder()
                .url("http://" + adbServer + "/device/open/clear")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            private void launchAfterDelay() {
                uiHandler.postDelayed(() -> {
                    dismissLoadingDialog();
                    startPackage(packageName);
                }, 2000L);
            }

            @Override
            public void onFailure(Call call, java.io.IOException e) {
                launchAfterDelay();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try { response.close(); } catch (Exception ignored) { }
                launchAfterDelay();
            }
        });
    }

    /**
     * Diálogo de carga no cancelable para bloquear interacción mientras se limpia y abre una app externa.
     */
    private void showLoadingDialog(String appName) {
        try {
            dismissLoadingDialog();
            LayoutInflater inflater = LayoutInflater.from(this);
            View content = inflater.inflate(R.layout.dialog_loading_app, null, false);
            TextView tv = content.findViewById(R.id.loadingText);
            tv.setText("Preparando " + appName + "... Por favor, espere.");

            loadingDialog = new AlertDialog.Builder(this, android.R.style.Theme_Translucent_NoTitleBar)
                    .setView(content)
                    .setCancelable(false)
                    .create();
            loadingDialog.show();

            // Quitar fondo por defecto y ajustar tamaño (~40% del ancho)
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                int width = (int) (dm.widthPixels * 0.40f);
                loadingDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

                // Desenfoque opcional (Android 12+)
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    try {
                        loadingDialog.getWindow().setBackgroundBlurRadius(24);
                    } catch (Exception ignored) { }
                }
            }

            // Temporizador de seguridad: 10s para devolver control si algo se queda colgado
            loadingSafetyRunnable = () -> {
                dismissLoadingDialog();
            };
            uiHandler.postDelayed(loadingSafetyRunnable, 10_000L);
        } catch (Exception e) {
            Log.w(TAG, "No se pudo mostrar LoadingDialog: " + e.getMessage(), e);
        }
    }

    /**
     * Convierte package/alias técnico en nombre legible para UX.
     */
    private String getFriendlyAppName(String packageOrAlias) {
        if (packageOrAlias == null) return "aplicación";
        String raw = packageOrAlias.trim();
        if (raw.isEmpty()) return "aplicación";
        String key = raw.toLowerCase(Locale.ROOT);

        // Aliases (panel) y packages comunes
        if (key.equals("netflix") || key.contains("netflix")) return "Netflix";
        if (key.equals("youtube") || key.contains("youtube")) return "YouTube";
        if (key.equals("prime") || key.equals("primevideo") || key.contains("amazonvideo") || key.contains("avod")) return "Prime Video";
        if (key.equals("disney") || key.contains("disney")) return "Disney+";
        if (key.equals("hbo") || key.equals("max") || key.contains("wbd") || key.contains("hbo")) return "Max";
        if (key.equals("movistar") || key.contains("movistar") || key.contains("plus")) return "Movistar+";
        if (key.contains("settings")) return "Ajustes";

        // Fallback: último segmento del package sin puntos
        int lastDot = raw.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < raw.length() - 1) {
            String tail = raw.substring(lastDot + 1);
            if (!tail.isEmpty()) return tail;
        }
        return raw;
    }

    private void dismissLoadingDialog() {
        try {
            if (loadingSafetyRunnable != null) {
                uiHandler.removeCallbacks(loadingSafetyRunnable);
                loadingSafetyRunnable = null;
            }
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        } catch (Exception ignored) {
        } finally {
            loadingDialog = null;
        }
    }

    /**
     * Obtiene IP local IPv4 (mejor esfuerzo). Devuelve null si no se puede.
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo obtener IP local: " + e.getMessage());
        }
        return null;
    }

    /**
     * Abre el reproductor interno para reproducir .m3u8
     * @param videoUrl URL del video (.m3u8) - puede venir desde channel del language o functionTarget
     */
    private void openInternalPlayer(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            // Si no viene URL, intentar obtener el canal del lenguaje por defecto
            com.roomflix.tv.network.response.ResponseLanguages defaultLanguage = getDefaultLanguage();
            if (defaultLanguage != null && defaultLanguage.channel != null && !defaultLanguage.channel.isEmpty()) {
                // Construir URL completa si es relativa
                String fullUrl = defaultLanguage.channel.startsWith("http") 
                    ? defaultLanguage.channel 
                    : baseUrl + defaultLanguage.channel;
                videoUrl = fullUrl;
            } else {
                comingSoon(this);
                return;
            }
        } else {
            // Si viene URL relativa, concatenar con baseUrl
            if (!videoUrl.startsWith("http")) {
                videoUrl = baseUrl + videoUrl;
            }
        }

        Intent intent = new Intent(this, com.roomflix.tv.views.activities.PlayerActivity.class);
        intent.putExtra(com.roomflix.tv.views.activities.PlayerActivity.EXTRA_VIDEO_URL, videoUrl);
        startActivity(intent);
    }

    /**
     * Obtiene el lenguaje por defecto para usar su canal
     */
    private com.roomflix.tv.network.response.ResponseLanguages getDefaultLanguage() {
        // Obtener idioma por defecto desde la base de datos o SharedPreferences
        String defaultLangCode = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANG_DEFAULT);
        if (defaultLangCode == null || defaultLangCode.isEmpty()) {
            defaultLangCode = localeLang;
        }
        
        // Buscar el idioma en la lista actual (esto necesitaría acceso a los datos cargados)
        // Por ahora, retornamos null y se usará la URL que viene en functionTarget
        return null;
    }

    // setDay() - ELIMINADO: La función ya no se usa, el elemento 'day' fue eliminado del layout

    private void saveTimezone(ResponseConfiguration configuration) {
        String timezone = configuration.timeZone + "";
        switch (configuration.timeZone + "") {
            case "0": {
                timezone = "Europe/London";
                break;
            }
            case "1": {
                timezone = "Europe/Madrid";

                break;
            }
            case "-1": {
                timezone = "Europe/Madrid";
                break;
            }
            case "-2": {
                timezone = "Europe/Madrid";
                break;
            }
            default:
                timezone = "Europe/Madrid";

        }
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.TIMEZONE, timezone);
    }

    private void saveHotSpotValues(ResponseAllInfo body) {
        String ssid = body.configuration.ssid;
        String pass = body.configuration.pass;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.SSID, ssid);
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.PASS, pass);
    }

    private void startMoreAppsSubmenu(String args) {
        if (args != null) {
            int argsInt = Integer.parseInt(args);
            mySharedPreferences.putInt(Constants.SHARED_PREFERENCES.ID_MOREAPPS, argsInt);
        }

        Intent i = new Intent(getApplicationContext(), MoreAppsSubmenuActivity.class);
        startActivity(i);
    }


    private void infoCard(String args) {
        mySharedPreferences.putInt(Constants.SHARED_PREFERENCES.INFOCARD_INDEX, Integer.parseInt(args));
        Intent i = new Intent(getApplicationContext(), InfoCardActivity.class);
        startActivity(i);

    }

    private void startSubmenu(String argsSubmenu) {
        mySharedPreferences.putInt(Constants.SHARED_PREFERENCES.ID_SUBMENU, Integer.parseInt(argsSubmenu));
        Intent i = new Intent(getApplicationContext(), MoreAppsActivity.class);
        startActivity(i);
    }


    /** Antes lanzaba LanguageSelect; ahora el idioma se cambia desde la píldora en MainMenu. */
    public void changeLanguage(Context context) {
        // No-op: selector de idioma integrado en MainMenu (píldora expandible)
    }

    private void openWeb(String nPaquete) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(nPaquete));
        startActivity(browserIntent);
    }

    private void openCastTutorial(Context context) {
        String ssid = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.SSID);
        String pass = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.PASS);
        if (!ssid.equals("") && !pass.equals("")) {
            Intent i = new Intent(context, CastTutorial.class);
            startActivity(i);
        } else
            comingSoon(context);

    }

    private void openParentalControlDialog(Context context) {
        Dialog dialog = new Dialog(context); // Context, this, etc.
        dialog.setContentView(R.layout.fragment_exit_player);
        dialog.setTitle(R.string.app_name);
        EditText inputText = dialog.findViewById(R.id.input_control);
        TextView btnInput = dialog.findViewById(R.id.btn);
        btnInput.setOnFocusChangeListener((view, b) -> {
            if (b)
                btnInput.setTextColor(getResources().getColor(R.color.orange, getTheme()));
            else
                btnInput.setTextColor(getResources().getColor(R.color.md_grey_500, getTheme()));
        });
        btnInput.setOnClickListener(view -> {
            if (inputText.getText().toString() != null && !inputText.getText().toString().equals("") && inputText.getText().toString().equals("4314")) {
                if ((BuildConfig.ENVIRONMENT.equals(Constants.ENVIRONMENT.DEVELOP)))
                    startTvPremium("verion.desing.video.player.debug");
                else
                    startTvPremium("verion.desing.video.player");
            }else
                Toast.makeText(context,R.string.error_tv_premium, Toast.LENGTH_SHORT).show();

            dialog.dismiss();
        });
        dialog.show();
    }
}

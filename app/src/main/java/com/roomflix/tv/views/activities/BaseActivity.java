package com.roomflix.tv.views.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextClock;
import android.widget.Toast;
import android.widget.VideoView;

import com.mikepenz.iconics.Iconics;
// import com.mikepenz.iconics.context.IconicsContextWrapper; // Removed to fix Android 13 crash

import java.io.IOException;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;

import com.roomflix.tv.R;
import com.roomflix.tv.Constants;
import com.roomflix.tv.dragger.LauncherApplication;
import com.roomflix.tv.dragger.MySharedPreferences;
import com.roomflix.tv.helpers.FileHelper;
import com.roomflix.tv.helpers.ImageHelper;
import com.roomflix.tv.helpers.PermissionHelper;
import com.roomflix.tv.managers.DBManager;
import com.roomflix.tv.network.service.CallManager;
import com.orhanobut.logger.Logger;
import com.roomflix.tv.utils.Mac;
import com.roomflix.tv.utils.Utils;
import com.roomflix.tv.views.fragment.FragmentCodes;

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "baseActivity";
    @Inject
    public MySharedPreferences mySharedPreferences;
    @Inject
    public ImageHelper imageHelper;
    @Inject
    public FileHelper mFileHelper;
    @Inject
    public DBManager mDBManager;
    @Inject
    public CallManager call;
    @Inject
    PermissionHelper mPermissionHelper;
    public String code;
    private long lastKeyClick;    //Last key time
    public String macAddress;
    public AlertDialog dialog;
    public boolean hasError = false;
    public String localeLang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        Iconics.init(getApplicationContext());
        code = "";
        macAddress = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.MAC);
        localeLang = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        if (macAddress.equals("")) {
            macAddress = getMacAddress().replaceAll(":", "");
            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.MAC, macAddress);
        }
        //macAddress = "112233445566";

    }

    /** Antes lanzaba LanguageSelect; ahora el idioma se cambia desde la píldora en MainMenu. */
    public void goLangSelect() {
        // No-op: selector de idioma integrado en MainMenu (píldora expandible)
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // IconicsContextWrapper removed to fix NullPointerException on Android 13
        // super.attachBaseContext(IconicsContextWrapper.wrap(newBase));
        try {
            SharedPreferences sp = newBase.getSharedPreferences("hotelPlay", Context.MODE_PRIVATE);
            String selected = sp.getString(Constants.SHARED_PREFERENCES.SELECTED_LANGUAGE_CODE, "");
            String legacy = sp.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, "en");
            String lang = (selected != null && !selected.isEmpty()) ? selected : legacy;
            if (lang == null || lang.isEmpty()) lang = "en";
            java.util.Locale locale = new java.util.Locale(lang.toLowerCase());
            Context localized = Utils.createContextWithLocale(newBase, locale);
            super.attachBaseContext(localized);
        } catch (Exception e) {
            super.attachBaseContext(newBase);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public String getMacAddress() {
        try {
            String macAddress = Mac.getMACAddress("eth0");
            return macAddress;
        } catch (Exception e) {
            e.printStackTrace();
            return "NO:-:MAC";
        }
    }

    protected void goStreaming(final VideoView vidView, final String enlace) {
        Uri vidUri = Uri.parse(enlace);
        vidView.setVideoURI(vidUri);
        vidView.setOnPreparedListener(mp -> mp.setOnVideoSizeChangedListener((mp1, arg1, arg2) -> {
            mp1.start();
            vidView.setVisibility(View.VISIBLE);
        }));
        vidView.setOnCompletionListener(mp ->
                goStreaming(vidView, enlace));
        vidView.setOnErrorListener((mp, what, extra) -> {
            goStreaming(vidView, enlace);
            return true;
        });
        vidView.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END || what == MediaPlayer.MEDIA_ERROR_SERVER_DIED
                    || what == MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING || what == MediaPlayer.MEDIA_ERROR_IO
                    || what == MediaPlayer.MEDIA_ERROR_TIMED_OUT || what == MediaPlayer.MEDIA_ERROR_UNKNOWN
                    || what == MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING)
                goStreaming(vidView, enlace);
            return true;
        });
        vidView.start();
    }

    public void comingSoon(Context context) {
        try {
            runOnUiThread(() -> {
                Toast.makeText(context, "Próximamente - Coming Soon", Toast.LENGTH_SHORT).show();
            });

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void comingSoon(Context context, String str) {
        try {
            runOnUiThread(() -> Toast.makeText(context, str, Toast.LENGTH_SHORT).show());

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    protected void appOpener(String nPackage) {
        if (dialog != null)
            if (dialog.isShowing()) {
                dialog.dismiss();
                startPackage(nPackage);
                return;
            }

        dialog = new FragmentCodes().createDialog(this);
        dialog.setOnDismissListener(dialogInterface -> {
            startPackage(nPackage);
        });
        dialog.setOnCancelListener(dialogInterface -> startPackage(nPackage));
        runOnUiThread(() -> {

            dialog.show();
            new Thread(() -> {
                if (dialog != null) dialog.dismiss();
            });
        });
    }

    public void startPackage(String nPackage) {
        try {
            if (nPackage == null || nPackage.isEmpty()) {
                Logger.w("startPackage: Package name is null or empty");
                return;
            }

            String name = nPackage.trim().toLowerCase();
            String[] packageNames;

            Logger.d("startPackage: Intentando abrir package: " + name);

            // Diccionario de resiliencia RoomFlix (Big Five + fallback)
            switch (name) {
                case "netflix":
                    packageNames = new String[]{"com.netflix.ninja", "com.netflix.mediaclient"};
                    break;
                case "youtube":
                    packageNames = new String[]{"com.google.android.youtube.tv", "com.google.android.youtube"};
                    break;
                case "prime":
                case "amazon":
                case "primevideo":
                    packageNames = new String[]{"com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"};
                    break;
                case "disney":
                    packageNames = new String[]{"com.disney.disneyplus"};
                    break;
                case "hbo":
                case "max":
                    packageNames = new String[]{"com.hbo.hbonow", "com.wbd.stream"};
                    break;
                case "movistar":
                    packageNames = new String[]{"es.plus.androidtv", "es.movistar.plus"};
                    break;
                default:
                    // Si viene un package directo del panel, úsalo tal cual
                    packageNames = new String[]{nPackage.trim()};
                    break;
            }

            resolveAndLaunch(packageNames, name);
        } catch (Exception e) {
            // Error al lanzar app - mostrar feedback sin romper el launcher
            handleAppNotFound(nPackage != null ? nPackage : "APP");
            Logger.e(e, "startPackage: Error launching package: " + nPackage);
        }
    }

    /**
     * Resuelve y lanza el mejor package disponible para una app de streaming,
     * priorizando intents Leanback para Android TV.
     */
    private void resolveAndLaunch(String[] packageNames, String friendlyName) {
        if (packageNames == null || packageNames.length == 0) {
            handleAppNotFound(friendlyName);
            return;
        }

        Intent launchIntent = null;
        String foundPackage = null;

        for (String pkg : packageNames) {
            try {
                if (pkg == null || pkg.trim().isEmpty()) continue;

                // 1) Intent específico para TV (Leanback)
                launchIntent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);

                // 2) Fallback: intent normal de la app
                if (launchIntent == null) {
                    launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
                }

                if (launchIntent != null) {
                    foundPackage = pkg;
                    Logger.d("startPackage: Found launch intent for package: " + pkg);
                    break;
                } else {
                    Logger.d("startPackage: No launch intent found for package: " + pkg);
                }
            } catch (Exception e) {
                Logger.w("startPackage: Error checking package " + pkg + ": " + e.getMessage());
            }
        }

        if (launchIntent != null && foundPackage != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                Logger.d("startPackage: Successfully launched package: " + foundPackage);
            } catch (android.content.ActivityNotFoundException e) {
                Logger.e(e, "startPackage: ActivityNotFoundException for package: " + foundPackage);
                handleAppNotFound(friendlyName);
            }
        } else {
            handleAppNotFound(friendlyName);
            Logger.w("startPackage: No launch intent found for any package variant. Tried packages: " + java.util.Arrays.toString(packageNames));
        }
    }

    /**
     * Manejo UX de apps ausentes: diálogo RoomFlix en lugar de Toast genérico.
     */
    protected void handleAppNotFound(String friendlyName) {
        final String safeName = friendlyName != null ? friendlyName : "contenido";
        Logger.e("TELEMETRY", "App faltante: " + safeName);
        // UX requerido: no mostrar mensajes/toasts/diálogos en pantalla.
    }

    public void startTvPremium(String nPackage){
        Intent intent = getPackageManager().getLaunchIntentForPackage(nPackage);
        Bundle adult = new Bundle();
        adult.putBoolean("adult", true);
        intent.putExtra("adult", adult);
        startActivity(intent);
    }

    /**
     * Configura el reloj con formato 24 horas y timezone
     * Compatible con BaseTVActivity.kt para mantener consistencia
     * @param clock TextClock a configurar
     */
    protected void setupClock(TextClock clock) {
        if (clock == null) return;
        clock.setFormat12Hour(null);
        clock.setFormat24Hour("HH:mm");
        String timezone = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.TIMEZONE);
        if (timezone != null && !timezone.isEmpty()) {
            clock.setTimeZone(timezone);
        }
    }
}

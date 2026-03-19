package verion.desing.launcher.views.activities;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.roomflix.tv.BuildConfig;
import verion.desing.launcher.Constants;
import com.roomflix.tv.R;
import com.roomflix.tv.views.activities.MainMenu;
import verion.desing.launcher.listener.CallBackAllInfoCheck;
import verion.desing.launcher.listener.CallBackCheckConnection;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.callbacks.CallBackData;
import verion.desing.launcher.network.response.ResponseAllInfo;
import verion.desing.launcher.network.response.ResponseConfiguration;
import verion.desing.launcher.network.response.ResponseUpdate;
import verion.desing.launcher.utils.NetWorkUtils;
import verion.desing.launcher.views.fragment.FragmentExit;

public class NetworkBaseActivity extends BaseActivity {

    private static final String TAG = "NetworkBaseActivity";
    private String dateNow;
    private boolean inserting;
    private boolean dataSave;
    private String baseUrl;
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
                } else if (s.equals("503"))
                    callBackAllInfoCheck.dataNoChange();
                Log.d(TAG, "Code: " + s);
            }
        });
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
        for (int i = 0; i < body.languages.size(); i++) {
            if (body.languages.get(i).isDefault) {
                codeLangDefault = body.languages.get(i).code;
                defaultChannel = body.languages.get(i).channel;
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, codeLangDefault);
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
        Logger.d(function + "-" + args);
        switch (function) {
            case 1:
                exitApp(args);
                break;
            case 2:
                openWeb(args);
                break;
            case 3:
                startSubmenu(args);
                break;
            case 4:
                infoCard(args);
                break;
            case 5:
                startMoreAppsSubmenu(args);
                break;
            case 6:
                changeLanguage(this);
                break;
            case 7:
                openCastTutorial(this);
                break;
            case 8: {
                android.content.Context ctx = MainMenu.getContext();
                openParentalControlDialog(ctx != null ? ctx : this);
                break;
            }
            case 9:
                startPackage("verion.desing.verionweather");
                break;
            default:
                comingSoon(this);
        }
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

    private void exitApp(String nPackage) {
        if (dialog != null)
            if (dialog.isShowing()) {
                dialog.dismiss();
                //(nPackage);
                //startPackage(nPackage);
                return;
            }

        dialog = new FragmentExit().createDialog(this);
        dialog.setOnShowListener(dialogInterface -> {

            String adbServer = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.ADB_SERVER);

            OkHttpClient okHttpClient = new OkHttpClient();
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            formBodyBuilder.add("package", nPackage);
            formBodyBuilder.add("mac", macAddress);
            FormBody formBody = formBodyBuilder.build();
            Request.Builder builder = new Request.Builder();
            builder = builder.url("http://"+adbServer+"/device/open/clear");
            builder = builder.post(formBody);
            Request request = builder.build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    dialog.dismiss();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    dialog.dismiss();
                }
            });
        });
        dialog.setOnCancelListener(dialogInterface -> startPackage(nPackage));
        runOnUiThread(() -> {
            dialog.show();
            new Thread(() -> {
                if (dialog != null) dialog.dismiss();
            });
        });
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


    /** Antes lanzaba LanguageSelect (eliminado); idioma se cambia desde la píldora en MainMenu. */
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

package verion.desing.launcher.views.activities;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.logger.Logger;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import verion.desing.launcher.Constants;
import verion.desing.launcher.listener.CallBackAllInfoCheck;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.callbacks.CallBackData;
import verion.desing.launcher.network.response.ResponseAllInfo;

public class NetworkBaseActivity extends BaseActivity {

    private static final String TAG = "NetworkBaseActivity";
    private String dateNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void callAllInfo(CallBackAllInfoCheck callBackAllInfoCheck) {
        call.getDataFromServer(macAddress, new CallBackData<ResponseAllInfo>() {
            @Override
            public void finishAction(ResponseAllInfo body) {
                //if (isDataDifferent(body)) {
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.BASE_URL, body.baseUrl);
                    saveData(body, callBackAllInfoCheck);
                /*} else {
                    Logger.d("DATA NOT DIFFERENT");
                }*/
            }

            @Override
            public void error(String s) {
                Log.d(TAG, "Code: " + s);
            }
        });
    }


    private void saveBackground(ResponseAllInfo body) {
        String background = body.templates.background;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK, background);
    }

    private void saveBackgroundLanguages(ResponseAllInfo body) {
        String backgroundLanguages = body.templates.backgroundLanguages;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK_LANG, backgroundLanguages);
    }

    private void saveDefaultLanguage(ResponseAllInfo body) {
        String codeLangDefault;
        for (int i = 0; i < body.languages.size(); i++) {
            if (body.languages.get(i).isDefault) {
                codeLangDefault = body.languages.get(i).code;
                mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANG_DEFAULT, codeLangDefault);
                Log.d(TAG, codeLangDefault);
            }
        }
    }

    public String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE);
        Date date = new Date();
        dateNow = dateFormat.format(date);
        return dateNow;
    }


    private void saveData(ResponseAllInfo body, CallBackAllInfoCheck callBackAllInfoCheck) {
        Logger.d("DATA DIFFERENT");
        saveBackground(body);
        saveBackgroundLanguages(body);
        saveDefaultLanguage(body);
        insertDataBaseData(body, new CallBackSaveData() {
            @Override
            public void finish() {
                callBackAllInfoCheck.dataChange();
            }

            @Override
            public void error(String s) {
                callBackAllInfoCheck.error(s);
            }
        });
    }

    public synchronized void insertDataBaseData(final ResponseAllInfo body, CallBackSaveData callBackSaveData) {
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
        });
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
            /*String date = body.mac.devices.get(0).apk.getFecha();
            Log.d(TAG, "Different date: " + date + " shared preferences date: "
                    + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.APK_DATE));
            if (!date.equals(mySharedPreferences.getString(Constants.SHARED_PREFERENCES.APK_DATE))) {
                checkUpdate(body.mac.devices.get(0).apk.getApk(), body.mac.devices.get(0).apk.getPkg());
            }*/
            //clearGlideCache(this);
            return true;
        }
    }

    public synchronized void goFunction(int function, String args) {
        Logger.d(function + "-" + args);
        switch (function) {
            case 1:
                appOpener(args);
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
                break;
            case 6:
                changeLanguage(this);
                break;
            default:
                comingSoon(this);
        }
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


    public void changeLanguage(Context context) {
        finish();
        Intent i = new Intent(context, LanguageSelect.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void openWeb(String nPaquete) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(nPaquete));
        startActivity(browserIntent);
    }
}

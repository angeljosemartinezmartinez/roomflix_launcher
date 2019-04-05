package verion.desing.launcher.views.activities;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import verion.desing.launcher.Constants;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.listener.CallBackAllInfoCheck;
import verion.desing.launcher.listener.CallBackCheckConnection;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.callbacks.CallBackData;
import verion.desing.launcher.network.response.ResponseAllInfo;
import verion.desing.launcher.network.response.ResponseTemplates;
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
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if ((checkConnection(getApplicationContext()) && NetWorkUtils.isOnline()) || NetWorkUtils.isWifiOn(getApplicationContext()))
                    callBackCheckConnection.success();
                else {
                    if (!checkConnection(getApplicationContext()))
                        callBackCheckConnection.noConnection();
                    else
                        callBackCheckConnection.noPing();

                }


            }
        }, 0, 300000);
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
            String date = body.date;
            Log.d(TAG, "Different date: " + date + " shared preferences date: "
                    + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.APK_DATE));
            if (!date.equals(mySharedPreferences.getString(Constants.SHARED_PREFERENCES.APK_DATE))) {
                String apk = body.baseUrl + body.apk;
                checkUpdate(apk, body.pkg);
            }
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

    /**
     * Download and create a directory where is saved the app
     *
     * @param apk where is downloaded
     * @param pkg of the app
     */
    private void checkUpdate(String apk, String pkg) {
        new Thread(() -> {
            try {
                URL url = new URL(apk);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.connect();
                File apkStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        + "/" + "apks");
                if (!apkStorage.exists()) {
                    apkStorage.mkdir();
                }
                String apkDownloaded = apk.substring(38).trim();
                File outputFile = new File(apkStorage, apkDownloaded);
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(outputFile);
                InputStream is = c.getInputStream();
                byte[] buffer = new byte[1024];
                int len1;
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                }
                fos.close();
                is.close();
                installProcess(apkStorage.getAbsolutePath() + apkDownloaded, pkg);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Install an application downloaded from panel
     *
     * @param path where is specified apk
     * @param pkg  package of the app
     */
    public void installProcess(String path, String pkg) {
        Process pro1;
        try {
            pro1 = Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "chmod 777 " + path});
            try {
                pro1.waitFor();
            } catch (InterruptedException e) {
                Logger.d(e);
                e.printStackTrace();
            }
            Logger.d(path);
            Process pro2 = Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm install -r -d -g " + path});
            try {
                pro2.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.d(e);

            }
            Process pro3 = Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm install -r -i " + pkg + " -d -g  " + path});
            try {
                pro3.waitFor();
            } catch (InterruptedException e) {
                Logger.d(e);

                e.printStackTrace();
            }
        } catch (IOException e) {
            Logger.d(e);
            e.printStackTrace();
        }
    }

    /*private void getImgFromCall(ResponseAllInfo body) {
        ResponseTemplates mBtnList = body.templates;
        ArrayList<Translations> btns = getButtons(mBtnList);
        saveTemplateImages(btns);
        saveTemplateImagesFocused(btns);
    }

    private ArrayList<Translations> getButtons(ResponseTemplates templates) {
        ArrayList<Translations> pictures = new ArrayList<>();
        for (ResponseTemplates.Button button : templates.buttons) {
            for (ResponseTemplates.Button.Translations picture : button.pictures) {
                pictures.add(new Translations(0, "", picture.picture, picture.pictureFocused, 1, ""));
            }
        }
        return pictures;
    }

    public void saveTemplateImages(ArrayList<Translations> button) {
        ArrayList<String> img = new ArrayList<>();
        for (int i = 0; i < button.size(); i++) {
            if (!button.get(i).getPicture().equals("")) {
                img.add(baseUrl + button.get(i).getPicture());
            }
        }
        downloadImages(img, "button");
    }

    public void saveTemplateImagesFocused(ArrayList<Translations> buttonFocused) {
        ArrayList<String> focusImg = new ArrayList<>();
        for (int i = 0; i < buttonFocused.size(); i++) {
            if (!buttonFocused.get(i).getPictureFocused().equals("")) {
                focusImg.add(baseUrl + buttonFocused.get(i).getPictureFocused());
            }
        }
        downloadImages(focusImg, "button_focused");

    }*/

    /*private void downloadImages(ArrayList<String> img, String directory) {
        new Thread(() -> {
            for (int i = 0; i < img.size(); i++) {
                try {
                    URL url = new URL(img.get(i));
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET");
                    c.connect();
                    File imgStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            + "/" + directory);
                    if (!imgStorage.exists()) {
                        imgStorage.mkdir();
                    }
                    String imgDownloaded = img.get(i).replace("/", "");
                    File outputFile = new File(imgStorage, imgDownloaded.replace("http:", "")
                            .replace("hotelplay.tv", "").replace("demo", "")
                            .replace("img_new_api", ""));
                    if (!outputFile.exists()) {
                        outputFile.createNewFile();
                    }
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    InputStream is = c.getInputStream();
                    byte[] buffer = new byte[2048];
                    int len1;
                    while ((len1 = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len1);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
//                    c.disconnect();
                } catch (MalformedURLException e) {
                    Log.d(TAG, "URL format not valid");
                } catch (IOException e) {
                    Log.d(TAG, e.getLocalizedMessage());
                }
            }

        }).start();
    }*/

    private void saveBackground(ResponseAllInfo body) {
        String background = body.templates.background;
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_BACK, background);
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
        saveDefaultLanguage(body);
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
        if (args.equals("") && function != 6) {
            comingSoon(this);
            return;
        }
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
            default:
                comingSoon(this);
        }
    }

    private void startMoreAppsSubmenu(String args) {
        if(args != null) {
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
                startPackage(nPackage);
                return;
            }

        dialog = new FragmentExit().createDialog(this);
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

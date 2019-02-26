package verion.desing.launcher.views.activities;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import retrofit2.Call;
import retrofit2.Response;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.databinding.ActivityMainBinding;
import verion.desing.launcher.listener.CallBackAllInfoCheck;
import verion.desing.launcher.listener.CallBackArrayList;

public class MainMenu extends NetworkBaseActivity {

    private static final String TAG = "MainMenu";
    private ActivityMainBinding binding;
    private ArrayList<Button> buttons;
    private String baseUrl;
    private String background;
    private String langID;
    private ArrayList<Button> mButtonsList = new ArrayList<>();
    private ArrayList<Translations> mPicturesList = new ArrayList<>();
    private Handler autoHideLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
       // autoHideLoader = new Handler();
        langID = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        background = baseUrl + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK);
        setVideoView();
        setClock();
        executeCall();
        buttons = new ArrayList<>();
        checkPermission();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void executeCall() {
        callAllInfo(new CallBackAllInfoCheck() {
            @Override
            public void dataChange() {
                runOnUiThread(() ->{
                    generationMain();
                    hideLoader();
                });
            }

            @Override
            public void dataNoChange() {
                hideLoader();
                generationMain();
            }

            @Override
            public void error(String macAddress) {
                Log.d(TAG, "Error: " + macAddress);
            }
        });
    }

    private void generationMain() {
        setStreaming();
        setBackground();
        setPictures();
        openSettings("com.android.settings");
    }

    private void setStreaming() {
        goStreaming(binding.video, "/storage/emulated/0/Download/ARRECIFE_GRAN_HOTEL.mp4");
    }

    private void setBackground() {
        try{
            imageHelper.loadRoundCorner(background, binding.background);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setPictures() {
        if (langID.equals("")) {
            langID = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANG_DEFAULT);
            getTranslations(langID);
        } else {
            getTranslations(langID);
        }
    }

    private void getTranslations(String language) {
        Context context = getApplicationContext();
        mDBManager.getTranslationsFromLanguage(new CallBackArrayList<Translations>() {
            @Override
            public void finish(ArrayList<Translations> s) {
                if (s != null)
                    mPicturesList = s;
                setBtnImages(mPicturesList);
            }

            @Override
            public void error(String localizedMessage) {

            }
        }, context, language);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        executeCall();
    }

    private void setClock() {
        binding.toptextClock.setFormat12Hour(null);
        binding.toptextClock.setFormat24Hour("HH:mm");
        String timezone = "Europe/Madrid";
        if (timezone != "")
            binding.toptextClock.setTimeZone(timezone);
    }


    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    private void openSettings(String nPackage) {
        binding.icoSettings.setOnClickListener(view -> {
            startPackage(nPackage);
        });
        binding.icoSettings.setOnFocusChangeListener((view, b) -> {
            if (b) {
                binding.icoSettings.setTextColor(getResources().getColor(R.color.orange, getTheme()));
            } else
                binding.icoSettings.setTextColor(getResources().getColor(R.color.white, getTheme()));

        });
    }

    private void checkPermission() {
        String pkg = "verion.desing.launcher";
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.REAL_GET_TASKS"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.SET_TIME"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.READ_EXTERNAL_STORAGE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.WRITE_EXTERNAL_STORAGE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.INSTALL_PACKAGES"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.INSTANT_APP_FOREGROUND_SERVICE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.REQUEST_INSTALL_PACKAGES"});
            Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.WRITE_SETTINGS"});
            p.waitFor();
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.SET_TIME_ZONE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.SET_TIME"});
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }


    }

    private void setVideoView() {
        binding.video.setVisibility(View.VISIBLE);
        binding.video.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundResource(R.drawable.focus_video_view);
            } else {
                v.setBackgroundColor(80000000);
            }
        });
        binding.video.setOnClickListener(v -> {
            v.requestFocus();
        });
        binding.video.setOnTouchListener((v, event) -> {
                    v.callOnClick();
                    return false;
                }

        );
        binding.video.setBackgroundResource(R.drawable.focus_video_view);

        binding.video.setOnPreparedListener(mp -> mp.setOnVideoSizeChangedListener((mp1, arg1, arg2) -> {
            mp1.start();
            binding.video.setVisibility(View.VISIBLE);

        }));
        binding.video.requestFocus();

    }

    public void setBtnImages(ArrayList<Translations> translations) {
        if (translations == null) return;
        for (Translations translation : translations) {
            int i = translations.indexOf(translation);
            String btnID = "btn" + i;
            int resID = getResources().getIdentifier(btnID, "id", getPackageName());
            final ImageButton btnFor = binding.getRoot().findViewById(resID);
            btnFor.setVisibility(View.VISIBLE);
            runOnUiThread(() -> {
                btnFor.setOnFocusChangeListener((view, b) -> {
                    if (b)
                        imageHelper.loadRoundCorner(baseUrl + translation.getPictureFocused(), btnFor);
                    else
                        imageHelper.loadRoundCorner(baseUrl + translation.getPicture(), btnFor);
                });
                imageHelper.loadRoundCorner(baseUrl + translation.getPicture(), btnFor);
                btnFor.setOnClickListener(view -> {
                    goFunction(translation.getFunctionType(), translation.getFunctionTarget());
                });
            });
        }

    }





    private void setBtnFromDevice() {
        ArrayList<String> btns = new ArrayList<>();
        ArrayList<String> buttonsFocused = new ArrayList<>();
        File imgStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + "/" + "button");
        for (int i = 0; i < 17; i++) {

            if (imgStorage.exists()) {
                btns.add(imgStorage.getAbsolutePath() + "/" + imgStorage.listFiles()[i].getName());
            }
            File imgStorageFocused = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + "/" + "button_focused");
            if (imgStorageFocused.exists()) {
                buttonsFocused.add(imgStorageFocused.getAbsolutePath() + "/" + imgStorageFocused.listFiles()[i].getName());
            }
            //buttons.add(new Button(btns.get(i), buttonsFocused.get(i)));
        }
    }

    private void hideLoader(){
        runOnUiThread(() -> {
            binding.loader.setVisibility(View.GONE);
           // autoHideLoader.removeCallbacks(null);
        });
    }

    private void showLoader(){
        runOnUiThread(() -> {
            binding.loader.setVisibility(View.VISIBLE);
           // autoHideLoader.removeCallbacks(null);
        });
    }

}

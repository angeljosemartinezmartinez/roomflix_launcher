package verion.desing.launcher.views.activities;

import android.app.AlarmManager;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import verion.desing.launcher.BuildConfig;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.databinding.ActivityMainBinding;
import verion.desing.launcher.helpers.PermissionHelper;
import verion.desing.launcher.listener.CallBackAllInfoCheck;
import verion.desing.launcher.listener.CallBackArrayList;
import verion.desing.launcher.listener.CallBackCheckConnection;
import verion.desing.launcher.utils.KeyCodesConverter;
import verion.desing.launcher.utils.Utils;

public class MainMenu extends NetworkBaseActivity {

    private static final String TAG = "MainMenu";
    public static Context context;
    private ActivityMainBinding binding;
    private ArrayList<verion.desing.launcher.model.Button> buttons;
    private String baseUrl;
    private String background;
    private String langID;
    private ArrayList<Translations> mPicturesList = new ArrayList<>();
    private Handler autoHideLoader;
    private long lastKeyClick;
    private boolean timeSet;//if timeformat is change or not
    private boolean executingCall;
    private long lastTime;
    private Handler waitForNetwork = new Handler();
    private boolean restart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        langID = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        context = this;
        Utils.changeAppLanguage(new Locale(langID.toLowerCase()), this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        autoHideLoader = new Handler();
        executingCall = false;
        restart = false;
        setClock();
        buttons = new ArrayList<>();
        checkPermission();
        if (mPermissionHelper.hasWriteSettingsPermission(this)) timeFormat();
        else mPermissionHelper.askSettingPermission(this);
    }

    private void timeFormat() {
        String timezone =
                mySharedPreferences.getString(Constants.SHARED_PREFERENCES.TIMEZONE);

        if (timeSet) return;
        try {
            Settings.System.getInt(getContentResolver(), Settings.System.TIME_12_24);
            Settings.System.putString(this.getContentResolver(), Settings.System.TIME_12_24, "12");
            timeSet = true;
            AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            if (timezone.equals(""))
                am.setTimeZone("Europe/Madrid");
            else
                am.setTimeZone(timezone);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCasesConnection();
        binding.video.start();
        closeWeather();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.video.pause();
        executingCall = false;
    }

    private void checkCasesConnection() {
        checkCasesConnection(new CallBackCheckConnection() {
            @Override
            public void success() {
                executeCall();
            }

            @Override
            public void noPing() {
            }

            @Override
            public void noConnection() {
            }
        });
    }

    public void executeCall() {
        showLoader();
        callUpdate();
        callAllInfo(new CallBackAllInfoCheck() {
            @Override
            public void dataChange() {
                runOnUiThread(() -> {
                    generationMain();
                    hideLoader();
                });
            }

            @Override
            public void dataNoChange() {
                generationMain();
                hideLoader();
            }

            @Override
            public void error(String macAddress) {
                if (macAddress.equals("403")) {
                    hideLoader();
                    runOnUiThread(() -> imageHelper.loadRoundCorner(R.drawable.dispositivo_alta_es, binding.background));
                }
            }
        });
    }


    private void generationMain() {
        setDay(binding.help.day);
        setMySharedPreferencesData();
        setStreaming();
        setBackground();
        setPictures();
    }

    private void setMySharedPreferencesData() {
        langID = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        background = baseUrl + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK);
    }

    private void setStreaming() {
        goStreaming(binding.video, mySharedPreferences.getString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL));
        setVideoView();
    }

    private void setBackground() {
        try {
            imageHelper.loadRoundCorner(background, binding.background);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPictures() {
        imageHelper.loadRoundCorner(mySharedPreferences.getString(Constants.SHARED_PREFERENCES.MINI_LOGO), binding.help.logo);
        getTranslations(mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID));
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
        restart = true;
        checkCasesConnection();
        binding.video.start();
        hideLoader();
    }

    private void setClock() {
        binding.help.toptextClock.setFormat12Hour(null);
        binding.help.toptextClock.setFormat24Hour("HH:mm");
        String timezone = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.TIMEZONE);
        if (timezone != "")
            binding.help.toptextClock.setTimeZone(timezone);
    }


    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    public void openDialog() {
        Dialog dialog = new Dialog(MainMenu.this); // Context, this, etc.
        dialog.setContentView(R.layout.fragment_settings);
        dialog.setTitle(R.string.app_name);
        EditText inputText = dialog.findViewById(R.id.input_text);
        TextView btnInput = dialog.findViewById(R.id.btn);
        btnInput.setOnFocusChangeListener((view, b) -> {
            if (b)
                btnInput.setTextColor(getResources().getColor(R.color.orange, getTheme()));
            else
                btnInput.setTextColor(getResources().getColor(R.color.md_grey_500, getTheme()));
        });
        btnInput.setOnLongClickListener(view -> {
            if (inputText.getText().toString() != null && !inputText.getText().toString().equals(""))
                launchCode(inputText.getText().toString());
            dialog.dismiss();
            return true;
        });
        dialog.show();
    }



    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        if (action == KeyEvent.ACTION_DOWN && KeyCodesConverter.isNumber(keyCode)) {
            setCode(KeyCodesConverter.convertKeyCodeToNumber(keyCode));
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean setCode(int number) {
        Logger.d(number);
        if (System.currentTimeMillis() - lastKeyClick < 1000) {
            code = code + String.valueOf(number);
            if (code.length() == 7) {
                Logger.d(code);
                launchCode(code);
                code = "";
                return true;
            }
        } else {
            code = String.valueOf(number);
        }
        lastKeyClick = System.currentTimeMillis();
        return false;
    }

    private void launchCode(String value) {
        switch (value.trim()) {
            /*case "143":
                startPackage("com.android.settings");
                break;*/
            case Constants.Codes.SETTINGS:
                startPackage("com.android.settings");
                break;
            case Constants.Codes.INSTALLER: {
                appOpener("com.droidlogic.appinstall");
                break;
            }
            case Constants.Codes.TEAMVIEWER: {
                appOpener("com.teamviewer.host.market");
                break;
            }
            case Constants.Codes.MARKET: {
                appOpener("com.android.vending");
                break;
            }
            case Constants.Codes.AUTOREBOOT: {
                appOpener("com.pereira.autoreboot");
                break;
            }
            case Constants.Codes.REBOOT: {
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "reboot"});
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            case Constants.Codes.DROP: {
                mySharedPreferences.deleteAll();
                try {
                    Runtime runtime = Runtime.getRuntime();
                    runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "rm -r /data/dalvik-cache"});
                    runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "rm -r /cache/dalvik-cache"});
                    runtime.exec(new String[]{"/system/bin/su", "-c", "rm -r data/data/verion.desing.launcher/databases/"});
                    String packageName = getApplicationContext().getPackageName();
                    runtime.exec(new String[]{"/system/bin/su", "-c", "pm clear " + packageName});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            case Constants.Codes.MAC: {
                Toast.makeText(this, macAddress, Toast.LENGTH_LONG).show();
                break;
            }
            case Constants.Codes.DROID_SETTINGS: {
                appOpener("com.droidlogic.tv.settings");
                break;
            }

            case Constants.Codes.SHOW_IP: {
                String ip = Utils.getIPAddress(true);
                Toast.makeText(this, ip, Toast.LENGTH_LONG).show();
                break;
            }
            case Constants.Codes.TCP_IP: {
                try {
                    Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "setprop service.adb.tcp.port 5555"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

    }

    private void checkPermission() {
        String pkg = (BuildConfig.ENVIRONMENT.equals(Constants.ENVIRONMENT.DEVELOP)) ? "verion.desing.launcher.debug" : "verion.desing.launcher";
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.READ_EXTERNAL_STORAGE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.WRITE_EXTERNAL_STORAGE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.INSTALL_PACKAGES"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.INSTANT_APP_FOREGROUND_SERVICE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.SET_TIME_ZONE"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.REQUEST_INSTALL_PACKAGES"});
            Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c",
                    "pm grant " + pkg + "  android.permission.WRITE_SETTINGS"});
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }


    }

    private void setVideoView() {
        binding.video.setVisibility(View.VISIBLE);
        binding.video.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.verTv.setVisibility(View.VISIBLE);
                v.setBackgroundResource(R.drawable.focus_video_view);
            } else {
                binding.verTv.setVisibility(View.INVISIBLE);
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
        if (BuildConfig.ENVIRONMENT.equals(Constants.ENVIRONMENT.DEVELOP))
            binding.video.setOnClickListener(view -> startPackage("verion.desing.video.player.debug"));
        else
            binding.video.setOnClickListener(view -> startPackage("verion.desing.video.player"));

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
                        imageHelper.loadRoundCorner(baseUrl + translation.getPictureFocused(), btnFor, MainMenu.this);
                    else
                        imageHelper.loadRoundCorner(baseUrl + translation.getPicture(), btnFor, MainMenu.this);
                });
                imageHelper.loadRoundCorner(baseUrl + translation.getPicture(), btnFor, MainMenu.this);
                btnFor.setOnClickListener(view -> {
                    goFunction(translation.getFunctionType(), translation.getFunctionTarget());
                });
            });
        }

    }

    public void setBtnImagesFromDevice(ArrayList<verion.desing.launcher.model.Button> translations) {
        if (translations == null) return;
        for (verion.desing.launcher.model.Button translation : translations) {
            int i = translations.indexOf(translation);
            String btnID = "btn" + i;
            int resID = getResources().getIdentifier(btnID, "id", getPackageName());
            final ImageButton btnFor = binding.getRoot().findViewById(resID);
            btnFor.setVisibility(View.VISIBLE);
            runOnUiThread(() -> {
                btnFor.setOnFocusChangeListener((view, b) -> {
                    if (b)
                        imageHelper.loadRoundCorner(translation.getImgFocused(), btnFor);
                    else
                        imageHelper.loadRoundCorner(translation.getImg(), btnFor);
                });
                imageHelper.loadRoundCorner(translation.getImg(), btnFor);
            });
        }

    }

    private void hideLoader() {
        runOnUiThread(() -> {
            binding.loader.setVisibility(View.GONE);
            autoHideLoader.removeCallbacks(null);
        });
    }

    private void showLoader() {
        runOnUiThread(() -> {
            binding.loader.setVisibility(View.VISIBLE);
            autoHideLoader.removeCallbacks(null);
        });
    }

    private void closeWeather(){
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "am force-stop verion.desing.verionweather"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

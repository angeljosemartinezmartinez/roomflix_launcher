package verion.desing.launcher.views.activities;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.databinding.DataBindingUtil;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.databinding.ActivityMainBinding;
import verion.desing.launcher.listener.CallBackAllInfoCheck;
import verion.desing.launcher.listener.CallBackArrayList;
import verion.desing.launcher.listener.CallBackCheckConnection;
import verion.desing.launcher.utils.KeyCodesConverter;
import verion.desing.launcher.utils.Utils;

public class MainMenu extends NetworkBaseActivity {

    private static final String TAG = "MainMenu";
    private ActivityMainBinding binding;
    private ArrayList<verion.desing.launcher.model.Button> buttons;
    private String baseUrl;
    private String background;
    private String langID;
    private ArrayList<Button> mButtonsList = new ArrayList<>();
    private ArrayList<Translations> mPicturesList = new ArrayList<>();
    private Handler autoHideLoader;
    private long lastKeyClick;
    private boolean executingCall;
    private long lastTime;
    private Handler waitForNetwork = new Handler();

    @Override
    protected void onPause() {
        super.onPause();
        executingCall = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        autoHideLoader = new Handler();
        showLoader();
        executingCall = false;
        setClock();
        buttons = new ArrayList<>();
        checkPermission();
        waitAndExecute();
    }

    public synchronized void waitAndExecute() {
        if ((System.currentTimeMillis() - lastTime) < 20000) return;
        if (executingCall) return;
        executingCall = true;
        Logger.d("WAIT AND EXECUTE CALL ");
        waitForNetwork.removeCallbacksAndMessages(null);
        waitForNetwork.postDelayed(this::checkCasesConnection, 3000);
        lastTime = System.currentTimeMillis();
    }

    private void checkCasesConnection() {
        checkCasesConnection(new CallBackCheckConnection() {
            @Override
            public void success() {
                executeCall();
            }

            @Override
            public void noPing() {
                hideLoader();
                runOnUiThread(() -> binding.background.setBackgroundColor(getResources().getColor(R.color.transparent, getTheme())));
                runOnUiThread(() -> connectionError("no ping"));
            }

            @Override
            public void noConnection() {
                hideLoader();
                runOnUiThread(() -> connectionError("no connection"));
            }
        });
    }


    private void setDay() {
        Date myDate = new Date();
        SimpleDateFormat dmyFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.FRENCH);
        String dmy = dmyFormat.format(myDate);
        binding.day.setText(dmy);
    }

    public void executeCall() {
        showLoader();
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
                Log.d(TAG, "Error: " + macAddress);
            }
        });
    }

    private void generateFromDevice() {
        setBtnFromDevice();
        imageHelper.loadRoundCorner("/storage/emulated/0/Download/language_background/demoimgfondofondoHotelplay.png", binding.background);
        setStreaming();
    }

    private void generationMain() {
        setDay();
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
        goStreaming(binding.video, "/storage/emulated/0/Download/ARRECIFE_GRAN_HOTEL.mp4");
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
        checkCasesConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            case "143":
                startPackage("com.android.settings");
                break;
            case "1431430":
                openDialog();
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
            buttons.add(new verion.desing.launcher.model.Button(btns.get(i), buttonsFocused.get(i)));
        }
        setBtnImagesFromDevice(buttons);
    }

    private void setBtnFromDevice(ArrayList<Translations> translations) {
        mDBManager.setButtonImages(translations, getApplicationContext(), new CallBackArrayList<Translations>() {
            @Override
            public void finish(ArrayList<Translations> listButtons) {
                File imgStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        + "/" + "button");
                if (imgStorage.exists()) {
                    for (int i = 1; i < imgStorage.list().length; i++) {
                        String img = baseUrl + listButtons.get(i).getPicture();
                        String imgReplaced = img.replace("/", "").replace("http:", "")
                                .replace("hotelplay.tv", "").replace("demo", "")
                                .replace("img_new_api", "");
                        if (imgReplaced.equals(imgStorage.listFiles()[i].getName())) {
                            Log.d(TAG, "Img: " + imgReplaced);
                            listButtons.get(i).setPicture(imgStorage.getAbsolutePath() + "/" + imgStorage.listFiles()[i].getName());
                        } else
                            listButtons.get(i).setPicture(listButtons.get(i).getPicture());
                    }
                } else {
                    for (int i = 0; i < translations.size(); i++) {
                        translations.get(i).setPicture(listButtons.get(i).getPicture());
                    }
                }
            }

            @Override
            public void error(String localizedMessage) {
            }
        });
        mDBManager.setButtonImages(translations, getApplicationContext(), new CallBackArrayList<Translations>() {
            @Override
            public void finish(ArrayList<Translations> listButtons) {
                File imgStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        + "/" + "button_focused");
                if (imgStorage.exists()) {
                    for (int i = 1; i < imgStorage.list().length; i++) {
                        String img = baseUrl + listButtons.get(i).getPictureFocused();
                        String imgReplaced = img.replace("/", "").replace("http:", "")
                                .replace("hotelplay.tv", "").replace("demo", "")
                                .replace("img_new_api", "");
                        if (imgReplaced.equals(imgStorage.listFiles()[i].getName())) {
                            Log.d(TAG, "Focused: " + imgReplaced);
                            listButtons.get(i).setPictureFocused(imgStorage.getAbsolutePath() + "/" + imgStorage.listFiles()[i].getName());
                        }
                    }
                } else {
                    for (int i = 0; i < listButtons.size(); i++) {
                        translations.get(i).setPictureFocused(listButtons.get(i).getPictureFocused());
                    }
                }

            }

            @Override
            public void error(String localizedMessage) {
            }
        });
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

}

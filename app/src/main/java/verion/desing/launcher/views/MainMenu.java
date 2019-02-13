package verion.desing.launcher.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import verion.desing.launcher.R;
import verion.desing.launcher.databinding.ActivityMainBinding;
import verion.desing.launcher.model.Button;
import verion.desing.launcher.network.service.CallManager;
import verion.desing.launcher.network.service.callbacks.CallBackData;

public class MainMenu extends AppCompatActivity {

    private static final String TAG = "MainMenu";
    private ActivityMainBinding binding;
    private ArrayList<Button> buttons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        buttons = new ArrayList<>();
        Iconics.init(getApplicationContext());
        checkPermission();
        String background = "/storage/emulated/0/Download/language_background/demoimgfondofondoHotelplay.png";
        loadRoundCorner(background, binding.background);
        openSettings("com.android.settings");
        setBtnFromDevice();
        setBtnViews(buttons);
        goStream("/storage/emulated/0/Download/ARRECIFE_GRAN_HOTEL.mp4");
        setVideoView();
        setClock();
        //getDataFromServer();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        goStream("/storage/emulated/0/Download/ARRECIFE_GRAN_HOTEL.mp4");
        setVideoView();
        setClock();
    }

    private void setClock() {
        binding.toptextClock.setFormat12Hour(null);
        binding.toptextClock.setFormat24Hour("HH:mm");
        String timezone = "2";
        if (timezone != "")
            binding.toptextClock.setTimeZone(timezone);

    }

    private void getDataFromServer(){
        CallManager call = new CallManager();
        call.getDataFromServer("900EB3068F9A", new CallBackData() {
            @Override
            public void finishAction(Object body) {
            }

            @Override
            public void error(String s) {

            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(IconicsContextWrapper.wrap(newBase));
    }


    private void openSettings(String nPackage){
        binding.icoSettings.setOnClickListener(view -> {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(nPackage);
                if (launchIntent != null) {
                    startActivity(launchIntent);//null pointer check in case package nombre was not found
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        binding.icoSettings.setOnFocusChangeListener((view, b) -> {
            if(b){
                binding.icoSettings.setTextColor(getResources().getColor(R.color.orange, getTheme()));
            }else
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

    protected void goStream(final String response) {
        if (response == null) return;
        binding.video.suspend(); // clears media player
        runOnUiThread(() -> {
            binding.video.setVideoURI(Uri.parse(response));
            binding.video.requestFocus();
            binding.video.setOnErrorListener((mp, what, extra) -> {
                Logger.e(mp + "-" + what + "-" + extra);
                mp.start();
                goStream(response);
                return true;
            });
            binding.video.setOnCompletionListener((mediaPlayer) -> {
                goStream(response);
            });

            binding.video.setOnInfoListener((mp, what, extra) -> {
                if (what > 700) {
                    mp.start();
                }
                return false;
            });
            binding.video.setVisibility(View.VISIBLE);
            binding.video.requestFocus();

        });
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (binding.video.isPlaying()) {
                    long start = System.nanoTime();
                    binding.video.suspend();
                    runOnUiThread(() -> goStream(response));
                    long finish = System.nanoTime();
                    double time = (finish - start) / 1000000000.0;
                    double temp = ((double) ((int) (time * 100.0))) / 100.0;
                    Log.d(TAG, "Time: " + temp + " s");
                }
            }
        }, 0, 3600000);
        binding.video.start();
    }

    private void setVideoView() {
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
//        binding.video.requestFocus();

    }

    public void setBtnViews(ArrayList<Button> buttons) {
        if (buttons == null) return;
        for (Button button : buttons) {
            runOnUiThread(() -> {
                int i = buttons.indexOf(button);
                String btnID = "btn" + i;
                int resID = getResources().getIdentifier(btnID, "id", getPackageName());
                final ImageButton btnFor = binding.getRoot().findViewById(resID);
                btnFor.setVisibility(View.VISIBLE);
                btnFor.setOnFocusChangeListener((view, b) -> {
                    if (b) {
                        Log.d(TAG, "Focused: " + i + " btn: " + button.getImgFocused());
                        loadRoundCorner(button.getImgFocused(), btnFor);
                    } else {
                        Log.d(TAG, "Not focused: " + i + " btn: " + button.getImg());
                        loadRoundCorner(button.getImg(), btnFor);
                    }
                });
                loadRoundCorner(button.getImg(), btnFor);
            });
        }
        binding.btn11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LanguageSelect.class));
            }
        });

    }

    public void loadRoundCorner(String url, ImageButton view) {

        Context context = view.getContext();
        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void loadRoundCorner(String url, ImageView view) {
        Context context = view.getContext();
        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
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
            buttons.add(new Button(btns.get(i), buttonsFocused.get(i)));
        }
    }


}

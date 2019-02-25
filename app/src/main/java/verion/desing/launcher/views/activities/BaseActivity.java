package verion.desing.launcher.views.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import verion.desing.launcher.Constants;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.dragger.LauncherApplication;
import verion.desing.launcher.dragger.MySharedPreferences;
import verion.desing.launcher.helpers.FileHelper;
import verion.desing.launcher.helpers.ImageHelper;
import verion.desing.launcher.listener.CallBackAllInfoCheck;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.managers.DBManager;
import verion.desing.launcher.network.callbacks.CallBackData;
import verion.desing.launcher.network.response.ResponseAllInfo;
import verion.desing.launcher.network.service.CallManager;
import verion.desing.launcher.views.fragment.FragmentCodes;

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
    CallManager call;
    public String code;
    public String macAddress;
    public AlertDialog dialog;
    public boolean inserting;
    public boolean dataSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        Iconics.init(getApplicationContext());
        code = "";
        macAddress = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.MAC);
        if (macAddress.equals("")) {
            macAddress = getMacAddress().replaceAll(":", "");
            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.MAC, macAddress);
        }
        inserting = false;
        dataSave = false;
        //macAddress = "C44EAC158C1C";

    }

    public void goLangSelect() {
        Intent intent = new Intent(getApplicationContext(), LanguageSelect.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }



    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        /*if (action == KeyEvent.ACTION_DOWN && KeyCodesConverter.isNumber(keyCode)) {
            setCode(KeyCodesConverter.convertKeyCodeToNumber(keyCode));
            return false;
        }*/
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(IconicsContextWrapper.wrap(newBase));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public String getMacAddress() {
        try {
            return mFileHelper.loadFileAsString("/sys/class/net/eth0/address")
                    .toUpperCase().substring(0, 17);
        } catch (IOException e) {
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
                    || what == MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING || what == MediaPlayer.MEDIA_ERROR_SERVER_DIED)
                goStreaming(vidView, enlace);
            return true;
        });
        /*new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (vidView.isPlaying()) {
                    long start = System.nanoTime();
                    vidView.suspend();
                    runOnUiThread(() -> goStreaming(vidView, enlace));
                    long finish = System.nanoTime();
                    double time = (finish - start) / 1000000000.0;
                    Log.d(TAG, "Time: " + time + " s");
                }
            }
        }, 0, 3600000);*/

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

    protected void cleanCache() {
        try {
//            Glide.getPhotoCacheDir(this).delete();
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm clear com.netflix.mediaclient"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm clear com.amazon.avod.thirdpartyclient"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm clear es.plus.yomvi"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm clear com.nousguide.android.rbtv"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm clear rtve.tablet.android"});
            Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "pm clear com.google.android.youtube.tv"});
        } catch (IOException e) {
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
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(nPackage);
            if (launchIntent != null) {
                startActivity(launchIntent);//null pointer check in case package nombre was not found
            }
        } catch (Exception e) {
            comingSoon(this);
            e.printStackTrace();
        }
    }
}

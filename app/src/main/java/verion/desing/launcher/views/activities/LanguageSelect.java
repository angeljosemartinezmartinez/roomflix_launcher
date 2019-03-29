package verion.desing.launcher.views.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.databinding.ActivityIdiomasBinding;
import verion.desing.launcher.dragger.LauncherApplication;
import verion.desing.launcher.helpers.ImageHelper;
import verion.desing.launcher.listener.CallBackArrayList;
import verion.desing.launcher.listener.CallBackViewEvents;
import verion.desing.launcher.utils.Utils;
import verion.desing.launcher.views.adapter.LanguageAdapter;

public class LanguageSelect extends BaseActivity {

    private static final String TAG = "LanguageSelect";
    private ActivityIdiomasBinding binding;
    private String baseURl;
    @Inject
    ImageHelper imageHelper;
    private String background;
    private Handler autoHideLoader;
    private String logo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_idiomas);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        autoHideLoader = new Handler();
        deleteDirectoryContent();
        baseURl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        logo = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LOGO);
        background = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK_LANG);
        imageHelper.loadRoundCorner(baseURl + background, binding.background);
        imageHelper.loadRoundCorner(logo, binding.logo);
        hideLoader();
        getLanguageButtons();
    }

    public void getLanguageButtons() {
        mDBManager.getLanguages(new CallBackArrayList<Languages>() {
            @Override
            public void finish(ArrayList<Languages> s) {
                runOnUiThread(() -> setLanguageView(s));
            }

            @Override
            public void error(String localizedMessage) {
                Logger.d(localizedMessage);
            }
        }, this);
    }

    public void setLanguageView(ArrayList<Languages> s) {
        binding.recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recycler.setAdapter(new LanguageAdapter(s, (CallBackViewEvents<Languages>) (language, v) -> {
            String IDLanguage = language.getCode();
            final String backgroundLang = baseURl + language.getPicture();
            String defaultChannel = language.getChannel();
            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_LANG, backgroundLang);
            selectLang(IDLanguage, backgroundLang, defaultChannel);
        }, baseURl));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        hideLoader();
        deleteDirectoryContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLanguageButtons();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    private synchronized void selectLang(String idLangSelected, final String langBackground, final String defaultChannel) {
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, idLangSelected);
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_LANG, langBackground);
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.DEFAULT_CHANNEL, defaultChannel);
        try {
            Utils.changeAppLanguage(new Locale(idLangSelected.toUpperCase(), idLangSelected.toLowerCase()), this);
            createFileLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        openMainMenu(this);
    }

    public void createFileLocation() {
        File locationStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + "/" + "location");
        if (!locationStorage.exists()) {
            locationStorage.mkdir();
        }
        String language = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        String file = language + ".txt";
        File outputFile = new File(locationStorage, file);
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void deleteDirectoryContent() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + "/" + "location");
        if (file.exists()) {
            String[] entries = file.list();
            for (String s : entries) {
                new File(file.getPath(), s).delete();
            }
        }
        try {
            Runtime.getRuntime().exec(new String[]{"system/bin/su", "-c", " rm -rf /storage/emulated/0/Download/location/"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openMainMenu(Context context) {
        Intent i;
        i = new Intent(context, MainMenu.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        showLoader();
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

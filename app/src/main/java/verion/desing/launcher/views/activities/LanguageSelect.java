package verion.desing.launcher.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.orhanobut.logger.Logger;

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

public class LanguageSelect extends NetworkBaseActivity {

    private static final String TAG = "LanguageSelect";
    private ActivityIdiomasBinding binding;
    private String baseURl;
    @Inject
    ImageHelper imageHelper;
    private String background;
    private Handler autoHideLoader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_idiomas);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        autoHideLoader = new Handler();
        baseURl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        background = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK_LANG);
        imageHelper.loadRoundCorner(baseURl + background, binding.background);
        hideLoader();
        getLanguageButtons();
    }

    public void getLanguageButtons() {
        mDBManager.getLanguages(new CallBackArrayList<Languages>() {
            @Override
            public void finish(ArrayList<Languages> s) {
                runOnUiThread(() -> setLanguageView(s));
                Log.d(TAG, "Finish");
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
            mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_LANG, backgroundLang);
            selectLang(IDLanguage, backgroundLang);
        }, baseURl));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        hideLoader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLanguageButtons();
    }

    private synchronized void selectLang(String idLangSelected, final String langBackground) {
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, idLangSelected);
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_LANG, langBackground);
        try {
            Utils.change_setting(new Locale(idLangSelected.toUpperCase(), idLangSelected.toLowerCase()), this);

        } catch (Exception e) {
            e.printStackTrace();
        }
        openMainMenu();
    }

    public void openMainMenu() {
        Intent i;
        i = new Intent(getApplicationContext(), MainMenu.class);
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

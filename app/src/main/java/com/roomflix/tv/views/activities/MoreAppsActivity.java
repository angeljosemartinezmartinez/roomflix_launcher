package com.roomflix.tv.views.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.roomflix.tv.Constants;
import com.roomflix.tv.R;
import com.roomflix.tv.database.models.TranslationSubmenu;
import com.roomflix.tv.database.tables.Button;
import com.roomflix.tv.database.tables.Submenus;
import com.roomflix.tv.database.tables.Translations;
import com.roomflix.tv.databinding.ActivityMoreAppsBinding;
import com.roomflix.tv.dragger.LauncherApplication;
import com.roomflix.tv.listener.CallBackGetOne;
import com.roomflix.tv.managers.DBManager;
import com.roomflix.tv.views.adapter.ChildAdapter;
import com.roomflix.tv.views.adapter.MoreAppsAdapter;

public class MoreAppsActivity extends NetworkBaseActivity {

    public static final String TAG = "MOREAPPS";
    private ActivityMoreAppsBinding binding;
    @Inject
    DBManager mDBManager;
    private Submenus mSubmenu;
    private ArrayList<Button> mBtnList;
    private String background;
    private String baseUrl;
    private ArrayList<Translations> mTranslationsList;
    private int idSubmenu;
    private String actualLang;
    private Handler autoHideLoader;
    private String logo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_more_apps);
        mBtnList = new ArrayList<>();
        mTranslationsList = new ArrayList<>();
        autoHideLoader = new Handler();
        showLoader();
        chargeSharedPreferencesData();
        imageHelper.loadRoundCorner(background, binding.background);
        imageHelper.loadRoundCorner(logo, binding.help.logo);
        idSubmenu = mySharedPreferences.getInt(Constants.SHARED_PREFERENCES.ID_SUBMENU);
        getSubmenuFromBtn();
        // setClock() y setDay() - ELIMINADOS: El reloj antiguo ya no existe
    }

    // setClock() - ELIMINADO: El reloj antiguo (toptextClock) ya no existe en el layout

    private void chargeSharedPreferencesData() {
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        background = baseUrl + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK);
        actualLang = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        logo = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.MINI_LOGO);
        Log.d(TAG, "logo: " + logo);
    }

    private void getSubmenuFromBtn() {
        mDBManager.getSubmenu(new CallBackGetOne<Submenus>() {
            @Override
            public void finish(Submenus s) {
                runOnUiThread(() -> {
                    if (s != null)
                        mSubmenu = s;
                    mBtnList = mSubmenu.getButtons();
                    if (mBtnList != null)
                        getTranslations(mBtnList);
                    getTitle(mSubmenu.getTranslations());
                    setFirstPage();
                    setSecondPage();
                    hideLoader();
                });
            }

            @Override
            public void error(String localizedMessage) {

            }
        }, this, idSubmenu);
    }

    private void getTranslations(ArrayList<Button> btnList) {
        for (Button btn : btnList) {
            for (Translations translations : btn.getPictures()) {
                if (translations.getLocale().equals(actualLang)) {
                    mTranslationsList.add(translations);
                }
            }
        }

    }

    private void getTitle(ArrayList<TranslationSubmenu> translationList) {
        for (TranslationSubmenu trans : translationList) {
            if (trans.getLanguage().equals(actualLang)) {
                binding.title.setText(trans.getTitle());
            }
        }

    }

    private void setFirstPage() {
        binding.recyclerTop.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerTop.setAdapter(new MoreAppsAdapter(filterItemsFirstPage(), buttonSelected -> {
            try {
                if (buttonSelected != null) {
                    goFunction(buttonSelected.getFunctionType(),
                            buttonSelected.getFunctionTarget());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, baseUrl));
    }

    private void setSecondPage() {
        binding.recyclerBottom.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerBottom.setAdapter(new ChildAdapter(filterItemsSecondPage(), buttonSelected -> {
            try {
                if (buttonSelected != null) {
                    goFunction(buttonSelected.getFunctionType(),
                            buttonSelected.getFunctionTarget());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, baseUrl));
    }

    private ArrayList<Translations> filterItemsFirstPage() {
        ArrayList<Translations> itemsTemp = new ArrayList<>();
        for (Translations item : mTranslationsList) {
            if ((mTranslationsList.indexOf(item) == 0
                    || mTranslationsList.indexOf(item) == 1
                    || mTranslationsList.indexOf(item) == 3
                    || mTranslationsList.indexOf(item) == 5) && itemsTemp.size() < 4) {
                itemsTemp.add(item);
            }
        }
        return itemsTemp;
    }

    private ArrayList<Translations> filterItemsSecondPage() {
        ArrayList<Translations> itemsTemp = new ArrayList<>();
        for (Translations item : mTranslationsList) {
            if ((mTranslationsList.indexOf(item) == 2
                    || mTranslationsList.indexOf(item) == 4
                    || mTranslationsList.indexOf(item) == 6
                    || mTranslationsList.indexOf(item) == 7) && itemsTemp.size() < 4) {
                itemsTemp.add(item);
            }
        }
        return itemsTemp;
    }

    private void hideLoader() {
        runOnUiThread(() -> {
            binding.loader.getRoot().setVisibility(View.GONE);
            autoHideLoader.removeCallbacks(null);
        });
    }

    private void showLoader() {
        runOnUiThread(() -> {
            binding.loader.getRoot().setVisibility(View.VISIBLE);
            autoHideLoader.removeCallbacks(null);
        });
    }
}

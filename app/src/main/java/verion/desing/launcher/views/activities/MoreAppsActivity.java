package verion.desing.launcher.views.activities;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.Submenus;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.databinding.ActivityMoreAppsBinding;
import verion.desing.launcher.dragger.LauncherApplication;
import verion.desing.launcher.listener.CallBackGetOne;
import verion.desing.launcher.managers.DBManager;
import verion.desing.launcher.views.adapter.ChildAdapter;
import verion.desing.launcher.views.adapter.MoreAppsAdapter;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_more_apps);
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        background = baseUrl + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK);
        actualLang = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        imageHelper.loadRoundCorner(background, binding.background);
        mBtnList = new ArrayList<>();
        mTranslationsList = new ArrayList<>();
        idSubmenu = mySharedPreferences.getInt(Constants.SHARED_PREFERENCES.ID_SUBMENU);
        getSubmenuFromBtn();
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
                    setFirstPage();
                    setSecondPage();
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
        ArrayList<Translations> itemsFiltered = new ArrayList<>();
        ArrayList<Translations> itemsTemp = new ArrayList<>();
        for (Translations item : mTranslationsList) {
            itemsFiltered.add(item);
            if (itemsFiltered.size() <= 4)
                itemsTemp.add(item);
        }
        return itemsTemp;
    }

    private ArrayList<Translations> filterItemsSecondPage() {
        ArrayList<Translations> itemsFiltered = new ArrayList<>();
        ArrayList<Translations> itemsTemp = new ArrayList<>();
        for (Translations item : mTranslationsList) {
            itemsFiltered.add(item);
            if ((itemsFiltered.indexOf(item) > 3 && itemsFiltered.indexOf(item) <= 7) && itemsTemp.size() < 4){
                itemsTemp.add(item);
            }
        }
        return itemsTemp;
    }
}

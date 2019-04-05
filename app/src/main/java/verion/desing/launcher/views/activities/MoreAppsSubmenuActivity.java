package verion.desing.launcher.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.Submenus;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.databinding.ActivityMoreAppsSubmenuBinding;
import verion.desing.launcher.dragger.LauncherApplication;
import verion.desing.launcher.listener.CallBackGetOne;
import verion.desing.launcher.managers.DBManager;
import verion.desing.launcher.views.adapter.MoreAppsSubmenuAdapter;

public class MoreAppsSubmenuActivity extends NetworkBaseActivity {

    public static final String TAG = "MOREAPPSSUBMENU";
    private ActivityMoreAppsSubmenuBinding binding;
    private Submenus mSubmenu;
    private ArrayList<Button> mBtnList;
    private int idSubmenu;
    private String actualLang;
    private ArrayList<Translations> mTranslationsList;
    private String background;
    private String baseUrl;
    @Inject
    DBManager mDBManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_more_apps_submenu);
        mBtnList = new ArrayList<>();
        mTranslationsList = new ArrayList<>();
        chargeSharedPreferencesData();
        idSubmenu = mySharedPreferences.getInt(Constants.SHARED_PREFERENCES.ID_MOREAPPS);
        imageHelper.loadRoundCorner(background, binding.background);
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

                    setRecycler();
                });
            }

            @Override
            public void error(String localizedMessage) {

            }
        }, this, idSubmenu);
    }

    private void setRecycler() {
        binding.recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recycler.setAdapter(new MoreAppsSubmenuAdapter(filterItems(), buttonSelected -> {
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

    private ArrayList<Translations> filterItems() {
        ArrayList<Translations> itemsFiltered = new ArrayList<>();
        ArrayList<Translations> itemsTemp = new ArrayList<>();
        for (Translations item : mTranslationsList) {
            itemsFiltered.add(item);
            if (itemsFiltered.size() <= 4)
                itemsTemp.add(item);
        }
        return itemsTemp;
    }

    private void chargeSharedPreferencesData() {
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        background = baseUrl + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK);
        actualLang = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
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
}



package com.roomflix.tv.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import com.roomflix.tv.Constants;
import com.roomflix.tv.R;
import com.roomflix.tv.database.models.TranslationSubmenu;
import com.roomflix.tv.database.tables.Button;
import com.roomflix.tv.database.tables.Submenus;
import com.roomflix.tv.database.tables.Translations;
import com.roomflix.tv.databinding.ActivityMoreAppsSubmenuBinding;
import com.roomflix.tv.dragger.LauncherApplication;
import com.roomflix.tv.listener.CallBackGetOne;
import com.roomflix.tv.managers.DBManager;
import com.roomflix.tv.views.adapter.MoreAppsSubmenuAdapter;

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
    private String logo;
    private MoreAppsSubmenuAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_more_apps_submenu);
        mBtnList = new ArrayList<>();
        mTranslationsList = new ArrayList<>();
        chargeSharedPreferencesData();
        // ID puede venir por SharedPreferences (flujo clásico) o por Intent (diagnóstico / deep links)
        int idFromPrefs = mySharedPreferences.getInt(Constants.SHARED_PREFERENCES.ID_MOREAPPS);
        int idFromIntent = getIntent() != null ? getIntent().getIntExtra(Constants.SHARED_PREFERENCES.ID_MOREAPPS, 0) : 0;
        idSubmenu = idFromIntent != 0 ? idFromIntent : idFromPrefs;
        imageHelper.loadRoundCorner(background, binding.background);
        imageHelper.loadRoundCorner(logo, binding.help.logo);

        // Evitar "No adapter attached": montar RecyclerView desde el inicio (vacío)
        binding.recycler.setVisibility(android.view.View.VISIBLE);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new MoreAppsSubmenuAdapter(new ArrayList<>(), buttonSelected -> {
            try {
                if (buttonSelected != null) {
                    goFunction(buttonSelected.getFunctionType(), buttonSelected.getFunctionTarget());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, baseUrl, false);
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);

        getSubmenuFromBtn();
        // setClock() y setDay() - ELIMINADOS: El reloj antiguo ya no existe

    }

    // setClock() - ELIMINADO: El reloj antiguo (toptextClock) ya no existe en el layout

    private void getSubmenuFromBtn() {
        mDBManager.getSubmenu(new CallBackGetOne<Submenus>() {
            @Override
            public void finish(Submenus s) {
                runOnUiThread(() -> {
                    // Log de diagnóstico solicitado
                    int foundButtons = 0;
                    if (s != null && s.getButtons() != null) {
                        foundButtons = s.getButtons().size();
                    }
                    Log.d("ROOMFLIX_DEBUG", "Cargando submenú ID: " + idSubmenu + " | Botones encontrados: " + foundButtons);

                    // Si el submenú no existe o viene vacío, no crashear: pintar recycler vacío con feedback
                    if (s == null || s.getButtons() == null || s.getButtons().isEmpty()) {
                        mSubmenu = s; // puede ser null; mantenemos referencia para evitar NPEs
                        mBtnList = new ArrayList<>();
                        mTranslationsList.clear();
                        // Título opcional si hay traducciones; si no, dejar el actual
                        if (s != null && s.getTranslations() != null) {
                            getTitle(s.getTranslations());
                        }
                        setRecycler();
                        return;
                    }

                    mSubmenu = s;
                    mBtnList = mSubmenu.getButtons();
                    mTranslationsList.clear();
                    getTranslations(mBtnList);
                    if (mSubmenu.getTranslations() != null) {
                        getTitle(mSubmenu.getTranslations());
                    }
                    setRecycler();
                });
            }

            @Override
            public void error(String localizedMessage) {
                runOnUiThread(() -> {
                    Log.d("ROOMFLIX_DEBUG", "Cargando submenú ID: " + idSubmenu + " | Botones encontrados: 0 (error DB: " + localizedMessage + ")");
                    mBtnList = new ArrayList<>();
                    mTranslationsList.clear();
                    setRecycler();
                });
            }
        }, this, idSubmenu);
    }

    private void setRecycler() {
        // Verificación de visibilidad: nunca dejar el RecyclerView en GONE
        binding.recycler.setVisibility(android.view.View.VISIBLE);

        ArrayList<Translations> items = new ArrayList<>(mTranslationsList);
        if (idSubmenu == 123) {
            GridLayoutManager glm = new GridLayoutManager(this, 4);
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    // El primer botón (Piscina) ocupa 2 columnas, el resto 1.
                    return (position == 0) ? 2 : 1;
                }
            });
            binding.recycler.setLayoutManager(glm);
            adapter.setGridMode(true);
            // En mosaico no necesitamos padding de centrado horizontal
            binding.recycler.setClipToPadding(true);
            binding.recycler.setPadding(0, binding.recycler.getPaddingTop(), 0, binding.recycler.getPaddingBottom());
        } else {
            // Para el ID 95 (4 botones), mantener diseño horizontal centrado
            binding.recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            adapter.setGridMode(false);

            if (items.size() > 0 && items.size() <= 4) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int screenWidthPx = dm.widthPixels;
                float density = dm.density;
                int itemWidthPx = (int) ((212f + 6f) * density); // 212dp + márgenes 3dp*2
                int totalPx = itemWidthPx * items.size();
                int pad = Math.max(0, (screenWidthPx - totalPx) / 2);
                binding.recycler.setClipToPadding(false);
                binding.recycler.setPadding(pad, binding.recycler.getPaddingTop(), pad, binding.recycler.getPaddingBottom());
            } else {
                binding.recycler.setClipToPadding(true);
                binding.recycler.setPadding(0, binding.recycler.getPaddingTop(), 0, binding.recycler.getPaddingBottom());
            }
        }

        // Forzar refresco de datos + animación suave + repintado
        binding.recycler.setAdapter(adapter);
        adapter.setItems(items);
        adapter.notifyDataSetChanged();
        binding.recycler.scheduleLayoutAnimation();
        binding.recycler.requestLayout();
    }

    private void chargeSharedPreferencesData() {
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        background = baseUrl + mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK);
        actualLang = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        logo = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.MINI_LOGO);
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
}



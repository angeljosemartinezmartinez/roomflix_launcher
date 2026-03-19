package com.roomflix.tv.views.activities;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.roomflix.tv.Constants;
import com.roomflix.tv.R;
import com.roomflix.tv.database.models.Translation;
import com.roomflix.tv.database.tables.InfoCards;
import com.roomflix.tv.databinding.ActivityInfocardBinding;
import com.roomflix.tv.dragger.LauncherApplication;
import com.roomflix.tv.listener.CallBackGetOne;
import com.roomflix.tv.managers.DBManager;
import com.roomflix.tv.views.fragment.InfoCardImage;

public class InfoCardActivity extends NetworkBaseActivity {

    private static final String TAG = "InfoCardActivity";
    @Inject
    DBManager mDBManager;
    ActivityInfocardBinding binding;
    private int idInfocard;
    private InfoCards infoCard;
    private ArrayList<Translation> mTranslationsList;
    private ArrayList<InfoCards> childs;
    private String idLanguage;
    private String baseUrl;
    private String background;
    private String logo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_infocard);
        ((LauncherApplication) getApplicationContext()).getAppComponent().inject(this);
        mTranslationsList = new ArrayList<>();
        childs = new ArrayList<>();
        idInfocard = mySharedPreferences.getInt(Constants.SHARED_PREFERENCES.INFOCARD_INDEX);
        idLanguage = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.LANGUAGE_ID);
        generateUI();
        getInfocard();

    }

    // setClock() - ELIMINADO: El reloj antiguo (toptextClock) ya no existe en el layout

    private void generateUI() {
        background = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.URL_BACK);
        baseUrl = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.BASE_URL);
        logo = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.MINI_LOGO);
        imageHelper.loadRoundCorner(baseUrl + background, binding.background);
        imageHelper.loadRoundCorner(logo, binding.help.logo);
        // setClock() y setDay() - ELIMINADOS: El reloj antiguo ya no existe
    }

    private void getInfocard() {
        mDBManager.getInfoCard(new CallBackGetOne<InfoCards>() {
            @Override
            public void finish(InfoCards template) {
                runOnUiThread(() -> {
                    if (template != null) {
                        infoCard = template;
                        childs = infoCard.getChild();
                        Collections.reverse(childs);
                        childs.add(template);
                        Collections.reverse(childs);
                        setChilds(infoCard.getChild());
                        if(childs.size() == 1){
                            binding.indicator.setVisibility(View.INVISIBLE);
                        }
                        ScreenSlidePagerAdapter myAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), mTranslationsList);
                        binding.viewPager.setAdapter(myAdapter);
                        binding.indicator.setViewPager(binding.viewPager);
                    }
                });
            }

            @Override
            public void error(String localizedMessage) {

            }
        }, getApplicationContext(), idInfocard);
    }

    private void setChilds(ArrayList<InfoCards> childs) {
        for (InfoCards infoCards : childs) {
            for (Translation child : infoCards.getTranslations()) {
                if (child.getLanguage().equals(idLanguage)) {
                    mTranslationsList.add(child);
                }
            }
        }

    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        private ArrayList<Translation> mItems;

        public ScreenSlidePagerAdapter(FragmentManager fm, ArrayList<Translation> items) {
            super(fm);
            mItems = items;
        }


        @Override
        public Fragment getItem(int position) {
            Translation card = mItems.get(position);
            Fragment fragment = InfoCardImage.newInstance(card, baseUrl);

            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return String.valueOf(position);
        }

        @Override
        public int getCount() {
            if (mItems != null) {
                return mItems.size();
            }
            return 0;
        }
    }

}

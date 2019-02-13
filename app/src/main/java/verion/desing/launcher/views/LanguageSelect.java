package verion.desing.launcher.views;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.models.Data;
import verion.desing.launcher.database.tables.Language;
import verion.desing.launcher.databinding.ActivityIdiomasBinding;
import verion.desing.launcher.listener.CallBackArrayList;
import verion.desing.launcher.listener.CallBackViewEvents;
import verion.desing.launcher.network.service.callbacks.CallBackData;
import verion.desing.launcher.utils.Utils;
import verion.desing.launcher.views.activities.BaseActivity;
import verion.desing.launcher.views.adapter.LanguageAdapter;

public class LanguageSelect extends BaseActivity {

    private static final String TAG = "LanguageSelect";
    private ActivityIdiomasBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_idiomas);
        String background = "/storage/emulated/0/Download/language_select_background/demoimgfondofondoHotelplayIdioma.png";
        loadRoundCorner(background, binding.background);
        getLanguageButtons();
    }

    public void loadRoundCorner(String url, ImageView view) {
        Context context = view.getContext();
        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void getLanguageButtons() {
        mDBManager.getLanguages(new CallBackData<Language>() {
            @Override
            public void finishAction(Language s) {
                runOnUiThread(() -> {
                    setLanguageView(s.getData());
                    Log.d(TAG, "Finish");
                });
            }

            @Override
            public void error(String localizedMessage) {
                Logger.d(localizedMessage);
            }
        }, this);
    }

    public void setLanguageView(ArrayList<Data> s) {
        binding.recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recycler.setAdapter(new LanguageAdapter(s, new CallBackViewEvents<Data>() {
            @Override
            public void click(Data data, View v) {
                for(int i = 0;i<s.size();i++){
                    String IDLanguage = data.getCode();
                    final String backgroundLang = Constants.SHARED_PREFERENCES.BASE_URL + data.getPicture();
                    mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_LANG, backgroundLang);
                    selectLang(IDLanguage, backgroundLang);
                }

            }

            @Override
            public void focus(Data item, View v) {

            }

            @Override
            public void unFocus(Data item, View v) {

            }
        }));
    }

    private synchronized void selectLang(String idLangSelected, final String langBackground) {
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.LANGUAGE_ID, idLangSelected);
        mySharedPreferences.putString(Constants.SHARED_PREFERENCES.URL_LANG, langBackground);
        mySharedPreferences.putBoolean(Constants.SHARED_PREFERENCES.SMARTMODE, true);
        try {
            Utils.change_setting(new Locale(idLangSelected.toUpperCase(), idLangSelected.toLowerCase()), this);
            Log.d(TAG, idLangSelected);

        } catch (Exception e) {
            e.printStackTrace();
        }
        binding.recycler.setVisibility(View.GONE);
        openMainMenu();
    }

}

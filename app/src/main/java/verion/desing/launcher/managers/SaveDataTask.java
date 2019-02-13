package verion.desing.launcher.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.models.Data;
import verion.desing.launcher.database.tables.Language;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.response.ResponseLanguages;

public class SaveDataTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "SaveDataTask";
    private final Context context;
    private CallBackSaveData listener;
    private ArrayList<ResponseLanguages> data;
    private AppDataBase appDatabase;

    SaveDataTask(Context context, CallBackSaveData listener, ArrayList<ResponseLanguages> data) {
        this.listener = listener;
        this.data = data;
        this.context = context;
        appDatabase = AppDataBase.Companion.getInstance(context);

    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        boolean ok = false;
        try {
            if (appDatabase == null || !appDatabase.isOpen())
                appDatabase = AppDataBase.Companion.getInstance(context);
            saveLang(data, appDatabase);

            ok = true;
        } catch (Exception e) {
            appDatabase.close();
            AppDataBase.Companion.destroyInstance();
            e.printStackTrace();
            listener.error(e.getLocalizedMessage());
            listener.error("S");
        }


        return ok;
    }


    private void saveLang(ArrayList<ResponseLanguages> languages, AppDataBase appDatabase) {
        ArrayList<Language> langs = new ArrayList<>();
        ArrayList<Data> listData = new ArrayList<>();
        for (ResponseLanguages lang : languages) {
            for(ResponseLanguages.Data data : lang.data){
                listData.add(new Data(data.name, data.nativeName, data.code, data.texts));
            }
            langs.add(new Language(lang.statusCode, lang.baseUrl, lang.picture, listData));
        }
        if (langs.size() > 0) {
            appDatabase.languageDao().insertAll(langs);
            Log.d(TAG, "SAVE " + langs.size() + " LANGS");


        }
    }
}

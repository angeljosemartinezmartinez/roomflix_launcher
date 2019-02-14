package verion.desing.launcher.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.models.Language;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.response.ResponseLanguages;

public class SaveDataTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "SaveDataTask";
    private final Context context;
    private CallBackSaveData listener;
    private ResponseLanguages data;
    private AppDataBase appDatabase;

    SaveDataTask(Context context, CallBackSaveData listener, ResponseLanguages data) {
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


    private void saveLang(ResponseLanguages languages, AppDataBase appDatabase) {
        Languages langs;
        ArrayList<Language> listData = new ArrayList<>();
        for (ResponseLanguages.Languages data : languages.languages) {
            listData.add(new Language(data.nativeName, data.code, data.picture));
        }
        langs =new Languages(0,languages.baseUrl, listData);
            appDatabase.languageDao().insertAll(langs);
            Log.d(TAG, "SAVE " + langs.toString() + " LANGS");



    }
}

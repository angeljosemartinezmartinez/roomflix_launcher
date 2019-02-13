package verion.desing.launcher.managers;

import android.content.Context;

import java.util.ArrayList;

import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.tables.Language;
import verion.desing.launcher.listener.CallBackArrayList;

public class DBManager {

    private static final String TAG = "DBMANAGER";
    public AppDataBase appDatabase;

    public DBManager() {
    }


    public void getLanguages(final CallBackArrayList<Language> listener, final Context context) {

        open(context);

        new Thread(() -> {
            // a potentially  time consuming task
            try {
                ArrayList<Language> languages = (ArrayList<Language>) appDatabase.languageDao().getAll();
                if (languages.size() > 0)
                    listener.finish(languages);
                else
                    listener.error("no languages found");
            } catch (NullPointerException e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        }).start();

    }

    private synchronized void open(Context context) {
        try {
            if (appDatabase == null || !appDatabase.isOpen())
                appDatabase = AppDataBase.Companion.getInstance(context);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }
}

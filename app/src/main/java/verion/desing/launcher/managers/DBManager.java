package verion.desing.launcher.managers;

import android.content.Context;

import java.util.ArrayList;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.listener.CallBackArrayList;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.response.ResponseLanguages;
import verion.desing.launcher.network.service.callbacks.CallBackData;

@Module
public class DBManager {

    private static final String TAG = "DBMANAGER";
    public AppDataBase appDatabase;

    public DBManager() {
    }

    @Provides
    @Singleton
    public synchronized DBManager provideCallManager() {
        return new DBManager();
    }

    public void saveData(final ResponseLanguages data, final Context context, final CallBackSaveData listener) {
        open(context);
        SaveDataTask task = new SaveDataTask(context, listener, data);

        task.execute();
    }

    public void getLanguages(final CallBackData<Languages> listener, final Context context) {

        open(context);

        new Thread(() -> {
            // a potentially  time consuming task
            try {
                Languages languages = (Languages) appDatabase.languageDao().getAll();
                if (languages != null)
                    listener.finishAction(languages);
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

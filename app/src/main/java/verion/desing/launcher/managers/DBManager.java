package verion.desing.launcher.managers;

import android.content.Context;

import java.util.ArrayList;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.database.tables.Pictures;
import verion.desing.launcher.database.tables.Submenus;
import verion.desing.launcher.database.tables.Templates;
import verion.desing.launcher.listener.CallBackArrayList;
import verion.desing.launcher.listener.CallBackGetOne;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.response.ResponseAllInfo;

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

    public void saveData(final ResponseAllInfo data, final Context context, final CallBackSaveData listener) {
        open(context);
        SaveDataTask task = new SaveDataTask(context, listener, data);

        task.execute();
    }

    public void getLanguages(CallBackArrayList<Languages> listener, Context context) {
        open(context);
        new Thread(() -> {
            try {
                ArrayList<Languages> languages = (ArrayList<Languages>) appDatabase.languageDao().getAll();
                if (languages != null)
                    listener.finish(languages);
                else
                    listener.error("no languages found");
            } catch (NullPointerException e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        }).start();

    }

    public void getButtonsFromTemplate(CallBackArrayList<Button> listener, Context context) {
        open(context);
        new Thread(() -> {
            try {
                ArrayList<Button> buttons = (ArrayList<Button>) appDatabase.buttonDao().getAll();
                listener.finish(buttons);

            } catch (Exception e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        }).start();

    }

    public void getPicturesFromLanguage(CallBackArrayList<Pictures> listener, Context context, String id) {
        open(context);
        new Thread(() -> {
            try {
                ArrayList<Pictures> pictures = (ArrayList<Pictures>) appDatabase.picturesDao().getOne(id);
                listener.finish(pictures);

            } catch (Exception e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        }).start();
    }

    public void getTemplate(final CallBackGetOne<Templates> listener, final Context context, String langID) {

        open(context);

        new Thread(() -> {
            // a potentially  time consuming task
            try {
                Templates templates = (Templates) appDatabase.templateDao().getOne(langID);
                if (templates != null)
                    listener.finish(templates);
                else
                    listener.error("no templates found");
            } catch (NullPointerException e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        }).start();

    }

    public void getSubmenu(final CallBackArrayList<Submenus> listener, final Context context) {

        open(context);

        new Thread(() -> {
            // a potentially  time consuming task
            try {
                ArrayList<Submenus> submenus = (ArrayList<Submenus>) appDatabase.submenuDao().getAll();
                if (submenus != null)
                    listener.finish(submenus);
                else
                    listener.error("no submenus found");
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

    public void delete(Context c) {
        new Thread(() -> {
            open(c);
            appDatabase.clearAllTables();
        }).start();


    }
}

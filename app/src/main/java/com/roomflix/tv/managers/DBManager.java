package com.roomflix.tv.managers;

import android.content.Context;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import com.roomflix.tv.database.AppDataBase;
import com.roomflix.tv.database.tables.Button;
import com.roomflix.tv.database.tables.InfoCards;
import com.roomflix.tv.database.tables.Languages;
import com.roomflix.tv.database.tables.Submenus;
import com.roomflix.tv.database.tables.Templates;
import com.roomflix.tv.database.tables.Translations;
import com.roomflix.tv.listener.CallBackArrayList;
import com.roomflix.tv.listener.CallBackGetOne;
import com.roomflix.tv.listener.CallBackSaveData;
import com.roomflix.tv.network.response.ResponseAllInfo;
import com.roomflix.tv.network.response.ResponseUpdate;

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

    public void saveData(final ResponseAllInfo data, final Context context, final CallBackSaveData listener, final ResponseUpdate update) {
        open(context);
        SaveDataTask task = new SaveDataTask(context, listener, data, update);
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

    public void getTranslationsFromLanguage(CallBackArrayList<Translations> listener, Context context, String id) {
        open(context);
        new Thread(() -> {
            try {
                ArrayList<Translations> pictures = (ArrayList<Translations>) appDatabase.picturesDao().getOne(id);
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

    public void getSubmenu(CallBackGetOne<Submenus> listener,Context context, int id) {
        open(context);
        new Thread(() -> {
            // a potentially  time consuming task
            try {
                Submenus submenus = (Submenus) appDatabase.submenuDao().getOne(id);
                if (submenus != null)
                    listener.finish(submenus);
                else
                    listener.error("no buttons in submenu found");
            } catch (NullPointerException e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        }).start();
    }

    /**
     * Obtiene todos los IDs de submenús (rejilla de botones) para decidir si un target abre submenu o carrusel.
     */
    public void getSubmenuIds(CallBackArrayList<String> listener, Context context) {
        open(context);
        new Thread(() -> {
            try {
                java.util.List<Submenus> list = appDatabase.submenuDao().getAll();
                ArrayList<String> ids = new ArrayList<>();
                if (list != null) {
                    for (Submenus s : list) {
                        if (s.getId() != null) ids.add(String.valueOf(s.getId()));
                    }
                }
                listener.finish(ids);
            } catch (Exception e) {
                Log.e(TAG, "getSubmenuIds", e);
                listener.error(e.getLocalizedMessage());
            }
        }).start();
    }

    public void getInfoCard(final CallBackGetOne<InfoCards> listener, final Context context, int id) {
        open(context);
        new Thread(() -> {
            // a potentially  time consuming task
            try {
                InfoCards infoCards = appDatabase.infoCardDao().getOne(id);
                if (infoCards != null)
                    listener.finish(infoCards);
                else
                    listener.error("no infocard found");
            } catch (NullPointerException e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        }).start();
    }

    public void setButtonImages(ArrayList<Translations> b, Context context, CallBackArrayList<Translations> callBackUpdateButton){
        new Thread( () ->{
            try {
                open(context);
                appDatabase.picturesDao().updateAll(b);
                callBackUpdateButton.finish(b);
            } catch (Exception e) {
                e.printStackTrace();
                callBackUpdateButton.error(e.getLocalizedMessage());
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

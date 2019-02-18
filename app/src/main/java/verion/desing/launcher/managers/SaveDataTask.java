package verion.desing.launcher.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;

import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.models.FocusPictures;
import verion.desing.launcher.database.models.Pictures;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.database.tables.Submenus;
import verion.desing.launcher.database.tables.Templates;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.response.ResponseAllInfo;
import verion.desing.launcher.network.response.ResponseLanguages;
import verion.desing.launcher.network.response.ResponseSubmenu;
import verion.desing.launcher.network.response.ResponseTemplates;

public class SaveDataTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "SaveDataTask";
    private final Context context;
    private CallBackSaveData listener;
    private ResponseAllInfo data;
    private AppDataBase appDatabase;

    SaveDataTask(Context context, CallBackSaveData listener, ResponseAllInfo data) {
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
            saveLang(data.languages, appDatabase);
            saveTemplate(data.templates, appDatabase);
            saveButtons(data.templates, appDatabase);
            saveSubmenu(data.submenus, appDatabase);

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

    @Override
    protected void onPostExecute(Boolean aVoid) {
        super.onPostExecute(aVoid);
        appDatabase.close();
        AppDataBase.Companion.destroyInstance();
        if (aVoid)
            listener.finish();


    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        appDatabase.close();
        AppDataBase.Companion.destroyInstance();
    }


    private void saveLang(ArrayList<ResponseLanguages> languages, AppDataBase appDatabase) {
        ArrayList<Languages> langs = new ArrayList<>();
        for (ResponseLanguages data : languages) {
            langs.add(new Languages(languages.indexOf(data), data.nativeName, data.code, data.picture));
        }
        appDatabase.languageDao().insertAll(langs);
        Log.d(TAG, "SAVE " + langs.size() + " LANGS");

    }

    private void saveTemplate(ResponseTemplates responseTemplates, AppDataBase appDataBase) {
        Templates templates;
        ArrayList<Button> buttons = new ArrayList<>();
        Pictures pictures;
        FocusPictures focusPictures;
        for (ResponseTemplates.Button button : responseTemplates.buttons) {
            Field variableName[] = ResponseTemplates.Button.Pictures.class.getDeclaredFields();
            for (int i = 0; i < variableName.length; i++) {
                pictures = new Pictures(button.pictures.es, button.pictures.en, button.pictures.de, button.pictures.fr);
                focusPictures = new FocusPictures(button.picturesFocused.es, button.picturesFocused.en, button.picturesFocused.de, button.picturesFocused.fr);
                buttons.add(new Button(button.position, button.position, pictures, focusPictures, button.functionType, button.functionTarget, variableName[i].getName()));
            }
        }
        templates = new Templates(0, responseTemplates.logo, responseTemplates.background, responseTemplates.backgroundLanguages, buttons);
        appDataBase.templateDao().insertAll(templates);
        Log.d(TAG, "SAVE " + templates.toString() + " TEMPLATE");
    }

    private void saveButtons(ResponseTemplates responseTemplates, AppDataBase appDataBase) {
        for (ResponseTemplates.Button button : responseTemplates.buttons) {
            Pictures pictures;
            FocusPictures focusPictures;
            ArrayList<Button> buttons = new ArrayList<>();
            Field variableName[] = ResponseTemplates.Button.Pictures.class.getDeclaredFields();
            for (int i = 0; i < variableName.length; i++) {
                pictures = new Pictures(button.pictures.es, button.pictures.en, button.pictures.de, button.pictures.fr);
                focusPictures = new FocusPictures(button.picturesFocused.es, button.picturesFocused.en, button.picturesFocused.de, button.picturesFocused.fr);
                buttons.add(new Button(button.position, button.position, pictures, focusPictures, button.functionType, button.functionTarget, variableName[i].getName()));
            }
            appDataBase.buttonDao().insertAll(buttons);
            Log.d(TAG, "SAVE " + buttons.size() + " BUTTONS");
        }

    }

    private void saveSubmenu(ArrayList<ResponseSubmenu> responseSubmenus, AppDataBase appDataBase) {
        ArrayList<Submenus> submenus = new ArrayList<>();
        ArrayList<Button> buttons = new ArrayList<>();
        Pictures pictures;
        FocusPictures focusPictures;
        for (ResponseSubmenu submenu : responseSubmenus) {
            for (ResponseSubmenu.Button button : submenu.buttons) {
                Field variableName[] = ResponseSubmenu.Button.Pictures.class.getDeclaredFields();
                for (int i = 0; i < variableName.length; i++) {
                    pictures = new Pictures(button.pictures.es, button.pictures.en, button.pictures.de, button.pictures.fr);
                    focusPictures = new FocusPictures(button.picturesFocused.es, button.picturesFocused.en, button.picturesFocused.de, button.picturesFocused.fr);
                    buttons.add(new Button(0, button.position, pictures, focusPictures, button.functionType, button.functionTarget, variableName[i].getName()));
                }
            }
            submenus.add(new Submenus(submenu.id, buttons));
        }
        Log.d(TAG, "SAVE " + submenus.size() + " SUBMENUS");
        appDataBase.submenuDao().insertAll(submenus);
    }

}

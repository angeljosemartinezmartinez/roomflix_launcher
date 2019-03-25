package verion.desing.launcher.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.models.Child;
import verion.desing.launcher.database.models.Translation;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.InfoCards;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.database.tables.Submenus;
import verion.desing.launcher.database.tables.Templates;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.response.ResponseAllInfo;
import verion.desing.launcher.network.response.ResponseInfoCards;
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
            savePictures(data.templates, appDatabase);
            saveButtons(data.templates, appDatabase);
            saveSubmenu(data.submenus, appDatabase);
            saveInfoCards(data.infoCards, appDatabase);

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
            langs.add(new Languages(languages.indexOf(data), data.nativeName, data.code, data.picture, data.isDefault));
        }
        appDatabase.languageDao().insertAll(langs);
        Log.d(TAG, "SAVE " + langs.size() + " LANGS");
    }

    private void saveTemplate(ResponseTemplates responseTemplates, AppDataBase appDataBase) {
        Templates templates;
        ArrayList<Button> buttons = new ArrayList<>();
        ArrayList<Translations> pictures = new ArrayList<>();
        for (ResponseTemplates.Button button : responseTemplates.buttons) {
            for (ResponseTemplates.Button.Translations picture : button.pictures) {
                pictures.add(new Translations(0, picture.locale, picture.picture, picture.pictureFocused, picture.functionType, picture.functionTarget));
            }
            buttons.add(new Button(button.position, button.position, pictures));

        }
        templates = new Templates(0, responseTemplates.logo, responseTemplates.background, buttons);
        appDataBase.templateDao().insertAll(templates);
        Log.d(TAG, "SAVE::::::::::::" + "TEMPLATE");
    }

    private void savePictures(ResponseTemplates responseTemplates, AppDataBase appDataBase) {
        ArrayList<Translations> pictures = new ArrayList<>();
        for (ResponseTemplates.Button button : responseTemplates.buttons) {
            for (ResponseTemplates.Button.Translations picture : button.pictures) {
                pictures.add(new Translations(0, picture.locale, picture.picture, picture.pictureFocused, picture.functionType, picture.functionTarget));
            }
        }
        appDataBase.picturesDao().insertAll(pictures);
        Log.d(TAG, "SAVE " + pictures.size() + " PICTURES");
    }


    private void saveButtons(ResponseTemplates responseTemplates, AppDataBase appDataBase) {
        ArrayList<Button> buttons = new ArrayList<>();
        for (ResponseTemplates.Button button : responseTemplates.buttons) {
            ArrayList<Translations> pictures = new ArrayList<>();
            for (ResponseTemplates.Button.Translations picture : button.pictures) {
                pictures.add(new Translations(0, picture.locale, picture.picture, picture.pictureFocused, picture.functionType, picture.functionTarget));
            }
            buttons.add(new Button(button.position, button.position, pictures));
        }
        appDataBase.buttonDao().insertAll(buttons);
        Log.d(TAG, "SAVE " + buttons.size() + " BUTTONS");
    }


    private void saveSubmenu(ArrayList<ResponseSubmenu> responseSubmenus, AppDataBase appDataBase) {
        ArrayList<Submenus> submenus = new ArrayList<>();
        for (ResponseSubmenu submenu : responseSubmenus) {
            ArrayList<Button> buttons = new ArrayList<>();
            for (ResponseSubmenu.Button button : submenu.buttons) {
                ArrayList<Translations> pictures = new ArrayList<>();
                for (ResponseSubmenu.Button.Translations picture : button.pictures) {
                    pictures.add(new Translations(button.position, picture.locale, picture.picture, picture.pictureFocused, picture.functionType, picture.functionTarget));
                }
                buttons.add(new Button(0, button.position, pictures));
            }
            submenus.add(new Submenus(submenu.id, buttons));
        }
        Log.d(TAG, "SAVE " + submenus.size() + " SUBMENUS");
        appDataBase.submenuDao().insertAll(submenus);
    }

    private void saveInfoCards(ArrayList<ResponseInfoCards> responseInfoCards, AppDataBase appDataBase) {
        ArrayList<InfoCards> infoCards = new ArrayList<>();
        for (ResponseInfoCards infocard : responseInfoCards) {
            ArrayList<InfoCards> childs = new ArrayList<>();
            for (ResponseInfoCards.Child child : infocard.childs) {
                ArrayList<Translation> translationsChild = new ArrayList<>();
                for(ResponseInfoCards.Child.Translations trans : child.translations){
                    translationsChild.add(new Translation(trans.locale, trans.picture));
                }
                childs.add(new InfoCards(child.id, translationsChild, null));
            }
            ArrayList<Translation> translations = new ArrayList<>();
            for (ResponseInfoCards.Translations translation : infocard.translations) {
                translations.add(new Translation(translation.locale, translation.picture));
            }
            infoCards.add(new InfoCards(infocard.id, translations, childs));
        }
        Log.d(TAG, "SAVE " + infoCards.size() + " INFOCARDS");
        appDataBase.infoCardDao().insertAll(infoCards);
    }
}

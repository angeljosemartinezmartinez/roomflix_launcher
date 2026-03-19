package com.roomflix.tv.managers;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;

import com.roomflix.tv.database.AppDataBase;
import com.roomflix.tv.database.models.Translation;
import com.roomflix.tv.database.models.TranslationSubmenu;
import com.roomflix.tv.database.tables.Button;
import com.roomflix.tv.database.tables.Configuration;
import com.roomflix.tv.database.tables.InfoCards;
import com.roomflix.tv.database.tables.Languages;
import com.roomflix.tv.database.tables.Submenus;
import com.roomflix.tv.database.tables.Templates;
import com.roomflix.tv.database.tables.Translations;
import com.roomflix.tv.database.tables.Update;
import com.roomflix.tv.listener.CallBackSaveData;
import com.roomflix.tv.network.response.ResponseAllInfo;
import com.roomflix.tv.network.response.ResponseConfiguration;
import com.roomflix.tv.network.response.ResponseInfoCards;
import com.roomflix.tv.network.response.ResponseLanguages;
import com.roomflix.tv.network.response.ResponseSubmenu;
import com.roomflix.tv.network.response.ResponseTemplates;
import com.roomflix.tv.network.response.ResponseUpdate;

public class SaveDataTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "SaveDataTask";
    private final Context context;
    private CallBackSaveData listener;
    private ResponseAllInfo data;
    private ResponseUpdate dataUpdate;
    private AppDataBase appDatabase;

    SaveDataTask(Context context, CallBackSaveData listener, ResponseAllInfo data, ResponseUpdate update) {
        this.listener = listener;
        this.data = data;
        this.context = context;
        this.dataUpdate = update;
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
            saveUpdate(dataUpdate, appDatabase);
            saveConfiguration(data.configuration, appDatabase);
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

    private void saveUpdate(ResponseUpdate update, AppDataBase appDataBase) {
        appDataBase.updateDao().insert(new Update(update.baseUrl, update.date, update.pkg, update.apk));
    }

    private void saveConfiguration(ResponseConfiguration responseConfiguration, AppDataBase appDataBase) {
            Configuration configuration;
            configuration = new Configuration(responseConfiguration.timeZone);
            appDataBase.configurationDao().insert(configuration);
    }

    private void saveLang(ArrayList<ResponseLanguages> languages, AppDataBase appDatabase) {
        ArrayList<Languages> langs = new ArrayList<>();
        for (ResponseLanguages data : languages) {
            langs.add(new Languages(languages.indexOf(data), data.nativeName, data.code, data.picture, data.isDefault, data.channel));
        }
        appDatabase.languageDao().insertAll(langs);
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
        templates = new Templates(0, responseTemplates.logo, responseTemplates.miniLogo, responseTemplates.background, buttons);
        appDataBase.templateDao().insertAll(templates);
    }

    private void savePictures(ResponseTemplates responseTemplates, AppDataBase appDataBase) {
        ArrayList<Translations> pictures = new ArrayList<>();
        for (ResponseTemplates.Button button : responseTemplates.buttons) {
            for (ResponseTemplates.Button.Translations picture : button.pictures) {
                pictures.add(new Translations(0, picture.locale, picture.picture, picture.pictureFocused, picture.functionType, picture.functionTarget));
            }
        }
        appDataBase.picturesDao().insertAll(pictures);
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
            ArrayList<TranslationSubmenu> translationSubmenus = new ArrayList<>();
            for (ResponseSubmenu.TranslationSubmenu translationSubmenu : submenu.translations) {
                translationSubmenus.add(new TranslationSubmenu(translationSubmenu.language, translationSubmenu.title));
            }
            submenus.add(new Submenus(submenu.id, translationSubmenus, buttons));
        }
        appDataBase.submenuDao().insertAll(submenus);
    }

    private void saveInfoCards(ArrayList<ResponseInfoCards> responseInfoCards, AppDataBase appDataBase) {
        ArrayList<InfoCards> infoCards = new ArrayList<>();
        for (ResponseInfoCards infocard : responseInfoCards) {
            ArrayList<InfoCards> childs = new ArrayList<>();
            for (ResponseInfoCards.Child child : infocard.childs) {
                ArrayList<Translation> translationsChild = new ArrayList<>();
                for (ResponseInfoCards.Child.Translations trans : child.translations) {
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
        appDataBase.infoCardDao().insertAll(infoCards);
    }
}

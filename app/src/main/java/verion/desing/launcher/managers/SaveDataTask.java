package verion.desing.launcher.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import verion.desing.launcher.database.AppDataBase;
import verion.desing.launcher.database.models.Translation;
import verion.desing.launcher.database.models.TranslationSubmenu;
import verion.desing.launcher.database.tables.Button;
import verion.desing.launcher.database.tables.Configuration;
import verion.desing.launcher.database.tables.InfoCards;
import verion.desing.launcher.database.tables.Languages;
import verion.desing.launcher.database.tables.Submenus;
import verion.desing.launcher.database.tables.Templates;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.database.tables.Update;
import verion.desing.launcher.listener.CallBackSaveData;
import verion.desing.launcher.network.response.ResponseAllInfo;
import verion.desing.launcher.network.response.ResponseConfiguration;
import verion.desing.launcher.network.response.ResponseInfoCards;
import verion.desing.launcher.network.response.ResponseLanguages;
import verion.desing.launcher.network.response.ResponseSubmenu;
import verion.desing.launcher.network.response.ResponseTemplates;
import verion.desing.launcher.network.response.ResponseUpdate;

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
        Log.d(TAG, "SAVE: " + update.toString());
    }

    private void saveConfiguration(ResponseConfiguration responseConfiguration, AppDataBase appDataBase) {
            Configuration configuration;
            configuration = new Configuration(responseConfiguration.timeZone);
            appDataBase.configurationDao().insert(configuration);
            Log.d(TAG, "SAVE::::::" + configuration.toString() + " CONFIGURATION");

    }

    private void saveLang(ArrayList<ResponseLanguages> languages, AppDataBase appDatabase) {
        ArrayList<Languages> langs = new ArrayList<>();
        for (ResponseLanguages data : languages) {
            langs.add(new Languages(languages.indexOf(data), data.nativeName, data.code, data.picture, data.isDefault, data.channel));
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
        templates = new Templates(0, responseTemplates.logo, responseTemplates.miniLogo, responseTemplates.background, buttons);
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
            ArrayList<TranslationSubmenu> translationSubmenus = new ArrayList<>();
            for (ResponseSubmenu.TranslationSubmenu translationSubmenu : submenu.translations) {
                translationSubmenus.add(new TranslationSubmenu(translationSubmenu.language, translationSubmenu.title));
            }
            submenus.add(new Submenus(submenu.id, translationSubmenus, buttons));
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
        Log.d(TAG, "SAVE " + infoCards.size() + " INFOCARDS");
        appDataBase.infoCardDao().insertAll(infoCards);
    }
}

package verion.desing.launcher.dragger;

import javax.inject.Singleton;

import dagger.Component;
import verion.desing.launcher.helpers.FileHelper;
import verion.desing.launcher.helpers.ImageHelper;
import verion.desing.launcher.helpers.PermissionHelper;
import verion.desing.launcher.managers.DBManager;
import verion.desing.launcher.network.service.CallManager;
import verion.desing.launcher.views.activities.InfoCardActivity;
import verion.desing.launcher.views.activities.LanguageSelect;
import verion.desing.launcher.views.activities.MainMenu;
import verion.desing.launcher.views.activities.BaseActivity;
import verion.desing.launcher.views.activities.MoreAppsActivity;
import verion.desing.launcher.views.activities.MoreAppsSubmenuActivity;

@Singleton
@Component(modules = {SharedPreferencesModule.class, ImageHelper.class, FileHelper.class, CallManager.class, DBManager.class, PermissionHelper.class})

@MyApplicationScope
public interface AppComponent {

    void inject(BaseActivity target);

    void inject(LanguageSelect target);

    void inject(MainMenu target);

    void inject(MoreAppsActivity target);

    void inject(InfoCardActivity target);

    void inject(MoreAppsSubmenuActivity target);

}

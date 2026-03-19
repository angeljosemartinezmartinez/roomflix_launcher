package com.roomflix.tv.dragger;

import javax.inject.Singleton;

import dagger.Component;
import com.roomflix.tv.dragger.MyApplicationScope;
import com.roomflix.tv.helpers.FileHelper;
import com.roomflix.tv.helpers.ImageHelper;
import com.roomflix.tv.helpers.PermissionHelper;
import com.roomflix.tv.managers.DBManager;
import com.roomflix.tv.network.service.CallManager;
import com.roomflix.tv.views.activities.InfoCardActivity;
import com.roomflix.tv.views.activities.MainMenu;
import com.roomflix.tv.views.activities.BaseActivity;
import com.roomflix.tv.views.activities.BaseTVActivity;
import com.roomflix.tv.views.activities.MoreAppsActivity;
import com.roomflix.tv.views.activities.MoreAppsSubmenuActivity;

@Singleton
@Component(modules = {SharedPreferencesModule.class, ImageHelper.class, FileHelper.class, CallManager.class, DBManager.class, PermissionHelper.class})

@MyApplicationScope
public interface AppComponent {

    void inject(BaseActivity target);

    void inject(BaseTVActivity target);

    void inject(MainMenu target);

    void inject(MoreAppsActivity target);

    void inject(InfoCardActivity target);

    void inject(MoreAppsSubmenuActivity target);

}

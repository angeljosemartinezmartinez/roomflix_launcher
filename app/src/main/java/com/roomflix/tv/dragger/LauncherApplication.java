package com.roomflix.tv.dragger;

import android.app.Application;

import com.mikepenz.iconics.Iconics;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import com.roomflix.tv.BuildConfig;
import com.roomflix.tv.Constants;
import com.roomflix.tv.helpers.FileHelper;
import com.roomflix.tv.helpers.ImageHelper;
import com.roomflix.tv.managers.DBManager;
import com.roomflix.tv.network.service.CallManager;
import timber.log.Timber;

public class LauncherApplication extends Application {
    private AppComponent myComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Inicializar Timber para logging de seguridad
        if (BuildConfig.ENVIRONMENT.equals(Constants.ENVIRONMENT.DEVELOP)) {
            Timber.plant(new timber.log.Timber.DebugTree());
        }
        
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .tag("HP-LOG")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();
        if (BuildConfig.ENVIRONMENT.equals(Constants.ENVIRONMENT.DEVELOP))
            Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
        // Initialize ApiPro with global Context for DeviceIdInterceptor
        com.roomflix.tv.network.service.ApiPro.INSTANCE.init(getApplicationContext());

        myComponent = DaggerAppComponent.builder()
                .sharedPreferencesModule(new SharedPreferencesModule(getApplicationContext()))
                .callManager(new CallManager())
                .dBManager(new DBManager())
                .fileHelper(new FileHelper())
                .imageHelper(new ImageHelper())
                .build();
        //only required if you add a custom or generic font on your own
        Iconics.init(getApplicationContext());
    }


    public AppComponent getAppComponent() {
        return myComponent;
    }


}

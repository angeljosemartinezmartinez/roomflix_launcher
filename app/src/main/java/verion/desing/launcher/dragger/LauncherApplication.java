package verion.desing.launcher.dragger;

import android.app.Application;

import com.mikepenz.iconics.Iconics;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import verion.desing.launcher.helpers.FileHelper;
import verion.desing.launcher.helpers.ImageHelper;
import verion.desing.launcher.managers.DBManager;
import verion.desing.launcher.network.service.CallManager;

public class LauncherApplication extends Application {
    private AppComponent myComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .tag("HP-LOG")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();
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

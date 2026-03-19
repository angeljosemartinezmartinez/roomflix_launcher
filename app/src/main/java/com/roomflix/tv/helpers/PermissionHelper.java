package com.roomflix.tv.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import androidx.core.app.ActivityCompat;
import dagger.Module;
import dagger.Provides;

@Module
public class PermissionHelper {

    public PermissionHelper() {
    }

    @Provides
    @Singleton
    PermissionHelper providePermissionHelper() {
        return new PermissionHelper();
    }

    public boolean checkPermissions(Context mContext) {
        if (shouldCheckRuntimePermissions(mContext)) {
            List<String> listPermissionsNeeded = new ArrayList<>();
            listPermissionsNeeded.add(Manifest.permission.SET_TIME);
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions((Activity) mContext, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 200);
                return false;
            }
            return true;
        } else

            return true;
    }


    /**
     * Checks whether runtime permissions must be handled or not.
     *
     * @param context Application context.
     * @return Handle runtime permissions or not.
     */
    public boolean shouldCheckRuntimePermissions(Context context) {
        return
                isApplicationWithMarshmallowTargetSdkVersion(context);
    }

    /**
     * Checks whether the app is compiled with targetSdkVersion Marshmallow or above.
     *
     * @param context Application context.
     * @return Application targetSdkVersion above 23 or not.
     */
    public boolean isApplicationWithMarshmallowTargetSdkVersion(Context context) {
        return
                context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.M;
    }

    public boolean hasWriteSettingsPermission(Context c) {
        if (shouldCheckRuntimePermissions(c))
            return Settings.System.canWrite(c);
        else return true;
    }
}

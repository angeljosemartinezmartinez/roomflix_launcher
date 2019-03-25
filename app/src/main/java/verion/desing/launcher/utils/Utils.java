package verion.desing.launcher.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import verion.desing.launcher.dragger.MySharedPreferences;

public class Utils {

    public static int getPixels(int dp, Context c) {
        Resources r = c.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                r.getDisplayMetrics());
    }

    public static ArrayList<String> getSystemLocales(Context c) {

        //getting the languages that are shown same as in our device settings

        String[] systemLocaleIetfLanguageTags = c.getAssets().getLocales();
        Arrays.sort(systemLocaleIetfLanguageTags);
        ArrayList<String> locale_data = new ArrayList<>();
        for (String ietfLanguageTag : systemLocaleIetfLanguageTags) {
            if (ietfLanguageTag != null && ietfLanguageTag.length() == 5) {
                locale_data.add(ietfLanguageTag);
            }
        }
        return locale_data;
    }

    private void setLocale(Locale locale, Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        configuration.setLocale(locale);
        context.getApplicationContext().createConfigurationContext(configuration);
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    //to change the locale
    public static void changeAppLanguage(Locale locale, Activity a) {
        try {
            Locale.setDefault(locale);
            Configuration config = a.getBaseContext().getResources().getConfiguration();
            config.locale = locale;
            a.getBaseContext().getResources().updateConfiguration(config,
                    a.getBaseContext().getResources().getDisplayMetrics());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getScreenWidth(Activity c) {
        DisplayMetrics metrics = new DisplayMetrics();
        c.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //cube width unit based on screen width
        return metrics.widthPixels;

    }
}

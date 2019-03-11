package verion.desing.launcher.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetWorkUtils {

    private Context context;
    private static final String TAG = "NetworkUtils";

    public NetWorkUtils(Context c) {
        this.context = c;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkOnlineState(Context context) {
        ConnectivityManager CManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo NInfo = CManager.getActiveNetworkInfo();
        if (NInfo != null && NInfo.isConnectedOrConnecting()) {
            try {
                return InetAddress.getByName("http://www.google.com").isReachable(1000);
            } catch (UnknownHostException ex){
                return false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}


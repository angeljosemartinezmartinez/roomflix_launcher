package verion.desing.launcher.views.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.databinding.ActivityCatyTutorialBinding;
import verion.desing.launcher.model.ModelTutorial;
import verion.desing.launcher.utils.NetWorkUtils;
import verion.desing.launcher.views.fragment.FragmentOnBoard;

public class CastTutorial extends NetworkBaseActivity implements View.OnClickListener {
    private static final String TAG = "CASTTUTORIAL";
    public ActivityCatyTutorialBinding binding;
    private String ssid;
    private String pass;
    public ScreenSlidePagerAdapter myAdapter;
    private ArrayList<ModelTutorial> mList = new ArrayList<>();

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_caty_tutorial);
        binding.btnAndroidSelection.setOnClickListener(this);
        binding.btnIosSelection.setOnClickListener(this);
        binding.btnExitCast.setOnClickListener(this);
        ssid = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.SSID);
        pass = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.PASS);
        boolean isEthernetOn = NetWorkUtils.isOnline();
        boolean isWifiOn = NetWorkUtils.isWifiOn(getApplicationContext());
        Log.d(TAG, "Wifi connected: " + isWifiOn);
        Log.d(TAG, "ETH connected: " + isEthernetOn);
        if (isEthernetOn)
            wifiOn(ssid, pass);
        if (isWifiOn) {
            comingSoon(this);
            finish();
        }

        getFocus();
        startPackage("sec.jbf.loa");


    }

    public boolean wifiOn(String ssid, String pass) {

        WifiConfiguration wc = new WifiConfiguration();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method methodx = wifiManager.getClass().getMethod("getWifiApConfiguration");
            wc.SSID = ssid;
            wc.preSharedKey = pass;
            wc.hiddenSSID = false;
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wc.allowedKeyManagement.set(4);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiManager.disconnect();
            Method method = null;
            method = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            method.invoke(wifiManager, wc);

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return true;


    }

    private boolean isHotSpotActive(Context cxt) {
        boolean isSuccess = false;
        WifiManager wifi = (WifiManager) cxt.getSystemService(Context.WIFI_SERVICE);
        Method[] wmMethods = wifi.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    isSuccess = (boolean) method.invoke(wifi);
                    stopTethering();
                    Log.d(TAG, "Is Hotspot active: " + isSuccess);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return isSuccess;
    }

    private void stopHotspot() {
        try {
            WifiConfiguration wc = new WifiConfiguration();
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wc.SSID = ssid;
            wc.preSharedKey = pass;
            wc.hiddenSSID = false;
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wc.allowedKeyManagement.set(4);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiManager.disconnect();
            Method method = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            method.invoke(wifiManager, wc);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void stopTethering() {
        ConnectivityManager manager = getApplicationContext().getSystemService(ConnectivityManager.class);
        try {
            Method method = manager.getClass().getDeclaredMethod("stopTethering", int.class);

            if (method == null) {
                Log.d(TAG, "stopTetheringMethod is null");
            } else {
                method.invoke(manager, 0);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void getFocus() {
        binding.btnAndroidSelection.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                binding.btnAndroidSelection.setBackgroundResource(R.drawable.backstreaming_white);
            else
                binding.btnAndroidSelection.setBackgroundResource(0);
        });
        binding.btnIosSelection.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                binding.btnIosSelection.setBackgroundResource(R.drawable.backstreaming_white);
            else
                binding.btnIosSelection.setBackgroundResource(0);
        });
        binding.btnExitCast.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                binding.btnExitCast.setTextColor(getResources().getColor(R.color.black, getTheme()));
            else
                binding.btnExitCast.setTextColor(getResources().getColor(R.color.white, getTheme()));
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTethering();
        isHotSpotActive(getApplicationContext());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startPackage("sec.jbf.loa");
    }


    @Override
    public void onBackPressed() {
        binding.onboardIndicator.setVisibility(View.INVISIBLE);
        if (binding.castViewPager.getVisibility() == View.VISIBLE) {
            binding.btnAndroidSelection.setVisibility(View.VISIBLE);
            binding.btnIosSelection.setVisibility(View.VISIBLE);
            binding.castViewPager.setVisibility(View.INVISIBLE);
        } else {
            binding.onboardIndicator.setVisibility(View.INVISIBLE);
            stopTethering();
            isHotSpotActive(getApplicationContext());
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        mList.clear();
        switch (view.getId()) {
            case R.id.btn_android_selection:
                binding.onboardIndicator.setVisibility(View.VISIBLE);
                binding.btnIosSelection.setVisibility(View.GONE);
                binding.btnAndroidSelection.setVisibility(View.GONE);
                binding.castViewPager.setVisibility(View.VISIBLE);
                binding.castViewPager.requestFocus();
                mList.add(new ModelTutorial(R.drawable.android_wifi, "",
                        ssid + "\n\n\n" + pass, true,
                        ""));
                mList.add(new ModelTutorial(R.drawable.android_compartir_desde_app, "",
                        "", true,
                        ""));
                mList.add(new ModelTutorial(R.drawable.android_compartir_pantalla, "",
                        "",
                        true, ""));
                mList.add(new ModelTutorial(R.drawable.android_enhorabuena, "",
                        "",
                        true, 0));
                myAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), mList);
                binding.castViewPager.setAdapter(myAdapter);
                binding.onboardIndicator.setViewPager(binding.castViewPager);
                break;
            case R.id.btn_ios_selection:
                binding.onboardIndicator.setVisibility(View.VISIBLE);
                binding.btnIosSelection.setVisibility(View.GONE);
                binding.btnAndroidSelection.setVisibility(View.GONE);
                binding.castViewPager.setVisibility(View.VISIBLE);
                binding.castViewPager.requestFocus();
                binding.castViewPager.setCurrentItem(0);
                mList.add(new ModelTutorial(R.drawable.android_wifi, "",
                        ssid + "\n\n\n" + pass, true,
                        ""));
                mList.add(new ModelTutorial(R.drawable.ios_compartir_pantalla, "",
                        "",
                        true, ""));
                mList.add(new ModelTutorial(R.drawable.ios_listado, "",
                        "", true,
                        0));
                mList.add(new ModelTutorial(R.drawable.ios_enhorabuena, "",
                        "",
                        true, ""));
                myAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), mList);
                binding.castViewPager.setAdapter(myAdapter);
                binding.onboardIndicator.setViewPager(binding.castViewPager);
                break;
            case R.id.btn_exit_cast:
                finish();
                break;
            default:
                break;
        }
        myAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), mList);
        binding.castViewPager.setAdapter(myAdapter);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<ModelTutorial> mItems;

        public ScreenSlidePagerAdapter(FragmentManager fm, ArrayList<ModelTutorial> items) {
            super(fm);
            mItems = items;
        }

        @Override
        public Fragment getItem(int position) {
            FragmentOnBoard fragment = FragmentOnBoard.newInstance(mItems.get(position));
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return String.valueOf(position);
        }

        @Override
        public int getCount() {
            if (mItems != null) {
                return mItems.size();
            }
            return 0;
        }
    }
}


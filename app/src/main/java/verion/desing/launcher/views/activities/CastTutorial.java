package verion.desing.launcher.views.activities;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import java.util.ArrayList;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.databinding.ActivityCatyTutorialBinding;
import verion.desing.launcher.model.ModelTutorial;
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
        ssid = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.SSID);
        pass = mySharedPreferences.getString(Constants.SHARED_PREFERENCES.PASS);
        getFocus();
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
    }

    @Override
    public void onBackPressed() {
        binding.onboardIndicator.setVisibility(View.INVISIBLE);
        if (binding.castViewPager.getVisibility() == View.VISIBLE) {
            binding.btnAndroidSelection.setVisibility(View.VISIBLE);
            binding.btnIosSelection.setVisibility(View.VISIBLE);
            binding.btnIosSelection.requestFocus();
            binding.castViewPager.setVisibility(View.INVISIBLE);
        } else {
            binding.onboardIndicator.setVisibility(View.INVISIBLE);
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
                        ssid + "\n\n" + pass, true,
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
                        ssid + "\n\n" + pass, true,
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


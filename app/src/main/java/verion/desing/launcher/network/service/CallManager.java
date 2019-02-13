package verion.desing.launcher.network.service;

import android.util.Log;

import com.google.android.material.tabs.TabLayout;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import verion.desing.launcher.network.response.ResponseLanguages;
import verion.desing.launcher.network.service.callbacks.CallBackData;

@Module
public class CallManager {
    private static final String TAG = "CallManager";

    @Provides
    @Singleton
    CallManager provideCallManager() {
        return new CallManager();
    }

    public void getDataFromServer(String mac, final CallBackData listener) {
        ApiPro.createService(Service.class);
        Call<ResponseLanguages> call = ApiPro.createService(Service.class).getData(mac);
        call.enqueue(new Callback<ResponseLanguages>() {
            @Override
            public void onResponse(Call<ResponseLanguages> call, Response<ResponseLanguages> response) {
                try {
                    if (response.isSuccessful()) {
                        listener.finishAction(response.body());
                    } else listener.error(String.valueOf(response.code()));
                } catch (Exception e) {
                    listener.error("C");
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseLanguages> call, Throwable t) {
                Log.d(TAG, "Error: " + t.getCause().toString());
                listener.error("F");
            }
        });


    }
}

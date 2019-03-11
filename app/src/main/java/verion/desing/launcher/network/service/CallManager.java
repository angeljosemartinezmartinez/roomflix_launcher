package verion.desing.launcher.network.service;

import android.util.Log;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import verion.desing.launcher.network.callbacks.CallBackData;
import verion.desing.launcher.network.response.ResponseAllInfo;

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
        Call<ResponseAllInfo> call = ApiPro.createService(Service.class).getData(mac);
        call.enqueue(new Callback<ResponseAllInfo>() {
            @Override
            public void onResponse(Call<ResponseAllInfo> call, Response<ResponseAllInfo> response) {
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
            public void onFailure(Call<ResponseAllInfo> call, Throwable t) {
                Log.d(TAG, "Error: " + t.getLocalizedMessage());
                listener.error("F");
            }
        });

    }
}

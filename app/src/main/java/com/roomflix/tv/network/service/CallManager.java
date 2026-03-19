package com.roomflix.tv.network.service;

import android.util.Log;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.roomflix.tv.network.callbacks.CallBackData;
import com.roomflix.tv.network.response.ResponseAllInfo;
import com.roomflix.tv.network.response.ResponseUpdate;

@Module
public class CallManager {
    private static final String TAG = "CallManager";

    @Provides
    @Singleton
    CallManager provideCallManager() {
        return new CallManager();
    }

    public void getDataFromServer(String mac, final CallBackData listener) {
        // mac parameter is kept for compatibility but DeviceIdInterceptor will replace it
        // with the real Device ID automatically
        Log.d(TAG, "[CallManager] getDataFromServer llamado con mac parameter: " + mac);
        Log.d(TAG, "[CallManager] NOTA: El DeviceIdInterceptor reemplazará este valor automáticamente");
        Call<ResponseAllInfo> call = ApiPro.createService(Service.class).getData(mac);
        call.enqueue(new Callback<ResponseAllInfo>() {
            @Override
            public void onResponse(Call<ResponseAllInfo> call, Response<ResponseAllInfo> response) {
                try {
                    if (response.isSuccessful()) {
                        listener.finishAction(response.body());
                    } else {
                        // Error HTTP (404, 500, etc.) - NO guardar nada en caché
                        // Devolver estado de error limpio para que la UI no se rompa
                        listener.error(String.valueOf(response.code()));
                    }
                } catch (Exception e) {
                    // Error de procesamiento - NO guardar nada en caché
                    listener.error("C");
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseAllInfo> call, Throwable t) {
                // Error de red (sin conexión, timeout, etc.) - NO guardar nada en caché
                // Devolver estado de error limpio para que la UI no se rompa
                Log.d(TAG, "Network Error: " + (t.getLocalizedMessage() != null ? t.getLocalizedMessage() : t.toString()));
                listener.error("F");
            }
        });

    }


    public void getUpdate(String mac, CallBackData listener){
        // mac parameter is kept for compatibility but DeviceIdInterceptor will replace it
        // with the real Device ID automatically
        Call<ResponseUpdate> call = ApiPro.createService(Service.class).getUpdate(mac);
        call.enqueue(new Callback<ResponseUpdate>() {
            @Override
            public void onResponse(Call<ResponseUpdate> call, Response<ResponseUpdate> response) {
                try {
                    if (response.isSuccessful()) {
                        listener.finishAction(response.body());
                    } else {
                        // Error HTTP (404, 500, etc.) - NO guardar nada en caché
                        listener.error(String.valueOf(response.code()));
                    }
                } catch (Exception e) {
                    // Error de procesamiento - NO guardar nada en caché
                    listener.error("C");
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseUpdate> call, Throwable t) {
                // Error de red (sin conexión, timeout, etc.) - NO guardar nada en caché
                Log.d(TAG, "Network Error: " + (t.getLocalizedMessage() != null ? t.getLocalizedMessage() : t.toString()));
                listener.error("F");
            }
        });
    }

}

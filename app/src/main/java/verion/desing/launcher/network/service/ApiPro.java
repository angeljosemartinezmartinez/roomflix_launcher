package verion.desing.launcher.network.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import verion.desing.launcher.BuildConfig;
import verion.desing.launcher.Constants;

public class ApiPro {

    private static final ApiPro ourInstance = new ApiPro();

    private ApiPro() {
    }

    static ApiPro getInstance() {
        return ourInstance;
    }

    /***
     * This method return a instance of Retrofit object with basich auth
     *
     * @param serviceClass the diferent servers with his own methods and paths

     */

    public static <S> S createService(Class<S> serviceClass) {
        int cacheSize = 1024; // 10 MB
     /*   Cache cache = new Cache(Environment.getDownloadCacheDirectory(), cacheSize);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder().cache(cache);*/
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        Retrofit.Builder builder;
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create();
        builder =
                new Retrofit.Builder()
                        .baseUrl("https://panel.hotelplay.tv")
                        .addConverterFactory(GsonConverterFactory.create(gson));


        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.ENVIRONMENT.equalsIgnoreCase(Constants.ENVIRONMENT.DEVELOP) ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .method(original.method(), original.body());
           /* if (response.cacheControl() != null) {
                // from cache
            } else if (response.networkResponse() != null) {
                // from network

            }*/
            Request request = requestBuilder.build();
            return chain.proceed(request);
        })
                .addInterceptor(loggingInterceptor);
        OkHttpClient client = httpClient.build();
        Retrofit retrofit = builder.client(client).build();
        return retrofit.create(serviceClass);
    }



}

package verion.desing.launcher.network.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import verion.desing.launcher.network.response.ResponseAllInfo;
import verion.desing.launcher.network.response.ResponseLanguages;
import verion.desing.launcher.network.response.ResponseTemplates;

public interface Service {

    @Headers("Cache-Control: max-age=40")
    @GET("{mac}")
    Call<ResponseAllInfo> getData(@Path("mac") String id);
}

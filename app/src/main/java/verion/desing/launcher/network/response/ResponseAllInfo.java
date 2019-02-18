package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ResponseAllInfo {

    @SerializedName("baseUrl")
    public String baseUrl;
    @SerializedName("languages")
    public ArrayList<ResponseLanguages> languages;
    @SerializedName("template")
    public ResponseTemplates templates;
    @SerializedName("submenus")
    public ArrayList<ResponseSubmenu> submenus;
}

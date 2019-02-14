package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseLanguages {
    @SerializedName("baseUrl")
    public String baseUrl;
    @SerializedName("languages")
    public ArrayList<Languages> languages;


    public class Languages implements Serializable {
        @SerializedName("nativeName")
        public String nativeName;
        @SerializedName("code")
        public String code;
        @SerializedName("picture")
        public String picture;


        public Languages(String nativeName, String code) {
            this.nativeName = nativeName;
            this.code = code;
        }
    }

    @Override
    public String toString() {
        return "ResponseLanguages{" +
                ", baseUrl='" + baseUrl + '\'' +
                ", languages=" + languages +
                '}';
    }
}

package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseLanguages {
    @SerializedName("statusCode")
    public int statusCode;
    @SerializedName("baseUrl")
    public String baseUrl;
    @SerializedName("picture")
    public String picture;
    @SerializedName("data")
    public ArrayList<Data> data;


    public class Data implements Serializable{
        @SerializedName("name")
        public String name;
        @SerializedName("nativeName")
        public String nativeName;
        @SerializedName("code")
        public String code;
        @SerializedName("textsApp")
        public ArrayList<String> texts;

        @Override
        public String toString() {
            return "Data{" +
                    "name='" + name + '\'' +
                    ", nativeName='" + nativeName + '\'' +
                    ", code='" + code + '\'' +
                    ", texts=" + texts +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ResponseLanguages{" +
                "statusCode=" + statusCode +
                ", baseUrl='" + baseUrl + '\'' +
                ", picture='" + picture + '\'' +
                ", data=" + data +
                '}';
    }
}

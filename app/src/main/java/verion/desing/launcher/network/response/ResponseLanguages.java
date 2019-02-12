package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseLanguages {
    @SerializedName("statusCode")
    private int statusCode;
    @SerializedName("baseUrl")
    private String baseUrl;
    @SerializedName("picture")
    private String picture;
    @SerializedName("data")
    private ArrayList<Data> data;


    public class Data implements Serializable{
        @SerializedName("name")
        private String name;
        @SerializedName("nativeName")
        private String nativeName;
        @SerializedName("code")
        private String code;
        @SerializedName("textsApp")
        private TextsApp texts;

        @Override
        public String toString() {
            return "Data{" +
                    "name='" + name + '\'' +
                    ", nativeName='" + nativeName + '\'' +
                    ", code='" + code + '\'' +
                    ", texts=" + texts +
                    '}';
        }

        public class TextsApp implements Serializable{
            @SerializedName("Text 1")
            private String text1;
            @SerializedName("Text 2")
            private String text2;
            @SerializedName("Text 3")
            private String text3;

            @Override
            public String toString() {
                return "TextsApp{" +
                        "text1='" + text1 + '\'' +
                        ", text2='" + text2 + '\'' +
                        ", text3='" + text3 + '\'' +
                        '}';
            }
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

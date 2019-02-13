package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseLanguages {
    @SerializedName("statusCode")
    public int statusCode;
    @SerializedName("baseUrl")
    public String baseUrl;

    @SerializedName("data")
    public ArrayList<Data> data;


    public class Data implements Serializable{
        @SerializedName("name")
        public String name;
        @SerializedName("nativeName")
        public String nativeName;
        @SerializedName("code")
        public String code;
        @SerializedName("picture")
        public String picture;
        @SerializedName("textsApp")
        private TextsApp texts;

        public Data(String name, String nativeName, String code, TextsApp texts) {
            this.name = name;
            this.nativeName = nativeName;
            this.code = code;
            this.texts = texts;
        }

        public class TextsApp implements Serializable{
            @SerializedName("Text 1")
            private String text1;
            @SerializedName("Text 2")
            private String text2;
            @SerializedName("Text 3")
            private String text3;

            public TextsApp(String text1, String text2, String text3) {
                this.text1 = text1;
                this.text2 = text2;
                this.text3 = text3;
            }
        }
    }

    @Override
    public String toString() {
        return "ResponseLanguages{" +
                "statusCode=" + statusCode +
                ", baseUrl='" + baseUrl + '\'' +
                ", data=" + data +
                '}';
    }
}

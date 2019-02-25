package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseInfoCards {

    @SerializedName("id")
    public int id;
    @SerializedName("titles")
    public ArrayList<Titles> titles;
    @SerializedName("descriptions")
    public ArrayList<Descriptions> descriptions;
    @SerializedName("picture")
    public String picture;

    public class Titles implements Serializable {
        @SerializedName("language")
        public String language;
        @SerializedName("text")
        public String text;
    }

    public class Descriptions implements Serializable {
        @SerializedName("language")
        public String language;
        @SerializedName("text")
        public String text;
    }
}

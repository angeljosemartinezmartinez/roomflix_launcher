package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseTemplates {
    @SerializedName("logo")
    public String logo;
    @SerializedName("background")
    public String background;
    @SerializedName("buttons")
    public ArrayList<Button> buttons;

    public class Button implements Serializable {
        @SerializedName("position")
        public int position;
        @SerializedName("translations")
        public ArrayList<Translations> pictures;

        public class Translations implements Serializable {
            @SerializedName("language")
            public String locale;
            @SerializedName("picture")
            public String picture;
            @SerializedName("focusPicture")
            public String pictureFocused;
            @SerializedName("functionType")
            public int functionType;
            @SerializedName("functionTarget")
            public String functionTarget;

        }
    }

    @Override
    public String toString() {
        return "ResponseTemplates{" +
                "logo='" + logo + '\'' +
                ", background='" + background + '\'' +
                ", backgroundLanguages='" + '\'' +
                ", buttons=" + buttons +
                '}';
    }
}

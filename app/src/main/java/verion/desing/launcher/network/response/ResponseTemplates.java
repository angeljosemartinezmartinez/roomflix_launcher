package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseTemplates {
    @SerializedName("logo")
    public String logo;
    @SerializedName("background")
    public String background;
    @SerializedName("backgroundLanguages")
    public String backgroundLanguages;
    @SerializedName("buttons")
    public ArrayList<Button> buttons;

    public class Button implements Serializable {
        @SerializedName("position")
        public int position;
        @SerializedName("pictures")
        public ArrayList<Pictures> pictures;
        @SerializedName("functionType")
        public int functionType;
        @SerializedName("functionTarget")
        public String functionTarget;

        public class Pictures implements Serializable {
            @SerializedName("language")
            public String locale;
            @SerializedName("picture")
            public String picture;
            @SerializedName("focusPicture")
            public String pictureFocused;

        }
    }

    @Override
    public String toString() {
        return "ResponseTemplates{" +
                "logo='" + logo + '\'' +
                ", background='" + background + '\'' +
                ", backgroundLanguages='" + backgroundLanguages + '\'' +
                ", buttons=" + buttons +
                '}';
    }
}

package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseSubmenu {
    @SerializedName("id")
    public int id;
    @SerializedName("buttons")
    public ArrayList<Button> buttons;

    public class Button implements Serializable {
        @SerializedName("position")
        public int position;
        @SerializedName("pictures")
        public Pictures pictures;
        @SerializedName("focusPictures")
        public FocusPictures picturesFocused;
        @SerializedName("functionType")
        public int functionType;
        @SerializedName("functionTarget")
        public String functionTarget;

        public class Pictures implements Serializable{
            @SerializedName("es")
            public String es;
            @SerializedName("en")
            public String en;
            @SerializedName("de")
            public String de;
            @SerializedName("fr")
            public String fr;
        }

        public class FocusPictures implements Serializable{
            @SerializedName("es")
            public String es;
            @SerializedName("en")
            public String en;
            @SerializedName("de")
            public String de;
            @SerializedName("fr")
            public String fr;
        }
    }
}

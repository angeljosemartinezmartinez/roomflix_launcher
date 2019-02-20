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
        public ArrayList<Pictures> pictures;
        @SerializedName("focusPictures")
        public ArrayList<FocusPictures> picturesFocused;
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

        public class FocusPictures implements Serializable {
            @SerializedName("language")
            public String locale;
            @SerializedName("picture")
            public String picture;
        }
    }
}

package com.roomflix.tv.network.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class ResponseInfoCards {

    @SerializedName("id")
    public int id;
    @SerializedName("translations")
    public ArrayList<Translations> translations;
    @SerializedName("childs")
    public ArrayList<Child> childs;

    public class Translations implements Serializable {
        @SerializedName("language")
        public String locale;
        @SerializedName("picture")
        public String picture;

    }

    public class Child implements Serializable{
        @SerializedName("id")
        public int id;
        @SerializedName("translations")
        public ArrayList<Translations> translations;

        public class Translations implements Serializable {
            @SerializedName("language")
            public String locale;
            @SerializedName("picture")
            public String picture;

        }
    }
}

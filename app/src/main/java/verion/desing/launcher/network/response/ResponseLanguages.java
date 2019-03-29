package verion.desing.launcher.network.response;

import com.google.gson.annotations.SerializedName;

public class ResponseLanguages {
    @SerializedName("nativeName")
    public String nativeName;
    @SerializedName("code")
    public String code;
    @SerializedName("picture")
    public String picture;
    @SerializedName("isDefault")
    public boolean isDefault;
    @SerializedName("channel")
    public String channel;

    @Override
    public String toString() {
        return "ResponseLanguages{" +
                "nativeName='" + nativeName + '\'' +
                ", code='" + code + '\'' +
                ", picture='" + picture + '\'' +
                ", isDefault=" + isDefault +
                ", channel='" + channel + '\'' +
                '}';
    }
}

package com.roomflix.tv.network.response;

import com.google.gson.annotations.SerializedName;

public class ResponseUpdate {
    @SerializedName("baseUrl")
    public String baseUrl;
    @SerializedName("uploadDate")
    public String date;
    @SerializedName("package")
    public String pkg;
    @SerializedName("apk")
    public String apk;

    @Override
    public String toString() {
        return "ResponseUpdate{" +
                "baseUrl='" + baseUrl + '\'' +
                ", date='" + date + '\'' +
                ", pkg='" + pkg + '\'' +
                ", apk='" + apk + '\'' +
                '}';
    }
}

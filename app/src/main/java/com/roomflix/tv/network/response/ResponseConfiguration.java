package com.roomflix.tv.network.response;

import com.google.gson.annotations.SerializedName;

public class ResponseConfiguration {
    @SerializedName("timeZone")
    public int timeZone;
    @SerializedName("accessPoint")
    public boolean accessPoint;
    @SerializedName("accessPointName")
    public String ssid;
    @SerializedName("accessPointKey")
    public String pass;
    @SerializedName("adbServer")
    public String adbServer;
}

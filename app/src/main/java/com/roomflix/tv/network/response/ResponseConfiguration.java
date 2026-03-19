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

    // Legacy — se mantiene por compatibilidad hasta que Panel envie controlApiUrl
    @SerializedName("adbServer")
    public String adbServer;

    // Control API (reemplaza adbServer)
    @SerializedName("controlApiUrl")
    public String controlApiUrl;
    @SerializedName("controlApiToken")
    public String controlApiToken;
    @SerializedName("controlDeviceId")
    public String controlDeviceId;

    // VPN WireGuard
    @SerializedName("vpnAddress")
    public String vpnAddress;
    @SerializedName("vpnServerEndpoint")
    public String vpnServerEndpoint;
    @SerializedName("vpnServerPublicKey")
    public String vpnServerPublicKey;
    @SerializedName("vpnPrivateKey")
    public String vpnPrivateKey;
}

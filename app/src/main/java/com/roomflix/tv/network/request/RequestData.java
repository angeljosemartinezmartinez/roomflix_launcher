package com.roomflix.tv.network.request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestData {
    @SerializedName("lastUpdate")
    private String lastUpdate;
    @SerializedName("status")
    private Status status;

    public RequestData(String lastUpdate, Status status) {
        this.lastUpdate = lastUpdate;
        this.status = status;
    }

    public static class Status implements Serializable {
        @SerializedName("cores")
        private int cores;
        @SerializedName("usedCpu")
        private int usedCpu;
        @SerializedName("ram")
        private Double ram;
        @SerializedName("usedRam")
        private Double usedRam;
        @SerializedName("rom")
        private int rom;
        @SerializedName("usedRom")
        private Double usedRom;
        @SerializedName("apps")
        private ArrayList<Apps> apps;

        public Status(int cores, int usedCpu, Double ram, Double usedRam, int rom, Double usedRom, ArrayList<Apps> apps) {
            this.cores = cores;
            this.usedCpu = usedCpu;
            this.ram = ram;
            this.usedRam = usedRam;
            this.rom = rom;
            this.usedRom = usedRom;
            this.apps = apps;
        }

        public static class Apps implements Serializable{
            @SerializedName("name")
            private String name;
            @SerializedName("version")
            private String version;
            @SerializedName("package")
            private String pkg;

            public Apps(String name, String version, String pkg) {
                this.name = name;
                this.version = version;
                this.pkg = pkg;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getVersion() {
                return version;
            }

            public void setVersion(String version) {
                this.version = version;
            }
        }

    }
}

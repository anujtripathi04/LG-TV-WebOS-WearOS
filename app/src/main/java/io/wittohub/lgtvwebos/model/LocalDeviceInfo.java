package io.wittohub.lgtvwebos.model;

public class LocalDeviceInfo {
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    private String ip;
    private String hostname;

    public LocalDeviceInfo(String ip, String hostname) {
        this.ip = ip;
        this.hostname = hostname;
    }
}

package com.tmobile.cso.vault.api.model;

public class TargetSystemService {

    private String hostname;
    private String name;
    private int port;
    private int targetSystemGroupId;
    private int targetSystemId;
    private int targetSystemServiceId;

    public TargetSystemService(String hostname, String name, int port, int targetSystemGroupId, int targetSystemId, int targetSystemServiceId) {
        this.hostname = hostname;
        this.name = name;
        this.port = port;
        this.targetSystemGroupId = targetSystemGroupId;
        this.targetSystemId = targetSystemId;
        this.targetSystemServiceId = targetSystemServiceId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTargetSystemGroupId() {
        return targetSystemGroupId;
    }

    public void setTargetSystemGroupId(int targetSystemGroupId) {
        this.targetSystemGroupId = targetSystemGroupId;
    }

    public int getTargetSystemId() {
        return targetSystemId;
    }

    @Override
    public String toString() {
        return "TargetSystemService{" +
                "hostname='" + hostname + '\'' +
                ", name='" + name + '\'' +
                ", port=" + port +
                ", targetSystemGroupId=" + targetSystemGroupId +
                ", targetSystemId=" + targetSystemId +
                ", targetSystemServiceId=" + targetSystemServiceId +
                '}';
    }

    public void setTargetSystemId(int targetSystemId) {
        this.targetSystemId = targetSystemId;
    }

    public int getTargetSystemServiceId() {
        return targetSystemServiceId;
    }

    public void setTargetSystemServiceId(int targetSystemServiceId) {
        this.targetSystemServiceId = targetSystemServiceId;
    }
}

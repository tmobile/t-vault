package com.tmobile.cso.vault.api.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class TargetSystemServiceRequest {
    private int port;
    private String hostname;
    private String description;
    private String name;
    private boolean monitoringEnabled;
    private boolean multiIpMonitoringEnabled;

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }

    public boolean isMultiIpMonitoringEnabled() {
        return multiIpMonitoringEnabled;
    }

    public void setMultiIpMonitoringEnabled(boolean multiIpMonitoringEnabled) {
        this.multiIpMonitoringEnabled = multiIpMonitoringEnabled;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "TargetSystemServiceRequest{" +
                "port=" + port +
                ", hostname='" + hostname + '\'' +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                ", monitoringEnabled=" + monitoringEnabled +
                ", multiIpMonitoringEnabled=" + multiIpMonitoringEnabled +
                '}';
    }

/*@Override
    public String toString() {
        return "TargetSystemServiceRequest{" +
                "port=" + port +
                ", hostname='" + hostname + '\'' +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                '}';
    }*/
}

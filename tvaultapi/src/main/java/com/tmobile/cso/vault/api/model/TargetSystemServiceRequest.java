package com.tmobile.cso.vault.api.model;


import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class TargetSystemServiceRequest {
    @Min(value = 1, message = "can't be less than 1 or bigger than 999999")
    @Max(999999)
    private int port;
    private String hostname;
    private String description;
    @NotNull
    @NotEmpty
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

}

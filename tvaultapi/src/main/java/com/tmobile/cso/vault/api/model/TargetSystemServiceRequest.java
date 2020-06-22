package com.tmobile.cso.vault.api.model;


import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class TargetSystemServiceRequest {
    @Min(value = 1, message = "Please enter value between 0 and 65536")
    @Max(65535)
    private int port;
    @Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "HostName can have alphabets, numbers, . and - characters only")
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

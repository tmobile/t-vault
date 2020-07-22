package com.tmobile.cso.vault.api.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetSystemServiceRequest {
    @Min(value = 1, message = "Please enter value between 0 and 65536")
    @Max(65535)
    @ApiModelProperty(example="1", position=1)
    private int port;
    private String hostname;
    private String description;
    @NotNull
    @NotEmpty
    private String name;
    
    @ApiModelProperty(hidden = true)
    private boolean monitoringEnabled;
    
    @ApiModelProperty(hidden = true)
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

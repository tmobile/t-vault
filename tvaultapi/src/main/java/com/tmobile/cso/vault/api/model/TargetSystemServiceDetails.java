// =========================================================================
// Copyright 2020 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License")
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class TargetSystemServiceDetails implements Serializable {

    private static final long serialVersionUID = -7686021728491883938L;
    /**
     *
     */
    private String name;
    private String description;
    private String targetSystemServiceId;
    private String hostname;
    private Boolean monitoringEnabled;
    private Boolean multiIpMonitoringEnabled;
    private Integer port;

    public TargetSystemServiceDetails() {
    }

    public TargetSystemServiceDetails(String name, String description, String targetSystemServiceId, String hostname, Boolean monitoringEnabled, Boolean multiIpMonitoringEnabled, Integer port) {
        this.name = name;
        this.description = description;
        this.targetSystemServiceId = targetSystemServiceId;
        this.hostname = hostname;
        this.monitoringEnabled = monitoringEnabled;
        this.multiIpMonitoringEnabled = multiIpMonitoringEnabled;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetSystemServiceId() {
        return targetSystemServiceId;
    }

    public void setTargetSystemServiceId(String targetSystemServiceId) {
        this.targetSystemServiceId = targetSystemServiceId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Boolean getMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled(Boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }

    public Boolean getMultiIpMonitoringEnabled() {
        return multiIpMonitoringEnabled;
    }

    public void setMultiIpMonitoringEnabled(Boolean multiIpMonitoringEnabled) {
        this.multiIpMonitoringEnabled = multiIpMonitoringEnabled;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "TargetSystemServiceDetails{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", targetSystemServiceId='" + targetSystemServiceId + '\'' +
                ", hostname='" + hostname + '\'' +
                ", monitoringEnabled=" + monitoringEnabled +
                ", multiIpMonitoringEnabled=" + multiIpMonitoringEnabled +
                ", port=" + port +
                '}';
    }
}
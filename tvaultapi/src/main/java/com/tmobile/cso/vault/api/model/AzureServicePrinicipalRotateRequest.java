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

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

public class AzureServicePrinicipalRotateRequest implements Serializable {

    private static final long serialVersionUID = -2124663808021117763L;

    @NotBlank
    private String secretKeyId;
    @NotBlank
    @Size(min = 10, max = 128, message = "ServicePrinicipalId specified should be minimum 10 chanracters and maximum 128 characters only")
    private String servicePrinicipalId;
    @NotBlank
    @Size(min = 10, max = 128, message = "TenantId specified should be minimum 10 chanracters and maximum 128 characters only")
    private String tenantId;

    /**
     *
     */
    public AzureServicePrinicipalRotateRequest() {
        super();
    }


    /**
     * @param secretKeyId
     * @param servicePrinicipalId
     * @param tenantId
     */
    public AzureServicePrinicipalRotateRequest(String secretKeyId, String servicePrinicipalId, String tenantId) {
        super();
        this.secretKeyId = secretKeyId;
        this.servicePrinicipalId = servicePrinicipalId;
        this.tenantId = tenantId;
    }

    public String getSecretKeyId() {
        return secretKeyId;
    }

    public void setSecretKeyId(String secretKeyId) {
        this.secretKeyId = secretKeyId;
    }

    public String getServicePrinicipalId() {
        return servicePrinicipalId;
    }

    public void setServicePrinicipalId(String servicePrinicipalId) {
        this.servicePrinicipalId = servicePrinicipalId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "AzureServicePrinicipalRotateRequest{" +
                "secretKeyId='" + secretKeyId + '\'' +
                ", servicePrinicipalId='" + servicePrinicipalId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
}
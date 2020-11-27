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

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

public class AzureServicePrincipalRotateRequest implements Serializable {

    private static final long serialVersionUID = -2124663808021117763L;

    @NotBlank
    @Size(min = 11, message = "Azure service principal name specified should be minimum 11 chanracters only")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Azure service principal name can have alphabets, numbers, _ and - characters only")
    private String azureSvcAccName;
    @NotBlank
    private String secretKeyId;
    @NotBlank
    @Size(min = 10, max = 128, message = "ServicePrincipalId specified should be minimum 10 chanracters and maximum 128 characters only")
    private String servicePrincipalId;
    @NotBlank
    @Size(min = 10, max = 128, message = "TenantId specified should be minimum 10 chanracters and maximum 128 characters only")
    private String tenantId;

    /**
     *
     */
    public AzureServicePrincipalRotateRequest() {
        super();
    }


    /**
     * @param secretKeyId
     * @param servicePrincipalId
     * @param tenantId
     */
    public AzureServicePrincipalRotateRequest(String azureSvcAccName, String secretKeyId, String servicePrincipalId, String tenantId) {
        super();
        this.azureSvcAccName = azureSvcAccName;
        this.secretKeyId = secretKeyId;
        this.servicePrincipalId = servicePrincipalId;
        this.tenantId = tenantId;
    }

    public String getSecretKeyId() {
        return secretKeyId;
    }

    public void setSecretKeyId(String secretKeyId) {
        this.secretKeyId = secretKeyId;
    }

    public String getServicePrincipalId() {
        return servicePrincipalId;
    }

    public void setServicePrincipalId(String servicePrincipalId) {
        this.servicePrincipalId = servicePrincipalId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAzureSvcAccName() {
        return azureSvcAccName;
    }

    public void setAzureSvcAccName(String azureSvcAccName) {
        this.azureSvcAccName = azureSvcAccName;
    }

    @Override
    public String toString() {
        return "AzureServicePrincipalRotateRequest{" +
                "azureSvcAccName='" + azureSvcAccName + '\'' +
                ", secretKeyId='" + secretKeyId + '\'' +
                ", servicePrincipalId='" + servicePrincipalId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
}
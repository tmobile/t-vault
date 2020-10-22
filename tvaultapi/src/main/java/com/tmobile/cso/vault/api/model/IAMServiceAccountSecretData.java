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
import java.util.Arrays;

public class IAMServiceAccountSecretData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -8296077697839791334L;

    private String userName;
    private IAMServiceAccountSecret[] iamServiceAccountSecrets;
    /**
     *
     */
    public IAMServiceAccountSecretData() {
        super();
    }

    public IAMServiceAccountSecretData(String userName, IAMServiceAccountSecret[] iamServiceAccountSecrets) {
        this.userName = userName;
        this.iamServiceAccountSecrets = iamServiceAccountSecrets;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public IAMServiceAccountSecret[] getIamServiceAccountSecrets() {
        return iamServiceAccountSecrets;
    }

    public void setIamServiceAccountSecrets(IAMServiceAccountSecret[] iamServiceAccountSecrets) {
        this.iamServiceAccountSecrets = iamServiceAccountSecrets;
    }

    @Override
    public String toString() {
        return "IAMServiceAccountSecretData{" +
                "userName='" + userName + '\'' +
                ", iamServiceAccountSecrets=" + Arrays.toString(iamServiceAccountSecrets) +
                '}';
    }
}
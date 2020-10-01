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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

public class IAMServiceAccountRotateRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 7097997298453993781L;

    @NotNull
    private String accessKeyId;
    @NotNull
    private String userName;
    @NotNull
    @Pattern( regexp = "^$|^[0-9]+$", message="Invalid AWS account id")
    private String accountId;

    /**
     *
     */
    public IAMServiceAccountRotateRequest() {
        super();
    }

    /**
     * @param accessKeyId
     * @param userName
     * @param accountId
     */
    public IAMServiceAccountRotateRequest(String accessKeyId, String userName, String accountId) {
        super();
        this.accessKeyId = accessKeyId;
        this.userName = userName;
        this.accountId = accountId;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return "IAMServiceAccountRotateRequest{" +
                "accessKeyId='" + accessKeyId + '\'' +
                ", userName='" + userName + '\'' +
                ", accountId='" + accountId + '\'' +
                '}';
    }
}
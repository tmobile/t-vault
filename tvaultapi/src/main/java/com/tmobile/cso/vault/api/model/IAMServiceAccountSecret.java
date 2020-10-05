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

public class IAMServiceAccountSecret implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5122545345759202711L;

    private String accessKeyId;
    private String accessKeySecret;
    private Long expiryDateEpoch;
    private String userName;
    private String awsAccountId;
    /**
     *
     */
    public IAMServiceAccountSecret() {
        super();
    }

    public IAMServiceAccountSecret(String userName, String accessKeyId, String accessKeySecret, Long expiryDateEpoch, String awsAccountId) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.expiryDateEpoch = expiryDateEpoch;
        this.userName = userName;
        this.awsAccountId = awsAccountId;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public Long getExpiryDateEpoch() {
        return expiryDateEpoch;
    }

    public void setExpiryDateEpoch(Long expiryDateEpoch) {
        this.expiryDateEpoch = expiryDateEpoch;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAwsAccountId() {
        return awsAccountId;
    }

    public void setAwsAccountId(String awsAccountId) {
        this.awsAccountId = awsAccountId;
    }

    @Override
    public String toString() {
        return "IAMServiceAccountSecret{" +
                "accessKeyId='" + accessKeyId + '\'' +
                ", accessKeySecret='" + accessKeySecret + '\'' +
                ", expiryDateEpoch=" + expiryDateEpoch +
                ", userName='" + userName + '\'' +
                ", awsAccountId='" + awsAccountId + '\'' +
                '}';
    }
}
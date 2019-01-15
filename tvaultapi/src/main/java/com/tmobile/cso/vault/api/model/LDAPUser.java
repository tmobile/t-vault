// =========================================================================
// Copyright 2018 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License");
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

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

public class LDAPUser implements Serializable {
    private String username;
    private String policies;

    public LDAPUser() { super(); }

    /**
     *
     * @param username
     * @param policies
     */
    public LDAPUser(String username, String policies) {
        super();
        this.username = username;
        this.policies = policies;
    }

    /**
     *
     * @return username
     */
    @ApiModelProperty(example="user01", position=1)
    public String getUsername() {
        return username;
    }

    /**
     *
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     *
     * @return policies
     */
    @ApiModelProperty(example="r_users_safe01,w_users_safe02", position=2)
    public String getPolicies() {
        return policies;
    }

    /**
     *
     * @param policies
     */
    public void setPolicies(String policies) {
        this.policies = policies;
    }
}

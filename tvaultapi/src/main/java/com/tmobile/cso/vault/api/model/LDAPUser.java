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
    @ApiModelProperty(example="username", position=1)
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
    @ApiModelProperty(example="policy1,policy2", position=2)
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

package com.tmobile.cso.vault.api.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

public class LDAPGroup implements Serializable {
    private static final long serialVersionUID = -5040256853994229583L;
    private String groupname;
    private String policies;

    public LDAPGroup() { super(); }

    /**
     *
     * @param groupname
     * @param policies
     */
    public LDAPGroup(String groupname, String policies) {
        super();
        this.groupname = groupname;
        this.policies = policies;
    }

    /**
     *
     * @return username
     */
    @ApiModelProperty(example="groupname", position=1)
    public String getGroupname() {
        return groupname;
    }

    /**
     *
     * @param groupname
     */
    public void setGroupname(String groupname) {
        this.groupname = groupname;
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

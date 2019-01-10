package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AppRoleMetadataDetails implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -4592882556125751131L;

    private String name;

    private String createdBy;

    /**
     *
     */
    public AppRoleMetadataDetails() {
        super();
    }
    /**
     *
     * @param name
     */
    public AppRoleMetadataDetails(String name) {
        super();
        this.name = name;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }
    /**
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

}

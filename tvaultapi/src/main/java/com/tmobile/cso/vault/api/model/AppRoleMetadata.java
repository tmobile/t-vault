
package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Sivakumar
 *
 */
public class AppRoleMetadata implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 9196129835736362627L;

    private String path;
    /**
     * AppRoleMetadataDetails details
     */
    @JsonProperty("data")
    private AppRoleMetadataDetails appRoleMetadataDetails;
    public AppRoleMetadata() {
        super();
    }
    public AppRoleMetadata(String path, AppRoleMetadataDetails appRoleMetadataDetails) {
        super();
        this.path = path;
        this.appRoleMetadataDetails = appRoleMetadataDetails;
    }
    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }
    /**
     * @return the appRoleMetadataDetails
     */
    public AppRoleMetadataDetails getAppRoleMetadataDetails() {
        return appRoleMetadataDetails;
    }
    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }
    /**
     * @param appRoleMetadataDetails the appRoleMetadataDetails to set
     */
    public void setAppRoleMetadataDetails(AppRoleMetadataDetails appRoleMetadataDetails) {
        this.appRoleMetadataDetails = appRoleMetadataDetails;
    }

}

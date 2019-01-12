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

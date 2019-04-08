// =========================================================================
// Copyright 2019 T-Mobile, US
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
public class ServiceAccountMetadata implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9142666370123454032L;

	private String path;

    @JsonProperty("data")
    private ServiceAccountMetadataDetails serviceAccountMetadataDetails;
    public ServiceAccountMetadata() {
        super();
    }
    public ServiceAccountMetadata(String path, ServiceAccountMetadataDetails serviceAccountMetadataDetails) {
        super();
        this.path = path;
        this.serviceAccountMetadataDetails = serviceAccountMetadataDetails;
    }
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @return the serviceAccountMetadataDetails
	 */
	public ServiceAccountMetadataDetails getServiceAccountMetadataDetails() {
		return serviceAccountMetadataDetails;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
	/**
	 * @param serviceAccountMetadataDetails the serviceAccountMetadataDetails to set
	 */
	public void setServiceAccountMetadataDetails(ServiceAccountMetadataDetails serviceAccountMetadataDetails) {
		this.serviceAccountMetadataDetails = serviceAccountMetadataDetails;
	}


}

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


public class AppRoleDetails implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6360594229180017552L;

	private AppRole appRole;
	private String role_id;
	private String[] accessorIds;
	private AppRoleMetadata appRoleMetadata;
	
	/**
	 * 
	 */
	public AppRoleDetails() {
		
	}

	/**
	 * @return the appRole
	 */
	public AppRole getAppRole() {
		return appRole;
	}

	/**
	 * @return the role_id
	 */
	public String getRole_id() {
		return role_id;
	}

	/**
	 * @return the appRoleMetadata
	 */
	public AppRoleMetadata getAppRoleMetadata() {
		return appRoleMetadata;
	}

	/**
	 * @param appRole the appRole to set
	 */
	public void setAppRole(AppRole appRole) {
		this.appRole = appRole;
	}

	/**
	 * @param role_id the role_id to set
	 */
	public void setRole_id(String role_id) {
		this.role_id = role_id;
	}

	/**
	 * @param appRoleMetadata the appRoleMetadata to set
	 */
	public void setAppRoleMetadata(AppRoleMetadata appRoleMetadata) {
		this.appRoleMetadata = appRoleMetadata;
	}

	/**
	 * @return the accessorIds
	 */
	public String[] getAccessorIds() {
		return accessorIds;
	}

	/**
	 * @param accessorIds the accessorIds to set
	 */
	public void setAccessorIds(String[] accessorIds) {
		this.accessorIds = accessorIds;
	}

}

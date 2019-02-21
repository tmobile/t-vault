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

public class AppRoleAccessorIds implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2762825126992163009L;
	private String role_name;
	private String[] accessorIds;
	
	public AppRoleAccessorIds() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the role_name
	 */
	public String getRole_name() {
		return role_name;
	}


	/**
	 * @param role_name the role_name to set
	 */
	public void setRole_name(String role_name) {
		this.role_name = role_name;
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

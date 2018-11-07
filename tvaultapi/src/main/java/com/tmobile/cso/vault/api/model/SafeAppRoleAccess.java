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

import io.swagger.annotations.ApiModelProperty;

public class SafeAppRoleAccess implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3669132184845376706L;

	private String role_name;
	private String path;
	private String access;
	
	public SafeAppRoleAccess() {
		
	}

	public SafeAppRoleAccess(String role_name, String path, String access) {
		super();
		this.role_name = role_name;
		this.path = path;
		this.access = access;
	}

	/**
	 * @return the role_name
	 */
	@ApiModelProperty(example="myvaultapprole", position=1)
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
	 * @return the path
	 */
	@ApiModelProperty(example="shared/mysafe01", position=2)
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the access
	 */
	@ApiModelProperty(example="write", position=3)
	public String getAccess() {
		return access;
	}

	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

}

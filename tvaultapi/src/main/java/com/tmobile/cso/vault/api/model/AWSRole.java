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

public class AWSRole implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4206495527071077050L;
	private String path;
	private String role;
	private String access;
	
	public AWSRole() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param path
	 * @param role
	 * @param access
	 */
	public AWSRole(String path, String role, String access) {
		super();
		this.path = path;
		this.role = role;
		this.access = access;
	}

	/**
	 * @return the path
	 */
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
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @return the access
	 */
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

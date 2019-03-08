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

import io.swagger.annotations.ApiModelProperty;

public class SafeUser implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7074493553779533177L;

	private String path;
	private String username;
	private String access;
	
	
	/**
	 * 
	 */
	public SafeUser() {
		super();
	}


	/**
	 * @param path
	 * @param username
	 * @param access
	 */
	public SafeUser(String path, String username, String access) {
		super();
		this.path = path;
		this.username = username;
		this.access = access;
	}


	/**
	 * @return the path
	 */
	@ApiModelProperty(example="shared/mysafe01", position=1)
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
	 * @return the username
	 */
	@ApiModelProperty(example="testuser1", position=2)
	public String getUsername() {
		return username;
	}


	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
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

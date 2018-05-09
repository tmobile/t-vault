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

public class AppRoleIdSecretId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2730092461765186079L;
	private String role_id;
	private String secret_id;
	
	public AppRoleIdSecretId() {
	}

	public AppRoleIdSecretId(String role_id, String secret_id) {
		super();
		this.role_id = role_id;
		this.secret_id = secret_id;
	}

	/**
	 * @return the role_id
	 */
	@ApiModelProperty(example="31769d1d-5bf3-0e69-5064-4db0103981ca", position=1)
	public String getRole_id() {
		return role_id;
	}

	/**
	 * @param role_id the role_id to set
	 */
	public void setRole_id(String role_id) {
		this.role_id = role_id;
	}

	/**
	 * @return the secret_id
	 */
	@ApiModelProperty(example="61769d1d-5bf3-0e69-5064-4db0103981ca", position=2)
	public String getSecret_id() {
		return secret_id;
	}

	/**
	 * @param secret_id the secret_id to set
	 */
	public void setSecret_id(String secret_id) {
		this.secret_id = secret_id;
	}


	
}

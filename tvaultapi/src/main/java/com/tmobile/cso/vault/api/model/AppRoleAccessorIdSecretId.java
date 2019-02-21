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

public class AppRoleAccessorIdSecretId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5067266911205979139L;

	private String accessor_id;
	private String secret_id;
	
	/**
	 * 
	 */
	public AppRoleAccessorIdSecretId() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the accessor_id
	 */
	public String getAccessor_id() {
		return accessor_id;
	}

	/**
	 * @return the secret_id
	 */
	public String getSecret_id() {
		return secret_id;
	}

	/**
	 * @param accessor_id the accessor_id to set
	 */
	public void setAccessor_id(String accessor_id) {
		this.accessor_id = accessor_id;
	}

	/**
	 * @param secret_id the secret_id to set
	 */
	public void setSecret_id(String secret_id) {
		this.secret_id = secret_id;
	}

	
}

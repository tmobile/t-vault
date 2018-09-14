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

public class AWSClientConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2052708057284340456L;

	public AWSClientConfiguration() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * AWS Access key with permissions to query AWS APIs.
	 */
	private String access_key;
	/**
	 * AWS Secret key with permissions to query AWS APIs.
	 */
	private String secret_key;

	/**
	 * @return the access_key
	 */
	public String getAccess_key() {
		return access_key;
	}
	/**
	 * @return the secret_key
	 */
	public String getSecret_key() {
		return secret_key;
	}
	/**
	 * @param access_key the access_key to set
	 */
	public void setAccess_key(String access_key) {
		this.access_key = access_key;
	}
	/**
	 * @param secret_key the secret_key to set
	 */
	public void setSecret_key(String secret_key) {
		this.secret_key = secret_key;
	}
	
}

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

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

public class ServiceAccountGroup implements Serializable {


	/**
	 *
	 */
	private static final long serialVersionUID = 1971216743215122094L;
	private String svcAccName;
	private String groupname;
	private String access;

	/**
	 *
	 */
	public ServiceAccountGroup() {
		super();
	}


	/**
	 * @param svcAccName
	 * @param groupname
	 * @param access
	 */
	public ServiceAccountGroup(String svcAccName, String groupname, String access) {
		super();
		this.svcAccName = svcAccName;
		this.groupname = groupname;
		this.access = access;
	}


	/**
	 * @return the svcAccName
	 */
	@ApiModelProperty(example="svc_vault_test2", position=1)
	public String getSvcAccName() {
		return svcAccName;
	}


	/**
	 * @param svcAccName the svcAccName to set
	 */
	public void setSvcAccName(String svcAccName) {
		this.svcAccName = svcAccName;
	}


	/**
	 * @return the groupname
	 */
	@ApiModelProperty(example="testuser1", position=2)
	public String getGroupname() {
		return groupname;
	}


	/**
	 * @param groupname the username to set
	 */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}


	/**
	 * @return the access
	 */
	@ApiModelProperty(example="read", position=3, allowableValues="read,write,deny,owner")
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

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

public class ServiceAccountApprole implements Serializable {



	private static final long serialVersionUID = -768319598064659321L;
	private String svcAccName;
	private String approlename;
	private String access;

	public ServiceAccountApprole() {
		super();
	}


	/**
	 * @param svcAccName
	 * @param approlename
	 * @param access
	 */
	public ServiceAccountApprole(String svcAccName, String approlename, String access) {
		super();
		this.svcAccName = svcAccName;
		this.approlename = approlename;
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
	@ApiModelProperty(example="role1", position=2)
	public String getApprolename() {
		return approlename;
	}


	/**
	 * @param approlename the username to set
	 */
	public void setApprolename(String approlename) {
		this.approlename = approlename;
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

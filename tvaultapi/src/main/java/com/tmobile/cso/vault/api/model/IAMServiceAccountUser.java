/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */

package com.tmobile.cso.vault.api.model;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

public class IAMServiceAccountUser implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5621968533287529132L;

	@NotNull
	private String iamSvcAccName;
	@NotNull
	private String username;
	@NotNull
	private String access;
	@NotNull
	@Pattern(regexp = "^$|^[0-9]+$", message = "Invalid AWS account id")
	private String awsAccountId;

	/**
	 *
	 */
	public IAMServiceAccountUser() {
		super();
	}

	/**
	 * @param iamSvcAccName
	 * @param username
	 * @param access
	 */
	public IAMServiceAccountUser(String iamSvcAccName, String username, String access, String awsAccountId) {
		super();
		this.iamSvcAccName = iamSvcAccName;
		this.username = username;
		this.access = access;
		this.awsAccountId = awsAccountId;
	}

	/**
	 * @return the iamSvcAccName
	 */
	@ApiModelProperty(example = "svc_vault_test2", position = 1)
	public String getIamSvcAccName() {
		return iamSvcAccName.toLowerCase();
	}

	/**
	 * @param iamSvcAccName the iamSvcAccName to set
	 */
	public void setIamSvcAccName(String iamSvcAccName) {
		this.iamSvcAccName = iamSvcAccName;
	}

	/**
	 *
	 * @return then awsAccountId
	 */
	@ApiModelProperty(example = "123456789012", position = 2)
	public String getAwsAccountId() {
		return awsAccountId;
	}

	/**
	 *
	 * @param awsAccountId the awsAccountId to set
	 */
	public void setAwsAccountId(String awsAccountId) {
		this.awsAccountId = awsAccountId;
	}

	/**
	 * @return the username
	 */
	@ApiModelProperty(example = "testuser1", position = 3)
	public String getUsername() {
		return username.toLowerCase();
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
	@ApiModelProperty(example = "read", position = 4, allowableValues = "read,reset,deny,owner")
	public String getAccess() {
		return access.toLowerCase();
	}

	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

}

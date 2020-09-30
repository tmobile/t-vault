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
import javax.validation.constraints.Size;

import java.io.Serializable;

public class IAMServiceAccountGroup implements Serializable {

	private static final long serialVersionUID = 7345425327330378618L;

	@NotNull
	@Size(min = 11, max = 30, message = "UserName specified should be minimum 11 chanracters and maximum 30 characters only")
	@Pattern(regexp = "^[a-z0-9_-]+$", message = "Name can have alphabets, numbers, _ and - characters only")
	private String iamSvcAccName;

	@NotNull
	private String groupname;

	@NotNull
	private String access;

	@NotNull
	@Pattern(regexp = "^$|^[0-9]+$", message = "Invalid AWS account id")
	private String awsAccountId;

	/**
	 *
	 */
	public IAMServiceAccountGroup() {
		super();
	}

	/**
	 * @param svcAccName
	 * @param groupname
	 * @param access
	 */
	public IAMServiceAccountGroup(String iamSvcAccName, String groupname, String access, String awsAccountId) {
		super();
		this.iamSvcAccName = iamSvcAccName;
		this.groupname = groupname;
		this.access = access;
		this.awsAccountId = awsAccountId;
	}

	/**
	 * @return the iamSvcAccName
	 */
	@ApiModelProperty(example = "123123_svc_vlt_test2", position = 1)
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
	 * @return the group name
	 */
	@ApiModelProperty(example = "group1", position = 2)
	public String getGroupname() {
		return groupname.toLowerCase();
	}

	/**
	 * @param groupname the group name to set
	 */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	/**
	 * @return the access
	 */
	@ApiModelProperty(example = "read", position = 3, allowableValues = "read,reset,deny,owner")
	public String getAccess() {
		return access.toLowerCase();
	}

	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

	/**
	 * @return the awsAccountId
	 */
	@ApiModelProperty(example = "123456789012", position = 4)
	public String getAwsAccountId() {
		return awsAccountId;
	}

	/**
	 * @param awsAccountId the awsAccountId to set
	 */
	public void setAwsAccountId(String awsAccountId) {
		this.awsAccountId = awsAccountId;
	}
}

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

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModelProperty;

public class OnboardedIAMServiceAccount implements Serializable {

	private static final long serialVersionUID = -4785570097225692483L;

	@NotNull
	@Size(min = 11, max = 18, message = "UserName specified should be minimum 11 chanracters and maximum 18 characters only")
	@Pattern(regexp = "^[a-z0-9_-]+$", message = "Name can have alphabets, numbers, _ and - characters only")
	private String userName;

	@NotNull
	@Size(max = 12, message = "AWSAccountId specified should be maximum 12 characters only")
	private String awsAccountId;

	@NotNull
	@Pattern(regexp = "^$|^[a-z0-9_-]+$", message = "Ownerntid can have alphabets, numbers, _ and - characters only")
	private String ownerNtid;

	public OnboardedIAMServiceAccount() {

	}

	public OnboardedIAMServiceAccount(String userName, String awsAccountId, String ownerNtid) {
		super();
		this.userName = userName;
		this.awsAccountId = awsAccountId;
		this.ownerNtid = ownerNtid;
	}

	/**
	 * @return the userName
	 */
	@ApiModelProperty(example = "12444556_svc_vlt_test2", position = 1)
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the awsAccountId
	 */
	@ApiModelProperty(example = "12444556", position = 2)
	public String getAwsAccountId() {
		return awsAccountId;
	}

	/**
	 * @param awsAccountId the awsAccountId to set
	 */
	public void setAwsAccountId(String awsAccountId) {
		this.awsAccountId = awsAccountId;
	}

	/**
	 * @return the ownerNtid
	 */
	@ApiModelProperty(example = "", position = 3, required = false, allowEmptyValue = true, hidden = true)
	public String getOwnerNtid() {
		return ownerNtid;
	}

	/**
	 * @param ownerNtid the ownerNtid to set
	 */
	public void setOwnerNtid(String ownerNtid) {
		this.ownerNtid = ownerNtid;
	}

	@Override
	public String toString() {
		return "OnboardedIAMServiceAccount [userName=" + userName + ", awsAccountId=" + awsAccountId + ", ownerNtid="
				+ ownerNtid + "]";
	}
}
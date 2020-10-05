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
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class IAMServiceAccount implements Serializable {

	private static final long serialVersionUID = -9011756132661399159L;
	@NotNull
	@Size(min = 11, max = 20, message = "UserName specified should be minimum 11 chanracters and maximum 18 characters only")
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Name can have alphabets, numbers, _ and - characters only")
	private String userName;

	@NotNull
	@Size(max = 12, message = "AWSAccountId specified should be maximum 12 characters only")
	private String awsAccountId;

	@NotNull
	@Size(min = 1, max = 50, message = "AWSAccountName specified should be minimum 1 chanracters and maximum 50 characters only")
	private String awsAccountName;

	@Min(1)
	private Long createdAtEpoch;

	@NotNull
	@Pattern(regexp = "^$|^[a-z0-9_-]+$", message = "Owner can have alphabets, numbers, _ and - characters only")
	@JsonProperty("owner_ntid")
	private String ownerNtid;

	@NotNull
	@Email
	@JsonProperty("owner_email")
	private String ownerEmail;

	@NotNull
	@JsonProperty("application_id")
	private String applicationId;

	@NotNull
	@JsonProperty("application_name")
	private String applicationName;

	@NotNull
	@JsonProperty("application_tag")
	private String applicationTag;

	@NotNull
	private List<IAMSecrets> secret;

	public IAMServiceAccount() {
	}

	public IAMServiceAccount(String userName, String awsAccountId, String awsAccountName, Long createdAtEpoch,
			String ownerNtid, String ownerEmail, String applicationId, String applicationName, String applicationTag,
			List<IAMSecrets> secret) {
		super();
		this.userName = userName;
		this.awsAccountId = awsAccountId;
		this.awsAccountName = awsAccountName;
		this.createdAtEpoch = createdAtEpoch;
		this.ownerNtid = ownerNtid;
		this.ownerEmail = ownerEmail;
		this.applicationId = applicationId;
		this.applicationName = applicationName;
		this.applicationTag = applicationTag;
		this.secret = secret;
	}

	/**
	 * @return the userName
	 */
	@ApiModelProperty(example = "svc_tvt_test2", position = 1)
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
	@ApiModelProperty(example = "testaccountid", position = 2)
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
	 * @return the awsAccountName
	 */
	@ApiModelProperty(example = "testaccountname", position = 3)
	public String getAwsAccountName() {
		return awsAccountName;
	}

	/**
	 * @param awsAccountName the awsAccountName to set
	 */
	public void setAwsAccountName(String awsAccountName) {
		this.awsAccountName = awsAccountName;
	}

	/**
	 * @return the createdAtEpoch
	 */
	@ApiModelProperty(example = "5184000", position = 4)
	public Long getCreatedAtEpoch() {
		return createdAtEpoch;
	}

	/**
	 * @param createdAtEpoch the createdAtEpoch to set
	 */
	public void setCreatedAtEpoch(Long createdAtEpoch) {
		this.createdAtEpoch = createdAtEpoch;
	}

	/**
	 * @return the ownerNtid
	 */
	@ApiModelProperty(example = "testuser1", position = 5)
	public String getOwnerNtid() {
		return ownerNtid;
	}

	/**
	 * @param ownerNtid the ownerNtid to set
	 */
	public void setOwnerNtid(String ownerNtid) {
		this.ownerNtid = ownerNtid;
	}

	/**
	 * @return the ownerEmail
	 */
	public String getOwnerEmail() {
		return ownerEmail;
	}

	/**
	 * @param ownerEmail the ownerEmail to set
	 */
	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	/**
	 * @return the applicationId
	 */
	@ApiModelProperty(example = "ABC", position = 7)
	public String getApplicationId() {
		return applicationId;
	}

	/**
	 * @param applicationId the applicationId to set
	 */
	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	/**
	 * @return the applicationName
	 */
	@ApiModelProperty(example = "app1", position = 8)
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * @return the applicationTag
	 */
	@ApiModelProperty(example = "TAG1", position = 9)
	public String getApplicationTag() {
		return applicationTag;
	}

	/**
	 * @param applicationTag the applicationTag to set
	 */
	public void setApplicationTag(String applicationTag) {
		this.applicationTag = applicationTag;
	}

	/**
	 * @return the secret
	 */
	public List<IAMSecrets> getSecret() {
		return secret;
	}

	/**
	 * @param secret the secret to set
	 */
	public void setSecret(List<IAMSecrets> secret) {
		this.secret = secret;
	}

	@Override
	public String toString() {
		return "IAMServiceAccount [userName=" + userName + ", awsAccountId=" + awsAccountId + ", awsAccountName="
				+ awsAccountName + ", createdAtEpoch=" + createdAtEpoch + ", ownerNtid=" + ownerNtid + ", ownerEmail="
				+ ownerEmail + ", applicationId=" + applicationId + ", applicationName=" + applicationName
				+ ", applicationTag=" + applicationTag + ", secret=" + secret + "]";
	}
}
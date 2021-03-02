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
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SSLCertificateRequest implements Serializable {

	private static final long serialVersionUID = -2386135857129157386L;
	
	@ApiModelProperty(hidden = true)
	private TargetSystemServiceRequest targetSystemServiceRequest;

	@ApiModelProperty(hidden = true)
	private TargetSystem targetSystem;

	@NotNull
	@NotEmpty
	@Pattern(regexp = "^[a-zA-Z0-9*.-]+$", message = "certificateName can have alphabets, numbers, . and - characters only")
	private String certificateName;

	@NotNull
	@NotEmpty
	private String appName;

	@Email
	@ApiModelProperty(hidden = true)
	private String certOwnerEmailId;
	@NotNull
	@NotEmpty
	@ApiModelProperty(example="internal")
	private String certType;

	@ApiModelProperty(hidden = true)
	private String certOwnerNtid;

	@NotNull
	private String[] dnsList;
	
	@NotBlank
	private String notificationEmail;


	@ApiModelProperty(example="server", allowableValues="client,server,both")
	private String keyUsageValue;

	public String getKeyUsageValue() {
		return keyUsageValue;
	}

	public void setKeyUsageValue(String keyUsageValue) {
		this.keyUsageValue = keyUsageValue;
	}

	public String getNotificationEmail() {
		return notificationEmail;
	}
	public void setNotificationEmail(String notificationEmail) {
		this.notificationEmail = notificationEmail;
	}
	public String[] getDnsList() {
		return dnsList;
	}
	public void setDnsList(String[] dnsList) {
		this.dnsList = dnsList;
	}
	public String getCertOwnerNtid() {
		return certOwnerNtid;
	}

	public void setCertOwnerNtid(String certOwnerNtid) {
		this.certOwnerNtid = certOwnerNtid;
	}

	public SSLCertificateRequest() {
		//Empty constructor
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getCertOwnerEmailId() {
		return certOwnerEmailId;
	}
	public void setCertOwnerEmailId(String certOwnerEmailId) {
		this.certOwnerEmailId = certOwnerEmailId;
	}
	public String getCertType() {
		return certType;
	}
	public void setCertType(String certType) {
		this.certType = certType;
	}

	public TargetSystemServiceRequest getTargetSystemServiceRequest() {
		return targetSystemServiceRequest;
	}

	public String getCertificateName() {
		return certificateName.toLowerCase();
	}

	public void setCertificateName(String certificateName) {
		this.certificateName = certificateName;
	}

	public void setTargetSystemServiceRequest(TargetSystemServiceRequest targetSystemServiceRequest) {
		this.targetSystemServiceRequest = targetSystemServiceRequest;
	}
	/**
	 * @return the targetSystem
	 */
	public TargetSystem getTargetSystem() {
		return targetSystem;
	}

	/**
	 * @param targetSystem the targetSystem to set
	 */
	public void setTargetSystem(TargetSystem targetSystem) {
		this.targetSystem = targetSystem;
	}


	@Override
	public String toString() {
		return "SSLCertificateRequest{" +
				"targetSystemServiceRequest=" + targetSystemServiceRequest +
				", targetSystem=" + targetSystem +
				", certificateName='" + certificateName + '\'' +
				", appName='" + appName + '\'' +
				", certOwnerEmailId='" + certOwnerEmailId + '\'' +
				", certType='" + certType + '\'' +
				", certOwnerNtid='" + certOwnerNtid + '\'' +
				", dnsList=" + Arrays.toString(dnsList) +
				",notificationEmail='" + notificationEmail + '\'' +
				'}';
	}
}

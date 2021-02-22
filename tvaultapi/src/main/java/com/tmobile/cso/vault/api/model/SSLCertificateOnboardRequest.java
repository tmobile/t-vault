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

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SSLCertificateOnboardRequest implements Serializable {	
    /**
	 * Serial version Id
	 */
	private static final long serialVersionUID = 2957116172699587871L;

	@ApiModelProperty(hidden = true)
	private TargetSystemServiceRequest targetSystemServiceRequest;

	@ApiModelProperty(hidden = true)
	private TargetSystem targetSystem;

	@NotBlank
	@Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "certificateName can have alphabets, numbers, . and - characters only")
	private String certificateName;

	@NotBlank
	private String appName;

	@NotBlank
	@Email
	@Size(min = 1, message = "Certificate owner email can not be null or empty")
	@Pattern(regexp = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$", message = "Certificate owner email is not valid")
	private String certOwnerEmailId;
	
	@NotBlank
	@ApiModelProperty(example="internal")
	private String certType;

	@ApiModelProperty(hidden = true)
	private String certOwnerNtid;

	@ApiModelProperty(hidden = true)
	private String[] dnsList;
	
	@NotBlank
	private String notificationEmail;
	
	public SSLCertificateOnboardRequest() {
		//Empty constructor
	}

	/**
	 * @return the targetSystemServiceRequest
	 */
	public TargetSystemServiceRequest getTargetSystemServiceRequest() {
		return targetSystemServiceRequest;
	}

	/**
	 * @param targetSystemServiceRequest the targetSystemServiceRequest to set
	 */
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

	/**
	 * @return the certificateName
	 */
	public String getCertificateName() {
		return certificateName;
	}

	/**
	 * @param certificateName the certificateName to set
	 */
	public void setCertificateName(String certificateName) {
		this.certificateName = certificateName;
	}

	/**
	 * @return the appName
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * @param appName the appName to set
	 */
	public void setAppName(String appName) {
		this.appName = appName;
	}

	/**
	 * @return the certOwnerEmailId
	 */
	public String getCertOwnerEmailId() {
		return certOwnerEmailId;
	}

	/**
	 * @param certOwnerEmailId the certOwnerEmailId to set
	 */
	public void setCertOwnerEmailId(String certOwnerEmailId) {
		this.certOwnerEmailId = certOwnerEmailId;
	}

	/**
	 * @return the certType
	 */
	public String getCertType() {
		return certType;
	}

	/**
	 * @param certType the certType to set
	 */
	public void setCertType(String certType) {
		this.certType = certType;
	}

	/**
	 * @return the certOwnerNtid
	 */
	public String getCertOwnerNtid() {
		return certOwnerNtid;
	}

	/**
	 * @param certOwnerNtid the certOwnerNtid to set
	 */
	public void setCertOwnerNtid(String certOwnerNtid) {
		this.certOwnerNtid = certOwnerNtid;
	}

	/**
	 * @return the dnsList
	 */
	public String[] getDnsList() {
		return dnsList;
	}

	/**
	 * @param dnsList the dnsList to set
	 */
	public void setDnsList(String[] dnsList) {
		this.dnsList = dnsList;
	}

	/**
	 * @return the notificationEmail
	 */
	public String getNotificationEmail() {
		return notificationEmail;
	}

	/**
	 * @param notificationEmail the notificationEmail to set
	 */
	public void setNotificationEmail(String notificationEmail) {
		this.notificationEmail = notificationEmail;
	}

	@Override
	public String toString() {
		return "SSLCertificateOnboardRequest [targetSystemServiceRequest=" + targetSystemServiceRequest
				+ ", targetSystem=" + targetSystem + ", certificateName=" + certificateName + ", appName=" + appName
				+ ", certOwnerEmailId=" + certOwnerEmailId + ", certType=" + certType + ", certOwnerNtid="
				+ certOwnerNtid + ", dnsList=" + Arrays.toString(dnsList) + ", notificationEmail=" + notificationEmail
				+ "]";
	}	
}

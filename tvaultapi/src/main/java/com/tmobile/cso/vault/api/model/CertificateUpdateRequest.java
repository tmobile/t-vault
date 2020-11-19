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

import org.hibernate.validator.constraints.NotEmpty;

import io.swagger.annotations.ApiModelProperty;

public class CertificateUpdateRequest implements Serializable {	

	private static final long serialVersionUID = -7957486570893181631L;
	
	
	private String projectLeadEmail;
	private String applicationOwnerEmail;
	@NotNull
	@NotEmpty
	@Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "certificateName can have alphabets, numbers, . and - characters only")
	private String certificateName;	
	
	@NotNull
	@NotEmpty
	@ApiModelProperty(example="internal")
	private String certType;
	private String notificationEmail;
	
	/**
	 * 
	 */
	public CertificateUpdateRequest() {
		super();
	}

	public String getProjectLeadEmail() {
		return projectLeadEmail;
	}

	public void setProjectLeadEmail(String projectLeadEmail) {
		this.projectLeadEmail = projectLeadEmail;
	}

	public String getApplicationOwnerEmail() {
		return applicationOwnerEmail;
	}

	public void setApplicationOwnerEmail(String applicationOwnerEmail) {
		this.applicationOwnerEmail = applicationOwnerEmail;
	}

	public String getCertificateName() {
		return certificateName;
	}

	public void setCertificateName(String certificateName) {
		this.certificateName = certificateName;
	}

	public String getCertType() {
		return certType;
	}

	public void setCertType(String certType) {
		this.certType = certType;
	}

	public String getNotificationEmail() {
		return notificationEmail;
	}

	public void setNotificationEmail(String notificationEmail) {
		this.notificationEmail = notificationEmail;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		return "CertificateUpdateRequest [projectLeadEmail=" + projectLeadEmail + ", applicationOwnerEmail="
				+ applicationOwnerEmail + ", certificateName=" + certificateName + ", certType=" + certType
				+ ", notificationEmail=" + notificationEmail + "]";
	}

	
}

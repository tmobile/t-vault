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

import io.swagger.annotations.ApiModelProperty;

public class CertificateApprole implements Serializable {	
		
	private static final long serialVersionUID = 7712573298410362124L;
		
	private String certificateName;	
	private String approleName;	
	private String access;
	private String certType;
	
	/**
	 * 
	 */
	public CertificateApprole() {
		super();
	}

	/**
	 * @param certificateName
	 * @param approleName
	 * @param access
	 */
	public CertificateApprole(String certificateName, String approleName, String access, String certType) {
		super();		
		this.approleName = approleName;
		this.access = access;
		this.certificateName = certificateName;
		this.certType = certType;
	}
	
	/**
	 * @return the certificateName
	 */
	@ApiModelProperty(example="certname.t-mobile.com", position=1)
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
	 * @return the approleName
	 */
	@ApiModelProperty(example="role1", position=2)
	public String getApproleName() {
		return approleName;
	}

	/**
	 * @param approleName the approleName to set
	 */
	public void setApproleName(String approleName) {
		this.approleName = approleName;
	}	

	/**
	 * @return the access
	 */
	@ApiModelProperty(example="read", position=3)
	public String getAccess() {
		return access;
	}

	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

	/**
	 * @return the certType
	 */
	@ApiModelProperty(example="internal", position=4)
	public String getCertType() {
		return certType;
	}

	/**
	 * @param certType the certType to set
	 */
	public void setCertType(String certType) {
		this.certType = certType;
	}
}

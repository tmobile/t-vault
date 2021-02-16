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

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import io.swagger.annotations.ApiModelProperty;

public class AzureServiceAccountApprole implements Serializable{

	private static final long serialVersionUID = 8173121172146013072L;

	@NotBlank
	@Size(min = 11, message = "Azure service principal name specified should be minimum 11 chanracter only")
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Azure service principal name can have alphabets, numbers, _ and - character only")
	private String azureSvcAccName;

	@NotBlank
	@Size(min = 1, message = "Approle name can not be null or empty")
	private String approlename;

	@NotBlank
	@Size(min = 1, message = "Access can not be null or empty")
	private String access;

	public AzureServiceAccountApprole() {
		super();
	}

	/**
	 * @param iamsvcAccName
	 * @param approlename
	 * @param access
	 * @param awsAccountId
	 */
	public AzureServiceAccountApprole(String azureSvcAccName, String approlename, String access) {
		super();
		this.azureSvcAccName = azureSvcAccName;
		this.approlename = approlename;
		this.access = access;
	}

	/**
	 * @return the azureSvcAccName
	 */
	@ApiModelProperty(example="svc_vault_test2", position=1)
	public String getAzureSvcAccName() {
		return azureSvcAccName;
	}

	/**
	 * @param azureSvcAccName the azureSvcAccName to set
	 */
	public void setAzureSvcAccName(String azureSvcAccName) {
		this.azureSvcAccName = azureSvcAccName;
	}

	/**
	 * @return the approlename
	 */
	@ApiModelProperty(example="role1", position=2)
	public String getApprolename() {
		return approlename.toLowerCase();
	}


	/**
	 * @param approlename the approlename to set
	 */
	public void setApprolename(String approlename) {
		this.approlename = approlename;
	}


	/**
	 * @return the access
	 */
	@ApiModelProperty(example="read", position=3, allowableValues="read,write,deny,owner")
	public String getAccess() {
		return access.toLowerCase();
	}


	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

	@Override
	public String toString() {
		return "AzureServiceAccountApprole [azureSvcAccName=" + azureSvcAccName + ", approlename=" + approlename
				+ ", access=" + access + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((access == null) ? 0 : access.hashCode());
		result = prime * result + ((approlename == null) ? 0 : approlename.hashCode());
		result = prime * result + ((azureSvcAccName == null) ? 0 : azureSvcAccName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AzureServiceAccountApprole other = (AzureServiceAccountApprole) obj;
		if (access == null) {
			if (other.access != null)
				return false;
		} else if (!access.equals(other.access))
			return false;
		if (approlename == null) {
			if (other.approlename != null)
				return false;
		} else if (!approlename.equals(other.approlename))
			return false;
		if (azureSvcAccName == null) {
			if (other.azureSvcAccName != null)
				return false;
		} else if (!azureSvcAccName.equals(other.azureSvcAccName))
			return false;
		return true;
	}
}

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

public class AzureServiceAccountGroup implements Serializable {

	private static final long serialVersionUID = -4793648902185490051L;

	@NotBlank
	@Size(min = 11, message = "Azure service principal name specified should be minimum 11 chanracters only")
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Azure service principal name can have alphabets, numbers, _ and - characters only")
	private String azureSvcAccName;

	@NotBlank
	@Size(min = 1, message = "Group name can not be null or empty")
	private String groupname;

	@NotBlank
	@Size(min = 1, message = "Access can not be null or empty")
	private String access;

	/**
	 *
	 */
	public AzureServiceAccountGroup() {
		super();
	}

	/**
	 * @param azureSvcAccName
	 * @param groupname
	 * @param access
	 */
	public AzureServiceAccountGroup(String azureSvcAccName, String groupname, String access) {
		super();
		this.azureSvcAccName = azureSvcAccName;
		this.groupname = groupname;
		this.access = access;
	}

	/**
	 * @return the azureSvcAccName
	 */
	@ApiModelProperty(example = "svc_vlt_test2", position = 1)
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
	 * @return the group name
	 */
	@ApiModelProperty(example = "group1", position = 2)
	public String getGroupname() {
		return groupname;
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
}

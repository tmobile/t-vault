// =========================================================================
// Copyright 2019 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================
package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModelProperty;

public class OnboardedServiceAccount implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8139191059982483152L;

	@NotNull
	@Size(min = 3, max = 60, message="Name specified should be minimum 3 chanracters and maximum 60 characters only")
	@Pattern( regexp = "^[a-z0-9_-]+$", message="Name can have alphabets, numbers, _ and - characters only")
	private String name;

	@Pattern( regexp = "^$|^[a-z0-9_-]+$", message="Owner can have alphabets, numbers, _ and - characters only")
	private String owner;
	
	public OnboardedServiceAccount() {
		
	}
	public OnboardedServiceAccount(String name, String owner) {
		super();
		this.name = name;
		this.owner = owner;
	}
	/**
	 * @return the name
	 */
	@ApiModelProperty(example="svc_vault_test2", position=1)
	public String getName() {
		return name;
	}
	/**
	 * @return the owner
	 */
	@ApiModelProperty(example="", position=2, required=false, allowEmptyValue=true, hidden=true)
	public String getOwner() {
		return owner;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
		return "OnboardedServiceAccount [name=" + name + ", owner=" + owner + "]";
	}


}
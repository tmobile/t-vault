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

import io.swagger.annotations.ApiModelProperty;

public class SafeBasicDetails implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1928776631283008580L;
	/**
	 * Safe Name
	 */
	private String name;
	/**
	 * Safe Owner
	 */
	private String owner;
	/**
	 * Safe Type
	 */
	private String type;
	/**
	 * Safe Description
	 */
	private String description;
	/**
	 * Owner user id
	 */
	private String ownerid;
	
	/**
	 * 
	 */
	public SafeBasicDetails() {
		super();
	}
	/**
	 * @param name
	 * @param owner
	 * @param type
	 * @param description
	 */
	public SafeBasicDetails(String name, String owner, String type, String description) {
		super();
		this.name = name;
		this.owner = owner;
		this.type = type;
		this.description = description;
	}
	
	
	public SafeBasicDetails(String name, String owner, String type, String description, String ownerid) {
		super();
		this.name = name;
		this.owner = owner;
		this.type = type;
		this.description = description;
		this.ownerid = ownerid;
	}
	/**
	 * @return the name
	 */
	@ApiModelProperty(example="mysafe01")
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the owner
	 */
	@ApiModelProperty(example="youremail@yourcompany.com")
	public String getOwner() {
		return owner;
	}
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}
	/**
	 * @return the type
	 */
	@ApiModelProperty(hidden=true, example = "", allowEmptyValue=true, value="" )
	public String getType() {
		return this.type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the description
	 */
	@ApiModelProperty(example="My first safe")
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the ownerid
	 */
	@ApiModelProperty(example="normaluser")
	public String getOwnerid() {
		return ownerid;
	}
	/**
	 * @param ownerid the ownerid to set
	 */
	public void setOwnerid(String ownerid) {
		this.ownerid = ownerid;
	}
	

}
// =========================================================================
// Copyright 2018 T-Mobile, US
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

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Safe implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 140614555755219544L;
	/**
	 * path
	 */
	private String path;
	/**
	 * Safe Basic details
	 */
	@JsonProperty("data")
	private SafeBasicDetails safeBasicDetails;
	/**
	 * 
	 */
	public Safe() {
		super();
	}
	/**
	 * @param path
	 * @param safeBasicDetails
	 */
	public Safe(String path, SafeBasicDetails safeBasicDetails) {
		super();
		this.path = path;
		this.safeBasicDetails = safeBasicDetails;
	}
	/**
	 * @return the path
	 */
	@ApiModelProperty(example="shared/mysafe01", position=1)
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
	/**
	 * @return the safeBasicDetails
	 */
	public SafeBasicDetails getSafeBasicDetails() {
		return safeBasicDetails;
	}
	/**
	 * @param safeBasicDetails the safeBasicDetails to set
	 */
	public void setSafeBasicDetails(SafeBasicDetails safeBasicDetails) {
		this.safeBasicDetails = safeBasicDetails;
	}
	
}

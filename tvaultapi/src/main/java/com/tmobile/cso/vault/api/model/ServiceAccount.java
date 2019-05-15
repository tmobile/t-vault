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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import io.swagger.annotations.ApiModelProperty;

public class ServiceAccount implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9011756132661399159L;
	@NotNull
	@Size(min = 3, max = 60, message = "Name specified should be minimum 3 chanracters and maximum 60 characters only")
	@Pattern(regexp = "^[a-z0-9_-]+$", message = "Name can have alphabets, numbers, _ and - characters only")
	private String name;

	private boolean autoRotate;
	@Min(1)
	private Long ttl;
	@Min(1)
	@Max(1590897977L)

	private Long max_ttl;
	@JsonIgnore
	private String length;
	@JsonIgnore
	private String formatter;

	@Pattern(regexp = "^$|^[a-z0-9_-]+$", message = "Owner can have alphabets, numbers, _ and - characters only")
	private String owner ;

	public ServiceAccount() {

	}

	public ServiceAccount(String name, boolean autoRotate, Long ttl, Long max_ttl, String length, String formatter,
			String owner) {
		super();
		this.name = name;
		this.autoRotate = autoRotate;
		this.ttl = ttl;
		this.max_ttl = max_ttl;
		this.length = length;
		this.formatter = formatter;
	}

	/**
	 * @return the name
	 */
	@ApiModelProperty(example = "svc_vault_test2", position = 1)
	public String getName() {
		return name;
	}

	/**
	 * @return the autoRotate
	 */
	@ApiModelProperty(example = "false", position = 3)
	public boolean isAutoRotate() {
		return autoRotate;
	}

	/**
	 * @return the ttl
	 */
	@ApiModelProperty(example = "5184000", position = 4)
	public Long getTtl() {
		return ttl;
	}

	/**
	 * @return the max_ttl
	 */
	@ApiModelProperty(example = "5184001", position = 5)
	public Long getMax_ttl() {
		return max_ttl;
	}

	/**
	 * @return the length
	 */
	@ApiModelProperty(example = "64", position = 6, hidden = true)
	public String getLength() {
		return length;
	}

	/**
	 * @return the formatter
	 */
	@ApiModelProperty(example = "Prefix{{password}}", position = 7, hidden = true)
	public String getFormatter() {
		return formatter;
	}

	/**
	 * @return the owner
	 */
	@ApiModelProperty(example = "testuser1", position = 8, hidden = true)
	public String getOwner() {
		return owner;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param autoRotate
	 *            the autoRotate to set
	 */
	public void setAutoRotate(boolean autoRotate) {
		this.autoRotate = autoRotate;
	}

	/**
	 * @param ttl
	 *            the ttl to set
	 */
	public void setTtl(Long ttl) {
		this.ttl = ttl;
	}

	/**
	 * @param max_ttl
	 *            the max_ttl to set
	 */
	public void setMax_ttl(Long max_ttl) {
		this.max_ttl = max_ttl;
	}

	/**
	 * @param length
	 *            the length to set
	 */
	public void setLength(String length) {
		this.length = length;
	}

	/**
	 * @param formatter
	 *            the formatter to set
	 */
	public void setFormatter(String formatter) {
		this.formatter = formatter;
	}

	/**
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public String toString() {
		return "ServiceAccount [name=" + name + ", autoRotate=" + autoRotate + ", ttl=" + ttl + ", max_ttl=" + max_ttl
				+ ", length=" + length + ", formatter=" + formatter + ", owner=" + owner + "]";
	}

}
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

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class AppRole implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4510268414369277124L;
	
	private String role_name;
	private String[] policies;
	private boolean bind_secret_id;
	@Min(0)
	@Max(999999999)
	private Integer secret_id_num_uses;
	@Min(0)
	@Max(999999999)
	private Integer secret_id_ttl;
	@Min(0)
	@Max(999999999)
	private Integer token_num_uses;
	@Min(0)
	@Max(999999999)
	private Integer token_ttl;
	@Min(0)
	@Max(999999999)
	private Integer token_max_ttl;

	public AppRole() {
	}

	public AppRole(String role_name, String[] policies, boolean bind_secret_id, Integer secret_id_num_uses,
			Integer secret_id_ttl, Integer token_num_uses) {
		super();
		this.role_name = role_name;
		this.policies = policies;
		this.bind_secret_id = bind_secret_id;
		this.secret_id_num_uses = secret_id_num_uses;
		this.secret_id_ttl = secret_id_ttl;
		this.token_num_uses = token_num_uses;
	}
	
	public AppRole(String role_name, String[] policies, boolean bind_secret_id, Integer secret_id_num_uses,
			Integer secret_id_ttl, Integer token_num_uses, Integer token_ttl, Integer token_max_ttl) {
		super();
		this.role_name = role_name;
		this.policies = policies;
		this.bind_secret_id = bind_secret_id;
		this.secret_id_num_uses = secret_id_num_uses;
		this.secret_id_ttl = secret_id_ttl;
		this.token_num_uses = token_num_uses;
		this.token_ttl = token_ttl;
		this.token_max_ttl = token_max_ttl;
	}

	/**
	 * @return the role_name
	 */
	@ApiModelProperty(example="myvaultapprole", position=1)
	public String getRole_name() {
		return role_name;
	}

	/**
	 * @param role_name the role_name to set
	 */
	@ApiModelProperty(example="ccp-approle", position=2, required=true)
	public void setRole_name(String role_name) {
		this.role_name = role_name;
	}

	/**
	 * @return the policies
	 */
	@ApiModelProperty(hidden=true, position=3)
	public String[] getPolicies() {
		return policies;
	}

	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(String[] policies) {
		this.policies = policies;
	}

	/**
	 * @return the bind_secret_id
	 */
	@ApiModelProperty(hidden=true, example="true", position=4)
	public boolean isBind_secret_id() {
		return bind_secret_id;
	}

	/**
	 * @param bind_secret_id the bind_secret_id to set
	 */
	public void setBind_secret_id(boolean bind_secret_id) {
		this.bind_secret_id = bind_secret_id;
	}

	/**
	 * @return the secret_id_num_uses
	 */
	@ApiModelProperty(example="1", position=5)
	public Integer getSecret_id_num_uses() {
		return secret_id_num_uses;
	}

	/**
	 * @param secret_id_num_uses the secret_id_num_uses to set
	 */
	public void setSecret_id_num_uses(Integer secret_id_num_uses) {
		this.secret_id_num_uses = secret_id_num_uses;
	}

	/**
	 * @return the secret_id_ttl
	 */
	@ApiModelProperty(example="900", position=6)
	public Integer getSecret_id_ttl() {
		return secret_id_ttl;
	}

	/**
	 * @param secret_id_ttl the secret_id_ttl to set
	 */
	public void setSecret_id_ttl(Integer secret_id_ttl) {
		this.secret_id_ttl = secret_id_ttl;
	}

	/**
	 * @return the token_num_uses
	 */
	@ApiModelProperty(example="1", position=7)
	public Integer getToken_num_uses() {
		return token_num_uses;
	}

	/**
	 * @param token_num_uses the token_num_uses to set
	 */
	public void setToken_num_uses(Integer token_num_uses) {
		this.token_num_uses = token_num_uses;
	}

	/**
	 * @return the token_ttl
	 */
	@ApiModelProperty(example="60", position=8)
	public Integer getToken_ttl() {
		return token_ttl;
	}

	/**
	 * @return the token_max_ttl
	 */
	@ApiModelProperty(example="900", position=9)
	public Integer getToken_max_ttl() {
		return token_max_ttl;
	}

	/**
	 * @param token_ttl the token_ttl to set
	 */
	public void setToken_ttl(Integer token_ttl) {
		this.token_ttl = token_ttl;
	}

	/**
	 * @param token_max_ttl the token_max_ttl to set
	 */
	public void setToken_max_ttl(Integer token_max_ttl) {
		this.token_max_ttl = token_max_ttl;
	}
	
}

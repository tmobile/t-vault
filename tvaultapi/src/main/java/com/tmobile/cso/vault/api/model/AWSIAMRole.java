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

public class AWSIAMRole implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5565582198381225072L;

	public AWSIAMRole() {
		// TODO Auto-generated constructor stub
	}

	private String role;
	private String auth_type;
	private String[] bound_iam_principal_arn;
//	private String inferred_entity_type;
//	private String inferred_aws_region;
//	private String bound_iam_role_arn;
	private String[] policies;
	private Boolean resolve_aws_unique_ids;

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}
	/**
	 * @return the auth_type
	 */
	public String getAuth_type() {
		return auth_type;
	}
	/**
	 * @return the bound_iam_principal_arn
	 */
	public String[] getBound_iam_principal_arn() {
		return bound_iam_principal_arn;
	}
	/**
	 * @return the policies
	 */
	public String[] getPolicies() {
		return policies;
	}
	/**
	 * @return the resolve_aws_unique_ids
	 */
	public Boolean getResolve_aws_unique_ids() {
		return resolve_aws_unique_ids;
	}
	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}
	/**
	 * @param auth_type the auth_type to set
	 */
	public void setAuth_type(String auth_type) {
		this.auth_type = auth_type;
	}
	/**
	 * @param bound_iam_principal_arn the bound_iam_principal_arn to set
	 */
	public void setBound_iam_principal_arn(String[] bound_iam_principal_arn) {
		this.bound_iam_principal_arn = bound_iam_principal_arn;
	}
	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(String[] policies) {
		this.policies = policies;
	}
	/**
	 * @param resolve_aws_unique_ids the resolve_aws_unique_ids to set
	 */
	public void setResolve_aws_unique_ids(Boolean resolve_aws_unique_ids) {
		this.resolve_aws_unique_ids = resolve_aws_unique_ids;
	}
	
}

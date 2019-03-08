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

public class SafeAWSConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4286179672171801875L;
	
	private String role;
	private String bound_account_id; 
	private String bound_region;
	private String bound_vpc_id;
	private String bound_subnet_id;
	private String bound_ami_id;
	private String bound_iam_instance_profile_arn;
	private String bound_iam_role_arn;
	private String policies;
	private String region;

	/**
	 * 
	 */
	public SafeAWSConfiguration() {
		super();
	}

	/**
	 * @param role
	 * @param bound_account_id
	 * @param bound_region
	 * @param bound_vpc_id
	 * @param bound_subnet_id
	 * @param bound_ami_id
	 * @param bound_iam_instance_profile_arn
	 * @param bound_iam_role_arn
	 * @param policies
	 * @param region
	 */
	public SafeAWSConfiguration(String role, String bound_account_id, String bound_region, String bound_vpc_id,
			String bound_subnet_id, String bound_ami_id, String bound_iam_instance_profile_arn,
			String bound_iam_role_arn, String policies, String region) {
		super();
		this.role = role;
		this.bound_account_id = bound_account_id;
		this.bound_region = bound_region;
		this.bound_vpc_id = bound_vpc_id;
		this.bound_subnet_id = bound_subnet_id;
		this.bound_ami_id = bound_ami_id;
		this.bound_iam_instance_profile_arn = bound_iam_instance_profile_arn;
		this.bound_iam_role_arn = bound_iam_role_arn;
		this.policies = policies;
		this.region = region;
	}

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @return the bound_account_id
	 */
	public String getBound_account_id() {
		return bound_account_id;
	}

	/**
	 * @param bound_account_id the bound_account_id to set
	 */
	public void setBound_account_id(String bound_account_id) {
		this.bound_account_id = bound_account_id;
	}

	/**
	 * @return the bound_region
	 */
	public String getBound_region() {
		return bound_region;
	}

	/**
	 * @param bound_region the bound_region to set
	 */
	public void setBound_region(String bound_region) {
		this.bound_region = bound_region;
	}

	/**
	 * @return the bound_vpc_id
	 */
	public String getBound_vpc_id() {
		return bound_vpc_id;
	}

	/**
	 * @param bound_vpc_id the bound_vpc_id to set
	 */
	public void setBound_vpc_id(String bound_vpc_id) {
		this.bound_vpc_id = bound_vpc_id;
	}

	/**
	 * @return the bound_subnet_id
	 */
	public String getBound_subnet_id() {
		return bound_subnet_id;
	}

	/**
	 * @param bound_subnet_id the bound_subnet_id to set
	 */
	public void setBound_subnet_id(String bound_subnet_id) {
		this.bound_subnet_id = bound_subnet_id;
	}

	/**
	 * @return the bound_ami_id
	 */
	public String getBound_ami_id() {
		return bound_ami_id;
	}

	/**
	 * @param bound_ami_id the bound_ami_id to set
	 */
	public void setBound_ami_id(String bound_ami_id) {
		this.bound_ami_id = bound_ami_id;
	}

	/**
	 * @return the bound_iam_instance_profile_arn
	 */
	public String getBound_iam_instance_profile_arn() {
		return bound_iam_instance_profile_arn;
	}

	/**
	 * @param bound_iam_instance_profile_arn the bound_iam_instance_profile_arn to set
	 */
	public void setBound_iam_instance_profile_arn(String bound_iam_instance_profile_arn) {
		this.bound_iam_instance_profile_arn = bound_iam_instance_profile_arn;
	}

	/**
	 * @return the bound_iam_role_arn
	 */
	public String getBound_iam_role_arn() {
		return bound_iam_role_arn;
	}

	/**
	 * @param bound_iam_role_arn the bound_iam_role_arn to set
	 */
	public void setBound_iam_role_arn(String bound_iam_role_arn) {
		this.bound_iam_role_arn = bound_iam_role_arn;
	}

	/**
	 * @return the policies
	 */
	public String getPolicies() {
		return policies;
	}

	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(String policies) {
		this.policies = policies;
	}

	/**
	 * @return the region
	 */
	public String getRegion() {
		return region;
	}

	/**
	 * @param region the region to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	
}

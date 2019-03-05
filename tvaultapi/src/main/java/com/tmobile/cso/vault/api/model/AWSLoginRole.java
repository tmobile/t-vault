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

public class AWSLoginRole implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7895793207885356750L;
	
	private String auth_type;
	private String role;
	private String bound_ami_id;
	private String bound_account_id;
	private String bound_region;
	private String bound_vpc_id;
	private String bound_subnet_id;
	private String bound_iam_role_arn;
	private String bound_iam_instance_profile_arn;
	private String policies;
	
	public AWSLoginRole() {
		// TODO Auto-generated constructor stub
	}

	public AWSLoginRole(String auth_type, String role, String bound_ami_id, String bound_account_id,
			String bound_region, String bound_vpc_id, String bound_subnet_id, String bound_iam_role_arn,
			String bound_iam_instance_profile_arn, String policies) {
		super();
		this.auth_type = auth_type;
		this.role = role;
		this.bound_ami_id = bound_ami_id;
		this.bound_account_id = bound_account_id;
		this.bound_region = bound_region;
		this.bound_vpc_id = bound_vpc_id;
		this.bound_subnet_id = bound_subnet_id;
		this.bound_iam_role_arn = bound_iam_role_arn;
		this.bound_iam_instance_profile_arn = bound_iam_instance_profile_arn;
		this.policies = policies;
	}

	/**
	 * @return the auth_type
	 */
	@ApiModelProperty(example="ec2", position=1)
	public String getAuth_type() {
		return auth_type;
	}

	/**
	 * @return the role
	 */
	@ApiModelProperty(example="mytestawsrole", position=2)
	public String getRole() {
		return role;
	}

	/**
	 * @return the bound_ami_id
	 */
	@ApiModelProperty(example="ami-fce3c696", position=3)
	public String getBound_ami_id() {
		return bound_ami_id;
	}

	/**
	 * @return the bound_account_id
	 */
	@ApiModelProperty(example="1234567890123", position=4)
	public String getBound_account_id() {
		return bound_account_id;
	}

	/**
	 * @return the bound_region
	 */
	@ApiModelProperty(example="us-east-2", position=5)
	public String getBound_region() {
		return bound_region;
	}

	/**
	 * @return the bound_vpc_id
	 */
	@ApiModelProperty(example="vpc-2f09a348", position=6)
	public String getBound_vpc_id() {
		return bound_vpc_id;
	}

	/**
	 * @return the bound_subnet_id
	 */
	@ApiModelProperty(example="subnet-1122aabb", position=7)
	public String getBound_subnet_id() {
		return bound_subnet_id;
	}

	/**
	 * @return the bound_iam_role_arn
	 */
	@ApiModelProperty(example="arn:aws:iam::8987887:role/test-role", position=8)
	public String getBound_iam_role_arn() {
		return bound_iam_role_arn;
	}

	/**
	 * @return the bound_iam_instance_profile_arn
	 */
	@ApiModelProperty(example="arn:aws:iam::877677878:instance-profile/exampleinstanceprofile", position=9)
	public String getBound_iam_instance_profile_arn() {
		return bound_iam_instance_profile_arn;
	}

	/**
	 * @return the policies
	 */
	@ApiModelProperty(example="\"[prod, dev\"]", position=10)
	public String getPolicies() {
		return policies;
	}

	/**
	 * @param auth_type the auth_type to set
	 */
	public void setAuth_type(String auth_type) {
		this.auth_type = auth_type;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @param bound_ami_id the bound_ami_id to set
	 */
	public void setBound_ami_id(String bound_ami_id) {
		this.bound_ami_id = bound_ami_id;
	}

	/**
	 * @param bound_account_id the bound_account_id to set
	 */
	public void setBound_account_id(String bound_account_id) {
		this.bound_account_id = bound_account_id;
	}

	/**
	 * @param bound_region the bound_region to set
	 */
	public void setBound_region(String bound_region) {
		this.bound_region = bound_region;
	}

	/**
	 * @param bound_vpc_id the bound_vpc_id to set
	 */
	public void setBound_vpc_id(String bound_vpc_id) {
		this.bound_vpc_id = bound_vpc_id;
	}

	/**
	 * @param bound_subnet_id the bound_subnet_id to set
	 */
	public void setBound_subnet_id(String bound_subnet_id) {
		this.bound_subnet_id = bound_subnet_id;
	}

	/**
	 * @param bound_iam_role_arn the bound_iam_role_arn to set
	 */
	public void setBound_iam_role_arn(String bound_iam_role_arn) {
		this.bound_iam_role_arn = bound_iam_role_arn;
	}

	/**
	 * @param bound_iam_instance_profile_arn the bound_iam_instance_profile_arn to set
	 */
	public void setBound_iam_instance_profile_arn(String bound_iam_instance_profile_arn) {
		this.bound_iam_instance_profile_arn = bound_iam_instance_profile_arn;
	}

	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(String policies) {
		this.policies = policies;
	}

}

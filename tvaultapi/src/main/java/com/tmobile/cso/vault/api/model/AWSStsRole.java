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

public class AWSStsRole implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7575730485063336784L;

	public AWSStsRole() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * AWS account ID to be associated with STS role.
	 */
	private String account_id;
	/**
	 * AWS ARN for STS role to be assumed
	 */
	private String sts_role;

	/**
	 * @return the account_id
	 */
	public String getAccount_id() {
		return account_id;
	}
	/**
	 * @return the sts_role
	 */
	public String getSts_role() {
		return sts_role;
	}
	/**
	 * @param account_id the account_id to set
	 */
	public void setAccount_id(String account_id) {
		this.account_id = account_id;
	}
	/**
	 * @param sts_role the sts_role to set
	 */
	public void setSts_role(String sts_role) {
		this.sts_role = sts_role;
	}
	
	
}

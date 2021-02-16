// =========================================================================
// Copyright 2020 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License")
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


public class IAMPortalCred implements Serializable {


	private static final long serialVersionUID = -2111025796442426620L;
	/**
	 * roleId
	 */
    private String roleId;
    /**
     * secretId
     */
    private String secretId;

	public IAMPortalCred() {

	}

	public IAMPortalCred(String roleId, String secretId) {
		this.roleId = roleId;
		this.secretId = secretId;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getSecretId() {
		return secretId;
	}

	public void setSecretId(String secretId) {
		this.secretId = secretId;
	}

	@Override
	public String toString() {
		return "IAMPortalCred{" +
				"roleId='" + roleId + '\'' +
				", secretId='" + secretId + '\'' +
				'}';
	}
}

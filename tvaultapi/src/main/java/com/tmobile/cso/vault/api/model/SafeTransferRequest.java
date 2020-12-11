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

import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

public class SafeTransferRequest implements Serializable{

	private static final long serialVersionUID = -1898874660408305314L;

	@NotBlank
	private String safeName;
	@NotBlank
	private String safeType;
	@NotBlank
	private String newOwnerEmail;

	public SafeTransferRequest() {

	}

	public SafeTransferRequest(String safeName, String safeType, String newOwnerEmail) {
		super();
		this.safeName = safeName;
		this.safeType = safeType;
		this.newOwnerEmail = newOwnerEmail;
	}

	public String getSafeName() {
		return safeName;
	}

	public void setSafeName(String safeName) {
		this.safeName = safeName;
	}

	public String getNewOwnerEmail() {
		return newOwnerEmail;
	}

	public void setNewOwnerEmail(String newOwnerNtId) {
		this.newOwnerEmail = newOwnerNtId;
	}

	public String getSafeType() {
		return safeType;
	}

	public void setSafeType(String safeType) {
		this.safeType = safeType;
	}

	@Override
	public String toString() {
		return "SafeTransferRequest{" +
				"safeName='" + safeName + '\'' +
				", safeType='" + safeType + '\'' +
				", newOwnerEmail='" + newOwnerEmail + '\'' +
				'}';
	}
}

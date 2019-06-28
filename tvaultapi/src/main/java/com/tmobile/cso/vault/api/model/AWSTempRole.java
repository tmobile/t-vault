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

public class AWSTempRole implements Serializable {

	private static final long serialVersionUID = 8050691365638616423L;

	private String name;
	private String credential_type;
	private String policy_document;

	public AWSTempRole() {
		// TODO Auto-generated constructor stub
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCredential_type() {
		return credential_type;
	}

	public void setCredential_type(String credential_type) {
		this.credential_type = credential_type;
	}

	public String getPolicy_document() {
		return policy_document;
	}

	public void setPolicy_document(String policy_document) {
		this.policy_document = policy_document;
	}
}

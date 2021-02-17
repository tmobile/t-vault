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
import java.util.List;


public class OIDCGroup implements Serializable {

	private static final long serialVersionUID = -1720191110593009341L;

	private String id;
	private List<String> policies;

	public OIDCGroup() {
	}

	public OIDCGroup(String id, List<String> policies) {
		this.id = id;
		this.policies = policies;
	}

	/**
	 * @return id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return
	 */
	public List<String> getPolicies() {
		return policies;
	}

	/**
	 * @param policies
	 */
	public void setPolicies(List<String> policies) {
		this.policies = policies;
	}

	@Override
	public String toString() {
		return "OIDCGroup{" +
				"id='" + id + '\'' +
				", policies=" + policies +
				'}';
	}
}

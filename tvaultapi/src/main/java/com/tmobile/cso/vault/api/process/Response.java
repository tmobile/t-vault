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

package com.tmobile.cso.vault.api.process;

import java.util.List;

import org.springframework.http.HttpStatus;

public class Response {
	boolean success;
	String response;
	List<String> adminPolicies;
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public void setResponse(String response) {
		this.response = response;
	}
	public void setHttpstatus(HttpStatus httpstatus) {
		this.httpstatus = httpstatus;
	}
	HttpStatus httpstatus;
	
	public HttpStatus getHttpstatus() {
		return httpstatus;
	}
	public boolean isSuccess() {
		return success;
	}
	public String getResponse() {
		if (response != null) {
			return response;
		}
		else {
			return "{} 	";
		}
	}
	/**
	 * @return the adminPolicies
	 */
	public List<String> getAdminPolicies() {
		return adminPolicies;
	}
	/**
	 * @param adminPolicies the adminPolicies to set
	 */
	public void setAdminPolicies(List<String> adminPolicies) {
		this.adminPolicies = adminPolicies;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Response [success=" + success + ", response=" + response + ", adminPolicies=" + adminPolicies
				+ ", httpstatus=" + httpstatus + "]";
	}

}

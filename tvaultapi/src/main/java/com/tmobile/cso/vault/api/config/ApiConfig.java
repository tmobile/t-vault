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

package com.tmobile.cso.vault.api.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties({"#"})
public final class ApiConfig {
	String apiEndPoint;
	String vaultEndPoint;
	String tempVaultEndpoint;
	String method;
	List<Param> params;
	List<String> outparams;
	
	
	public List<String> getOutparams() {
		return outparams;
	}
	public void setOutparams(List<String> outparams) {
		this.outparams = outparams;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getApiEndPoint() {
		return apiEndPoint;
	}
	public void setApiEndPoint(String apiEndPoint) {
		this.apiEndPoint = apiEndPoint;
	}
	public String getVaultEndPoint() {
		return vaultEndPoint;
	}
	public void setVaultEndPoint(String vaultEndPoint) {
		this.vaultEndPoint = vaultEndPoint;
	}
	public List<Param> getParams() {
		return params;
	}
	public void setParams(List<Param> params) {
		this.params = params;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("apiEndPoint:").append(apiEndPoint).
			append(" vaultEndPoint:").append(vaultEndPoint).append(" params :").append(params);
		return sb.toString();
	}
	
}


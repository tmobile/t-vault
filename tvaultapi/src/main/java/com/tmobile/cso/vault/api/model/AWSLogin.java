// =========================================================================
// Copyright 2018 T-Mobile, US
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

public class AWSLogin implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1850106471444613271L;
	private String role;
	private String pkcs7;
	//private String nonce;
	
	public AWSLogin() {
		// TODO Auto-generated constructor stub
	}

	public AWSLogin(String role, String pkcs7/*, String nonce*/) {
		super();
		this.role = role;
		this.pkcs7 = pkcs7;
//		this.nonce = nonce;
	}

	/**
	 * @return the role
	 */
	@ApiModelProperty(example="testawsrole", position=1)
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
	 * @return the pkcs7
	 */
	@ApiModelProperty(example="MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====", position=2)
	public String getPkcs7() {
		return pkcs7;
	}

	/**
	 * @param pkcs7 the pkcs7 to set
	 */
	public void setPkcs7(String pkcs7) {
		this.pkcs7 = pkcs7;
	}

//	/**
//	 * @return the nonce
//	 */
//	@ApiModelProperty(example="MIAGCSqGSIb3DQEHAqCAMIACAQExCzAJBgUrDgMCGgUAMIAGC", position=3)
//	public String getNonce() {
//		return nonce;
//	}
//
//	/**
//	 * @param nonce the nonce to set
//	 */
//	public void setNonce(String nonce) {
//		this.nonce = nonce;
//	}

}

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

public class AWSAuthLogin implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6542131267566141138L;
	private String iam_http_request_method;
	private String iam_request_url;
	private String iam_request_body;
	private String iam_request_headers;
	private String role;
	private String pkcs7;
	
	public AWSAuthLogin() {
		// TODO Auto-generated constructor stub
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
	/**
	 * @return the iam_http_request_method
	 */
	public String getIam_http_request_method() {
		return iam_http_request_method;
	}
	/**
	 * @return the iam_request_url
	 */
	public String getIam_request_url() {
		return iam_request_url;
	}
	/**
	 * @return the iam_request_body
	 */
	public String getIam_request_body() {
		return iam_request_body;
	}
	/**
	 * @return the iam_request_headers
	 */
	public String getIam_request_headers() {
		return iam_request_headers;
	}
	/**
	 * @param iam_http_request_method the iam_http_request_method to set
	 */
	public void setIam_http_request_method(String iam_http_request_method) {
		this.iam_http_request_method = iam_http_request_method;
	}
	/**
	 * @param iam_request_url the iam_request_url to set
	 */
	public void setIam_request_url(String iam_request_url) {
		this.iam_request_url = iam_request_url;
	}
	/**
	 * @param iam_request_body the iam_request_body to set
	 */
	public void setIam_request_body(String iam_request_body) {
		this.iam_request_body = iam_request_body;
	}
	/**
	 * @param iam_request_headers the iam_request_headers to set
	 */
	public void setIam_request_headers(String iam_request_headers) {
		this.iam_request_headers = iam_request_headers;
	}
}

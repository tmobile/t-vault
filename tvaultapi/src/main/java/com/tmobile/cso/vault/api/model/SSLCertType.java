//=========================================================================
//Copyright 2020 T-Mobile, US
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//See the readme.txt file for additional language around disclaimer of warranties.
//=========================================================================
package com.tmobile.cso.vault.api.model;

public enum SSLCertType {

	PRIVATE_SINGLE_SAN("private_single_san"),
	PRIVATE_MULTI_SAN("private_multi_san"),
	PUBLIC_SINGLE_SAN("public_single_san"),
	PUBLIC_MULTI_SAN("public_multi_san");
	
	private String ssl_cert_type;
	SSLCertType(String ssl_cert_type) {
		this.ssl_cert_type=ssl_cert_type;
	}
	/**
	 * @return the ssl_cert_type
	 */
	public String getSsl_cert_type() {
		return ssl_cert_type;
	}
}

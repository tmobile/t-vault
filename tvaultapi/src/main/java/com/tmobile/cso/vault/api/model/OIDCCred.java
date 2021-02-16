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


public class OIDCCred implements Serializable {


	private static final long serialVersionUID = 4077563692404925905L;
	/**
	 * clientName
	 */
    private String clientName;
    /**
     * clientId
     */
    private String clientId;
	/**
	 * clientSecret
	 */
	private String clientSecret;
	/**
	 * boundAudiences
	 */
	private String boundAudiences;
	/**
	 * discoveryUrl
	 */
	private String discoveryUrl;
	/**
	 * adLoginUrl
	 */
	private String adLoginUrl;


	public OIDCCred() {

	}

	public OIDCCred(String clientName, String clientId, String clientSecret, String boundAudiences, String discoveryUrl, String adLoginUrl) {
		this.clientName = clientName;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.boundAudiences = boundAudiences;
		this.discoveryUrl = discoveryUrl;
		this.adLoginUrl = adLoginUrl;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getBoundAudiences() {
		return boundAudiences;
	}

	public void setBoundAudiences(String boundAudiences) {
		this.boundAudiences = boundAudiences;
	}

	public String getDiscoveryUrl() {
		return discoveryUrl;
	}

	public void setDiscoveryUrl(String discoveryUrl) {
		this.discoveryUrl = discoveryUrl;
	}

	public String getAdLoginUrl() {
		return adLoginUrl;
	}

	public void setAdLoginUrl(String adLoginUrl) {
		this.adLoginUrl = adLoginUrl;
	}
}

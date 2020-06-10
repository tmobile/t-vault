// =========================================================================
// Copyright 2020 T-Mobile, US
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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class SSLCertificateRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2386135857129157386L;
	private String SSLCertType;
	private TargetSystemServiceRequest targetSystemServiceRequest;
	private TargetSystem targetSystem;
	private String certificateName;

	public String getCertificateName() {
		return certificateName;
	}

	public void setCertificateName(String certificateName) {
		this.certificateName = certificateName;
	}

	public TargetSystemServiceRequest getTargetSystemServiceRequest() {
		return targetSystemServiceRequest;
	}

	public void setTargetSystemServiceRequest(TargetSystemServiceRequest targetSystemServiceRequest) {
		this.targetSystemServiceRequest = targetSystemServiceRequest;
	}


	public SSLCertificateRequest() {
	}

	/**
	 * @return the targetSystem
	 */
	public TargetSystem getTargetSystem() {
		return targetSystem;
	}

	/**
	 * @param targetSystem the targetSystem to set
	 */
	public void setTargetSystem(TargetSystem targetSystem) {
		this.targetSystem = targetSystem;
	}

	/**
	 * @return the sSLCertTypeConfig
	 */
	public String getSSLCertType() {
		return SSLCertType;
	}

	/**
	 * @param sSLCertType the sSLCertType to set
	 */
	public void setSSLCertType(String sSLCertType) {
		SSLCertType = sSLCertType;
	}


	@Override
	public String toString() {
		return "SSLCertificateRequest{" +
				"SSLCertType='" + SSLCertType + '\'' +
				", targetSystemServiceRequest=" + targetSystemServiceRequest +
				", targetSystem=" + targetSystem +
				", certificateName='" + certificateName + '\'' +
				'}';
	}
}

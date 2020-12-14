/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */

package com.tmobile.cso.vault.api.model;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

public class IAMServiceAccountOffboardRequest implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -4952493295608844065L;

	@NotBlank
	private String iamSvcAccName;
	@NotBlank
	@Pattern(regexp = "^$|^[0-9]+$", message = "Invalid AWS account id")
	private String awsAccountId;

	/**
	 *
	 */
	public IAMServiceAccountOffboardRequest() {
		super();
	}

	public IAMServiceAccountOffboardRequest(String iamSvcAccName, String awsAccountId) {
		this.iamSvcAccName = iamSvcAccName;
		this.awsAccountId = awsAccountId;
	}

	public String getIamSvcAccName() {
		return iamSvcAccName;
	}

	public void setIamSvcAccName(String iamSvcAccName) {
		this.iamSvcAccName = iamSvcAccName;
	}

	public String getAwsAccountId() {
		return awsAccountId;
	}

	public void setAwsAccountId(String awsAccountId) {
		this.awsAccountId = awsAccountId;
	}

	@Override
	public String toString() {
		return "IAMServiceAccountOffboardRequest{" +
				"iamSvcAccName='" + iamSvcAccName + '\'' +
				", awsAccountId='" + awsAccountId + '\'' +
				'}';
	}
}

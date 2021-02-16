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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class IAMSvccAccMetadata implements Serializable {

	private static final long serialVersionUID = 5948955917933522437L;

	private String path;

	@JsonProperty("data")
	private IAMServiceAccountMetadataDetails iamServiceAccountMetadataDetails;

	public IAMSvccAccMetadata(String path, IAMServiceAccountMetadataDetails iamServiceAccountMetadataDetails) {
		super();
		this.path = path;
		this.iamServiceAccountMetadataDetails = iamServiceAccountMetadataDetails;
	}

}

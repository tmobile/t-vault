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

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;

public class ADServiceAccountResetDetails implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8812522610377802443L;
	
	private String modifiedBy;
	private Long modifiedAt;
	
	private ADServiceAccountCreds adServiceAccountCreds;
	
	
	public ADServiceAccountResetDetails() {
		super();
	}

	public ADServiceAccountResetDetails(String modifiedBy, Long modifiedAt,
			ADServiceAccountCreds adServiceAccountCreds) {
		super();
		this.modifiedBy = modifiedBy;
		this.modifiedAt = modifiedAt;
		this.adServiceAccountCreds = adServiceAccountCreds;
	}

	/**
	 * 
	 * @return modifiedAt
	 */
	public String getModifiedBy() {
		return modifiedBy;
	}

	/**
	 * 
	 * @param modifiedBy
	 */
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	/**
	 * 
	 * @return modifiedAt
	 */
	public Long getModifiedAt() {
		return modifiedAt;
	}

	/**
	 * 
	 * @param modifiedAt
	 */
	public void setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	/**
	 * 
	 * @return adServiceAccountCreds
	 */
	public ADServiceAccountCreds getAdServiceAccountCreds() {
		return adServiceAccountCreds;
	}

	/**
	 * 
	 * @param adServiceAccountCreds
	 */
	public void setAdServiceAccountCreds(ADServiceAccountCreds adServiceAccountCreds) {
		this.adServiceAccountCreds = adServiceAccountCreds;
	}

	@Override
	public String toString() {
		return "ADServiceAccountResetDetails [modifiedBy=" + modifiedBy + ", modifiedAt=" + modifiedAt
				+ ", adServiceAccountCreds=" + adServiceAccountCreds + "]";
	}
	
	
	
	

}

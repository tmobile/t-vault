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
import java.util.Date;

public class OnboardedServiceAccountDetails implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6439154829409245624L;

	private String name;

	private String owner;
	/**
	 * Datetime when the password was rotated by Vault last time
	 */
	private String lastVaultRotation;
	/**
	 * Datetime when the password was last set
	 */
	private String passwordLastSet;
	
	private long ttl;
	
	public OnboardedServiceAccountDetails() {
		
	}

	public OnboardedServiceAccountDetails(String name, String owner, String lastVaultRotation, String passwordLastSet,
			long ttl) {
		super();
		this.name = name;
		this.owner = owner;
		this.lastVaultRotation = lastVaultRotation;
		this.passwordLastSet = passwordLastSet;
		this.ttl = ttl;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return the lastVaultRotation
	 */
	public String getLastVaultRotation() {
		return lastVaultRotation;
	}

	/**
	 * @return the passwordLastSet
	 */
	public String getPasswordLastSet() {
		return passwordLastSet;
	}

	/**
	 * @return the ttl
	 */
	public long getTtl() {
		return ttl;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @param lastVaultRotation the lastVaultRotation to set
	 */
	public void setLastVaultRotation(String lastVaultRotation) {
		this.lastVaultRotation = lastVaultRotation;
	}

	/**
	 * @param passwordLastSet the passwordLastSet to set
	 */
	public void setPasswordLastSet(String passwordLastSet) {
		this.passwordLastSet = passwordLastSet;
	}

	/**
	 * @param ttl the ttl to set
	 */
	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	@Override
	public String toString() {
		return "OnboardedServiceAccountDetails [name=" + name + ", owner=" + owner + ", lastVaultRotation="
				+ lastVaultRotation + ", passwordLastSet=" + passwordLastSet + ", ttl=" + ttl + "]";
	}
	
	

}
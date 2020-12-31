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

/**
 * 
 * SafeNode Object
 *
 */
public class SecretCount implements Serializable{


	private static final long serialVersionUID = -5213054737388883528L;

	int totalSecrets;
	SafeSecretCount userSafeSecretCount;
	SafeSecretCount sharedSafeSecretCount;
	SafeSecretCount appsSafeSecretCount;

	public SecretCount() {
		super();
	}

	public SecretCount(SafeSecretCount userSafeSecretCount, SafeSecretCount sharedSafeSecretCount, SafeSecretCount appsSafeSecretCount, int totalSecrets) {
		this.userSafeSecretCount = userSafeSecretCount;
		this.sharedSafeSecretCount = sharedSafeSecretCount;
		this.appsSafeSecretCount = appsSafeSecretCount;
		this.totalSecrets = totalSecrets;
	}

	public SafeSecretCount getUserSafeSecretCount() {
		return userSafeSecretCount;
	}

	public void setUserSafeSecretCount(SafeSecretCount userSafeSecretCount) {
		this.userSafeSecretCount = userSafeSecretCount;
	}

	public SafeSecretCount getSharedSafeSecretCount() {
		return sharedSafeSecretCount;
	}

	public void setSharedSafeSecretCount(SafeSecretCount sharedSafeSecretCount) {
		this.sharedSafeSecretCount = sharedSafeSecretCount;
	}

	public SafeSecretCount getAppsSafeSecretCount() {
		return appsSafeSecretCount;
	}

	public void setAppsSafeSecretCount(SafeSecretCount appsSafeSecretCount) {
		this.appsSafeSecretCount = appsSafeSecretCount;
	}

	public int getTotalSecrets() {
		return totalSecrets;
	}

	public void setTotalSecrets(int totalSecrets) {
		this.totalSecrets = totalSecrets;
	}

	@Override
	public String toString() {
		return "SecretCount{" +
				"totalSecrets=" + totalSecrets +
				", userSafeSecretCount=" + userSafeSecretCount +
				", sharedSafeSecretCount=" + sharedSafeSecretCount +
				", appsSafeSecretCount=" + appsSafeSecretCount +
				'}';
	}
}

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
import java.util.Map;

/**
 * 
 * SafeNode Object
 *
 */
public class SecretCount implements Serializable{


	private static final long serialVersionUID = -5213054737388883528L;

	int totalSafes;
	int totalSecretCount;
	Map<String, Integer> safeSecretCount;
	int next;

	public SecretCount() {
		super();
	}

	public int getTotalSecretCount() {
		return totalSecretCount;
	}

	public void setTotalSecretCount(int totalSecretCount) {
		this.totalSecretCount = totalSecretCount;
	}

	public Map<String, Integer> getSafeSecretCount() {
		return safeSecretCount;
	}

	public void setSafeSecretCount(Map<String, Integer> safeSecretCount) {
		this.safeSecretCount = safeSecretCount;
	}

	public int getTotalSafes() {
		return totalSafes;
	}

	public void setTotalSafes(int totalSafes) {
		this.totalSafes = totalSafes;
	}

	public int getNext() {
		return next;
	}

	public void setNext(int next) {
		this.next = next;
	}

	@Override
	public String toString() {
		return "SecretCount{" +
				"safeSecretCount=" + safeSecretCount +
				", totalSecretCount=" + totalSecretCount +
				", totalSafes=" + totalSafes +
				", next=" + next +
				'}';
	}
}

// =========================================================================
// Copyright 2021 T-Mobile, US
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

public class SecretVersionDetails implements Serializable{


	private static final long serialVersionUID = -5339326589606421383L;

	Long modifiedAt;
	String modifiedBy;

	public SecretVersionDetails() {
		super();
	}

	public SecretVersionDetails(Long modifiedAt, String modifiedBy) {
		this.modifiedAt = modifiedAt;
		this.modifiedBy = modifiedBy;
	}

	public Long getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	@Override
	public String toString() {
		return "SecretVersionDetails{" +
				"modifiedAt=" + modifiedAt +
				", modifiedBy='" + modifiedBy + '\'' +
				'}';
	}
}

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
import java.util.List;
import java.util.Map;

public class FolderVersion implements Serializable{


	private static final long serialVersionUID = 6234099644522723287L;

	String folderPath;
	Long folderModifiedAt;
	String folderModifiedBy;
	Map<String, List<SecretVersionDetails>> secretVersions;

	public FolderVersion() {
		super();
	}

	public FolderVersion(String folderPath, Long folderModifiedAt, String folderModifiedBy, Map<String, List<SecretVersionDetails>> secretVersions) {
		this.folderPath = folderPath;
		this.folderModifiedAt = folderModifiedAt;
		this.folderModifiedBy = folderModifiedBy;
		this.secretVersions = secretVersions;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}

	public Long getFolderModifiedAt() {
		return folderModifiedAt;
	}

	public void setFolderModifiedAt(Long folderModifiedAt) {
		this.folderModifiedAt = folderModifiedAt;
	}

	public String getFolderModifiedBy() {
		return folderModifiedBy;
	}

	public void setFolderModifiedBy(String folderModifiedBy) {
		this.folderModifiedBy = folderModifiedBy;
	}

	public Map<String, List<SecretVersionDetails>> getSecretVersions() {
		return secretVersions;
	}

	public void setSecretVersions(Map<String, List<SecretVersionDetails>> secretVersions) {
		this.secretVersions = secretVersions;
	}

	@Override
	public String toString() {
		return "FolderVersion{" +
				"folderPath='" + folderPath + '\'' +
				", folderModifiedAt=" + folderModifiedAt +
				", folderModifiedBy='" + folderModifiedBy + '\'' +
				", secretVersions=" + secretVersions +
				'}';
	}
}

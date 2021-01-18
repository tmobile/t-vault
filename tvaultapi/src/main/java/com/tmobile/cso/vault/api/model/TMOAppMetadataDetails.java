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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TMOAppMetadataDetails implements Serializable {


	private static final long serialVersionUID = -5203897039537250922L;
	private String applicationName;
	private String applicationOwnerEmailId;
	private String applicationTag;
	private String projectLeadEmailId;
	private boolean updateFlag;
	List<String> internalCertificateList;
	List<String> externalCertificateList;

	public TMOAppMetadataDetails() {
		super();
	}

	public TMOAppMetadataDetails(String applicationName, String applicationOwnerEmailId, String applicationTag,
								 String projectLeadEmailId, List<String> internalCertificateList,
								 List<String> externalCertificateList, boolean updateFlag) {
		this.applicationName = applicationName;
		this.applicationOwnerEmailId = applicationOwnerEmailId;
		this.applicationTag = applicationTag;
		this.projectLeadEmailId = projectLeadEmailId;
		this.internalCertificateList = internalCertificateList;
		this.externalCertificateList = externalCertificateList;
		this.updateFlag = updateFlag;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getApplicationOwnerEmailId() {
		return applicationOwnerEmailId;
	}

	public void setApplicationOwnerEmailId(String applicationOwnerEmailId) {
		this.applicationOwnerEmailId = applicationOwnerEmailId;
	}

	public String getApplicationTag() {
		return applicationTag;
	}

	public void setApplicationTag(String applicationTag) {
		this.applicationTag = applicationTag;
	}

	public String getProjectLeadEmailId() {
		return projectLeadEmailId;
	}

	public void setProjectLeadEmailId(String projectLeadEmailId) {
		this.projectLeadEmailId = projectLeadEmailId;
	}

	public List<String> getInternalCertificateList() {
		return internalCertificateList;
	}

	public void setInternalCertificateList(List<String> internalCertificateList) {
		this.internalCertificateList = internalCertificateList;
	}

	public List<String> getExternalCertificateList() {
		return externalCertificateList;
	}

	public void setExternalCertificateList(List<String> externalCertificateList) {
		this.externalCertificateList = externalCertificateList;
	}

	public boolean isUpdateFlag() {
		return updateFlag;
	}

	public void setUpdateFlag(boolean updateFlag) {
		this.updateFlag = updateFlag;
	}

	@Override
	public String toString() {
		return "TMOAppMetadataDetails{" +
				"applicationName='" + applicationName + '\'' +
				", applicationOwnerEmailId='" + applicationOwnerEmailId + '\'' +
				", applicationTag='" + applicationTag + '\'' +
				", projectLeadEmailId='" + projectLeadEmailId + '\'' +
				", updateFlag=" + updateFlag +
				", internalCertificateList=" + internalCertificateList +
				", externalCertificateList=" + externalCertificateList +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TMOAppMetadataDetails that = (TMOAppMetadataDetails) o;
		return updateFlag == that.updateFlag &&
				applicationName.equals(that.applicationName) &&
				applicationOwnerEmailId.equals(that.applicationOwnerEmailId) &&
				applicationTag.equals(that.applicationTag) &&
				projectLeadEmailId.equals(that.projectLeadEmailId) &&
				internalCertificateList.equals(that.internalCertificateList) &&
				externalCertificateList.equals(that.externalCertificateList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicationName, applicationOwnerEmailId, applicationTag, projectLeadEmailId, updateFlag, internalCertificateList, externalCertificateList);
	}
}

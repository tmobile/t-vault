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

public class WorkloadAppData extends WorkloadAppDetails {

	private static final long serialVersionUID = -5384813254597152554L;

	private String applicationOwnerEmailId;
	private String projectLeadEmail;

	public WorkloadAppData() {
		super();
	}

	public WorkloadAppData(String appName, String appTag, String appID, String applicationOwnerEmailId, String projectLeadEmail) {
		super(appName, appTag, appID);
		this.applicationOwnerEmailId = applicationOwnerEmailId;
		this.projectLeadEmail = projectLeadEmail;
	}

	public String getApplicationOwnerEmailId() {
		return applicationOwnerEmailId;
	}

	public void setApplicationOwnerEmailId(String applicationOwnerEmailId) {
		this.applicationOwnerEmailId = applicationOwnerEmailId;
	}

	public String getProjectLeadEmail() {
		return projectLeadEmail;
	}

	public void setProjectLeadEmail(String projectLeadEmail) {
		this.projectLeadEmail = projectLeadEmail;
	}

	@Override
	public String toString() {
		return "WorkloadAppData{" +
				"applicationOwnerEmailId='" + applicationOwnerEmailId + '\'' +
				", projectLeadEmail='" + projectLeadEmail + '\'' +
				'}';
	}
}

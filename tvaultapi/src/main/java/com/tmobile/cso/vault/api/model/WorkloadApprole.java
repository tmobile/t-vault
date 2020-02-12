// =========================================================================
// Copyright 2020 T-Mobile, US
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


public class WorkloadApprole implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -8536858541409561488L;
	private String appName;
	private String appTag;
	private String appID;

	/**
	 *
	 */
	public WorkloadApprole() {
		
	}

	/**
	 * @return appName
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * @param appName
	 */
	public void setAppName(String appName) {
		this.appName = appName;
	}

	/**
	 * @return appTag
	 */
	public String getAppTag() {
		return appTag;
	}

	/**
	 * @param appTag
	 */
	public void setAppTag(String appTag) {
		this.appTag = appTag;
	}

	/**
	 * @return appID
	 */
	public String getAppID() {
		return appID;
	}

	/**
	 * @param appID
	 */
	public void setAppID(String appID) {
		this.appID = appID;
	}
}

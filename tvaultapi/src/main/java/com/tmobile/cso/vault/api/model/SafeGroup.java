// =========================================================================
// Copyright 2018 T-Mobile, US
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

public class SafeGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3204979163387845879L;

	
	private String path;
	private String groupname;
	private String access;
	
	
	/**
	 * 
	 */
	public SafeGroup() {
		super();
	}


	/**
	 * @param path
	 * @param groupname
	 * @param access
	 */
	public SafeGroup(String path, String groupname, String access) {
		super();
		this.path = path;
		this.groupname = groupname;
		this.access = access;
	}


	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}


	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}


	/**
	 * @return the groupname
	 */
	public String getGroupname() {
		return groupname;
	}


	/**
	 * @param groupname the groupname to set
	 */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}


	/**
	 * @return the access
	 */
	public String getAccess() {
		return access;
	}


	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}
	
	
	
}

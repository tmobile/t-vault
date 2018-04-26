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
import java.util.ArrayList;
import java.util.List;
/**
 * 
 * SafeNode Object
 *
 */
public class SafeNode implements Serializable{
	
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -4614267034665833640L;
	/** Folder/Path */
	private String id;
	/** Value of the folder/Path */
	private String value;
	/** Type of the folder/Path */
	private String type;
	/** Reference to the parent folder/path */
	private String parentId;
	/** List of Sub-folders and/or secrets */
	private List<SafeNode> children = new ArrayList<SafeNode>();

	public SafeNode() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setChildren(List<SafeNode> children) {
		this.children = children;
	}
	
	
	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public SafeNode addChild(SafeNode child) {
		this.children.add(child);
		return child;
	}

	public List<SafeNode> getChildren() {
		return children;
	}
	
	
}

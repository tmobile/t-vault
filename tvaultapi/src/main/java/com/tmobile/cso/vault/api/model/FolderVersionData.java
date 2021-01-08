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

public class FolderVersionData implements Serializable{


	private static final long serialVersionUID = 4192557444259530535L;

	private FolderVersion data;

	public FolderVersionData() {
		super();
	}

	public FolderVersionData(FolderVersion data) {
		this.data = data;
	}

	public FolderVersion getData() {
		return data;
	}

	public void setData(FolderVersion data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "FolderVersionData{" +
				"data=" + data +
				'}';
	}
}

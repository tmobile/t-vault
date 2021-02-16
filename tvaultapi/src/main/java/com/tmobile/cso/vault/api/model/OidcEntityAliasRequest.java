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

public class OidcEntityAliasRequest implements Serializable {

	private static final long serialVersionUID = -2667148285223160156L;

	private String name;
    private String mount_accessor;

    public OidcEntityAliasRequest() {

    }

    public OidcEntityAliasRequest(String name, String mount_accessor) {
        this.name = name;
        this.mount_accessor = mount_accessor;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMount_accessor() {
		return mount_accessor;
	}

	public void setMount_accessor(String mount_accessor) {
		this.mount_accessor = mount_accessor;
	}

	@Override
	public String toString() {
		return "OidcEntityAliasRequest{" +
				"name='" + name + '\'' +
				", mount_accessor='" + mount_accessor + '\'' +
				'}';
	}
}
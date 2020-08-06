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

/**
 * OidcRequest
 */
public class OidcRequest implements Serializable {

	/**
	 * serialVersionUID
	 */
    private static final long serialVersionUID = -5343075241640106262L;

    private String role;
    private String redirect_uri;

    public OidcRequest() {

    }

    public OidcRequest(String role, String redirect_uri) {
        this.role = role;
        this.redirect_uri = redirect_uri;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRedirect_uri() {
        return redirect_uri;
    }

    public void setRedirect_uri(String redirect_uri) {
        this.redirect_uri = redirect_uri;
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OidcRequest other = (OidcRequest) obj;
		if (redirect_uri == null) {
			if (other.redirect_uri != null)
				return false;
		} else if (!redirect_uri.equals(other.redirect_uri))
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OidcRequest [role=" + role + ", redirect_uri=" + redirect_uri + "]";
	}

}
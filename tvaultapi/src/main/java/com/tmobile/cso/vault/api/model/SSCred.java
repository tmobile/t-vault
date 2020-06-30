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


public class SSCred implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3982528028216682904L;

	/**
	 * Username
	 */
    private String username;
    /**
     * Password
     */
    private String password;

	/**
	 * Username
	 */
	private String nclmusername;
	/**
	 * Password
	 */
	private String nclmpassword;

	/**
	 * Workload token.
	 */
	private String cwmToken;

	public SSCred() {

	}

	public SSCred(String username, String password,String nclmusername,String nclmpassword) {
		super();
		this.username = username;
		this.password = password;
		this.nclmusername=nclmusername;
		this.nclmpassword=nclmpassword;
	}

	public String getNclmusername() {
		return nclmusername;
	}

	public void setNclmusername(String nclmusername) {
		this.nclmusername = nclmusername;
	}

	public String getNclmpassword() {
		return nclmpassword;
	}

	public void setNclmpassword(String nclmpassword) {
		this.nclmpassword = nclmpassword;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 *
	 * @return cwmToken
	 */
	public String getCwmToken() {
		return cwmToken;
	}

	/**
	 *
	 * @param cwmToken
	 */
	public void setCwmToken(String cwmToken) {
		this.cwmToken = cwmToken;
	}
}

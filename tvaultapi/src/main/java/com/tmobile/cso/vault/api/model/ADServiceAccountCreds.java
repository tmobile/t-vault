// =========================================================================
// Copyright 2019 T-Mobile, US
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
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;

public class ADServiceAccountCreds implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9219352821495801098L;
	
	private String current_password;
	private String last_password;
	private String username;
	
	
	public ADServiceAccountCreds() {
		super();
	}


	public ADServiceAccountCreds(String current_password, String last_password, String username) {
		super();
		this.current_password = current_password;
		this.last_password = last_password;
		this.username = username;
	}


	/**
	 * @return the current_password
	 */
	public String getCurrent_password() {
		return current_password;
	}


	/**
	 * @return the last_password
	 */
	public String getLast_password() {
		return last_password;
	}


	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}


	/**
	 * @param current_password the current_password to set
	 */
	public void setCurrent_password(String current_password) {
		this.current_password = current_password;
	}


	/**
	 * @param last_password the last_password to set
	 */
	public void setLast_password(String last_password) {
		this.last_password = last_password;
	}


	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return "ADServiceAccountCreds [current_password=" + current_password + ", last_password=" + last_password
				+ ", username=" + username + "]";
	}
	
	

	
}
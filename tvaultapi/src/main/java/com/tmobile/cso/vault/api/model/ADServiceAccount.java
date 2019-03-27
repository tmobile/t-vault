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

public class ADServiceAccount implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 6243675648100232920L;
	/**
	 * User id (First part of the email)
	 */
	private String userId;
	/**
	 * Email Id
	 */
	@JsonIgnore
	private String userEmail;
	/**
	 * Display Name
	 */
	private String displayName;
	/**
	 * Given Name
	 */
	private String givenName;
	
	/**
	 * User id or name
	 */
	private String userName;
	
	@JsonIgnore
	private Instant whenCreated;

	private String accountExpires;
	
	private String pwdLastSet;
	
	private int maxPwdAge;
	
	private String managedBy;
	
	private String passwordExpiry;
	
	private String accountStatus;
	
	private String lockStatus;
	
	private String purpose;

	private String owner;
	
	/**
	 * @return the userId
	 */
	@ApiModelProperty(example="myfirstname.mylastname", position=1)
	public String getUserId() {
		return userId;
	}
	/**
	 * @return the userEmail
	 */
	@ApiModelProperty(example="myfirstname.mylastname@myorganization.com", position=2, hidden=true)
	public String getUserEmail() {
		return userEmail;
	}
	/**
	 * @return the displayName
	 */
	@ApiModelProperty(example="My First Name", position=3)
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * @return the givenName
	 */
	@ApiModelProperty(example="My Last Name", position=4)
	public String getGivenName() {
		return givenName;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @param userEmail the userEmail to set
	 */
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * @param givenName the givenName to set
	 */
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	
	/**
	 * @return the whenCreated
	 */
	@ApiModelProperty(hidden=true)
	public Instant getWhenCreated() {
		return whenCreated;
	}
	/**
	 * Formats the whenCreated using the pattern "yyyy-MM-dd HH:mm:ss"
	 * @return
	 */
	public String getCreationDate() throws IllegalArgumentException, DateTimeException {
		if (whenCreated != null) {
			return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(whenCreated, ZoneOffset.UTC));
		}
		return null;
	}
	/**
	 * @return the accountExpires
	 */
	public String getAccountExpires() {
		return accountExpires;
	}
	/**
	 * @return the passwordExpiry
	 */
	public String getPasswordExpiry() {
		return passwordExpiry;
	}
	/**
	 * @return the accountStatus
	 */
	public String getAccountStatus() {
		return accountStatus;
	}
	/**
	 * @return the lockStatus
	 */
	public String getLockStatus() {
		return lockStatus;
	}
	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}
	/**
	 * @param whenCreated the whenCreated to set
	 */
	public void setWhenCreated(Instant whenCreated) {
		this.whenCreated = whenCreated;
	}
	/**
	 * @param accountExpires the accountExpires to set
	 */
	public void setAccountExpires(String accountExpires) {
		this.accountExpires = accountExpires;
	}
	/**
	 * @param passwordExpiry the passwordExpiry to set
	 */
	public void setPasswordExpiry(String passwordExpiry) {
		this.passwordExpiry = passwordExpiry;
	}
	/**
	 * @param accountStatus the accountStatus to set
	 */
	public void setAccountStatus(String accountStatus) {
		this.accountStatus = accountStatus;
	}
	/**
	 * @param lockStatus the lockStatus to set
	 */
	public void setLockStatus(String lockStatus) {
		this.lockStatus = lockStatus;
	}
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the pwdLastSet
	 */
	public String getPwdLastSet() {
		return pwdLastSet;
	}
	/**
	 * @return the managedBy
	 */
	public String getManagedBy() {
		return managedBy;
	}
	/**
	 * @param pwdLastSet the pwdLastSet to set
	 */
	public void setPwdLastSet(String pwdLastSet) {
		this.pwdLastSet = pwdLastSet;
	}
	/**
	 * @param managedBy the managedBy to set
	 */
	public void setManagedBy(String managedBy) {
		this.managedBy = managedBy;
	}
	
	/**
	 * @return the maxPwdAge
	 */
	public int getMaxPwdAge() {
		return maxPwdAge;
	}
	/**
	 * @param maxPwdAge the maxPwdAge to set
	 */
	public void setMaxPwdAge(int maxPwdAge) {
		this.maxPwdAge = maxPwdAge;
	}

	/**
	 * @return the purpose
	 */
	public String getPurpose() {
		return purpose;
	}
	/**
	 * @param purpose the purpose to set
	 */
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	@Override
	public String toString() {
		return "ADServiceAccount [userId=" + userId + ", userEmail=" + userEmail + ", displayName=" + displayName
				+ ", givenName=" + givenName + ", userName=" + userName + ", whenCreated=" + whenCreated
				+ ", accountExpires=" + accountExpires + ", pwdLastSet=" + pwdLastSet + ", maxPwdAge=" + maxPwdAge
				+ ", managedBy=" + managedBy + ", passwordExpiry=" + passwordExpiry + ", accountStatus=" + accountStatus
				+ ", lockStatus=" + lockStatus + ", purpose=" + purpose + "]";
	}


	
}
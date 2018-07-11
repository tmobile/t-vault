package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;

public class DirectoryUser implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7199490759079547738L;

	
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
	@ApiModelProperty(example="myfirstname.mylastname@myorganization.com", position=2)
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
	 * User id (First part of the email)
	 */
	private String userId;
	/**
	 * Email Id
	 */
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
	@JsonIgnore
	private String userName;


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
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DirectoryUser [userId=" + userId + ", userEmail=" + userEmail + ", displayName=" + displayName
				+ ", givenName=" + givenName + "]";
	}
	
}
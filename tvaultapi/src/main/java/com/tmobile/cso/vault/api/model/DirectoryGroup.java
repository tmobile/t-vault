package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

public class DirectoryGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1492398382854693144L;
	/**
	 * Group Name
	 */
	private String groupName;
	/** Display Name
	 * 
	 */
	private String displayName;
	/** Email
	 * 
	 */
	private String email;

	/**
	 * @return the groupName
	 */
	@ApiModelProperty(example="My Group Name", position=1)
	public String getGroupName() {
		return groupName;
	}

	/**
	 * @param groupName the groupName to set
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DirectoryGroup [groupName=" + groupName + ", displayName=" + displayName + ", email=" + email + "]";
	}

}
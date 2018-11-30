/**
 * 
 */
package com.tmobile.cso.vault.api.model;

import java.io.Serializable;


public class UserDetails implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5813662317092946084L;

	private String username;
	private String clientToken;
	private boolean admin;
	private String selfSupportToken;
	private String[] policies;
	private String[] sudoPolicies;
	private String access;
	private Integer leaseDuration;
	
	/**
	 * 
	 */
	public UserDetails() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the clientToken
	 */
	public String getClientToken() {
		return clientToken;
	}

	/**
	 * @return the admin
	 */
	public boolean isAdmin() {
		return admin;
	}

	/**
	 * @return the selfSupportToken
	 */
	public String getSelfSupportToken() {
		return selfSupportToken;
	}

	/**
	 * @return the policies
	 */
	public String[] getPolicies() {
		return policies;
	}


	/**
	 * @return the access
	 */
	public String getAccess() {
		return access;
	}

	/**
	 * @return the leaseDuration
	 */
	public Integer getLeaseDuration() {
		return leaseDuration;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @param clientToken the clientToken to set
	 */
	public void setClientToken(String clientToken) {
		this.clientToken = clientToken;
	}

	/**
	 * @param admin the admin to set
	 */
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	/**
	 * @param selfSupportToken the selfSupportToken to set
	 */
	public void setSelfSupportToken(String selfSupportToken) {
		this.selfSupportToken = selfSupportToken;
	}

	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(String[] policies) {
		this.policies = policies;
	}

	/**
	 * @return the sudoPolicies
	 */
	public String[] getSudoPolicies() {
		return sudoPolicies;
	}

	/**
	 * @param sudoPolicies the sudoPolicies to set
	 */
	public void setSudoPolicies(String[] sudoPolicies) {
		this.sudoPolicies = sudoPolicies;
	}

	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

	/**
	 * @param leaseDuration the leaseDuration to set
	 */
	public void setLeaseDuration(Integer leaseDuration) {
		this.leaseDuration = leaseDuration;
	}


}

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class VaultTokenLookupDetails implements Serializable {

	private static final long serialVersionUID = 634420468074255217L;

	public VaultTokenLookupDetails() {
		// TODO Auto-generated constructor stub
	}
	
	private String username;
	private String token;
	private String[] policies;
	private boolean valid;
	private boolean admin;

	public VaultTokenLookupDetails(String username, String token, String[] policies) {
		super();
		this.username = username;
		this.token = token;
		this.policies = policies;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @return the policies
	 */
	public String[] getPolicies() {
		return policies;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(String[] policies) {
		this.policies = policies;
	}

	/**
	 * @return the valid
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * @param valid the valid to set
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * @return the admin
	 */
	public boolean isAdmin() {
		return admin;
	}

	/**
	 * @param admin the admin to set
	 */
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}
	
	

}

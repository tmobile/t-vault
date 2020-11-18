package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class AzureServiceAccountUser implements Serializable{
	
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -2100272556105686915L;

	@NotNull
	private String azureSvcAccName;
	@NotNull
	private String username;
	@NotNull
	private String access;
	public String getAzureSvcAccName() {
		return azureSvcAccName;
	}
	public void setAzureSvcAccName(String azureSvcAccName) {
		this.azureSvcAccName = azureSvcAccName;
	}
	public String getAccess() {
		return access;
	}
	public void setAccess(String access) {
		this.access = access;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * 
	 */
	public AzureServiceAccountUser() {
		super();
	}
	/**
	 * 
	 * @param azureSvcAccName
	 * @param username
	 * @param access
	 */
	public AzureServiceAccountUser(String azureSvcAccName, String username, String access) {
		super();
		this.azureSvcAccName = azureSvcAccName;
		this.username = username;
		this.access = access;
	}
	@Override
	public String toString() {
		return "AzureServiceAccountUser [azureSvcAccName=" + azureSvcAccName + ", username=" + username + ", access="
				+ access + "]";
	}

}

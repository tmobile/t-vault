package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import io.swagger.annotations.ApiModelProperty;

public class AzureServiceAccountUser implements Serializable {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -2100272556105686915L;

	@NotBlank
	@Size(min = 11, message = "Azure service principal name specified should be minimum 11 chanracters only")
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Azure service principal name can have alphabets, numbers, _ and - characters only")
	private String azureSvcAccName;

	@NotBlank
	@Size(min = 1, message = "User name can not be null or empty")
	private String username;

	@NotBlank
	@Size(min = 1, message = "Access can not be null or empty")
	private String access;

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

	@ApiModelProperty(example = "svc_vault_test2", position = 1)
	public String getAzureSvcAccName() {
		return azureSvcAccName;
	}

	public void setAzureSvcAccName(String azureSvcAccName) {
		this.azureSvcAccName = azureSvcAccName;
	}

	@ApiModelProperty(example = "testuser1", position = 2)
	public String getUsername() {
		return username.toLowerCase();
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@ApiModelProperty(example = "read", position = 3, allowableValues = "read,reset,deny,owner")
	public String getAccess() {
		return access.toLowerCase();
	}

	public void setAccess(String access) {
		this.access = access;
	}

	@Override
	public String toString() {
		return "AzureServiceAccountUser [azureSvcAccName=" + azureSvcAccName + ", username=" + username + ", access="
				+ access + "]";
	}
}

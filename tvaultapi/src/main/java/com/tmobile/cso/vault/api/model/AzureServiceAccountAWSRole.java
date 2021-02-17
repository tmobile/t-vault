package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

public class AzureServiceAccountAWSRole implements Serializable{
	
	
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -4125276066220976943L;

	@NotBlank
	@Size(min = 11, max = 30, message = "Azure SvcAccName specified should be minimum 11 characters and maximum 30 characters only")
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Name can have alphabets, numbers, _ and - characters only")
	private String azureSvcAccName;

	@NotBlank
	@Size(min = 1, message = "Role name can not be null or empty")
	private String rolename;

	@NotBlank
	@Size(min = 1, message = "Access can not be null or empty")
	private String access;

	public AzureServiceAccountAWSRole(String azureSvcAccName, String rolename, String access) {
		super();
		this.azureSvcAccName = azureSvcAccName;
		this.rolename = rolename;
		this.access = access;
	}
	
	public AzureServiceAccountAWSRole(){
		super();
	}

	public String getAzureSvcAccName() {
		return azureSvcAccName;
	}

	public void setAzureSvcAccName(String azureSvcAccName) {
		this.azureSvcAccName = azureSvcAccName;
	}

	public String getRolename() {
		return rolename;
	}

	public void setRolename(String rolename) {
		this.rolename = rolename;
	}

	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}

	@Override
	public String toString() {
		return "AzureServiceAccountAWSRole [azureSvcAccName=" + azureSvcAccName + ", rolename=" + rolename + ", access="
				+ access + "]";
	}
}

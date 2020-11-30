package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class OnboardedAzureServiceAccount implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 21049416320987295L;

	@NotNull
	private String servicePrincipalName;

	@NotNull
	@Pattern(regexp = "^$|^[a-z0-9_-]+$", message = "Ownerntid can have alphabets, numbers, _ and - characters only")
	private String ownerNtid;

	public String getServicePrincipalName() {
		return servicePrincipalName;
	}

	public void setServicePrincipalName(String servicePrincipalName) {
		this.servicePrincipalName = servicePrincipalName;
	}

	public String getOwnerNtid() {
		return ownerNtid;
	}

	public void setOwnerNtid(String ownerNtid) {
		this.ownerNtid = ownerNtid;
	}

	@Override
	public String toString() {
		return "OnboardedAzureServiceAccount [servicePrincipalName=" + servicePrincipalName + ", ownerNtid="
				+ ownerNtid + "]";
	}

	/**
	 * 
	 * @param servicePrincipalName
	 * @param ownerNtid
	 */
	public OnboardedAzureServiceAccount(String servicePrincipalName, String ownerNtid) {
		super();
		this.servicePrincipalName = servicePrincipalName;
		this.ownerNtid = ownerNtid;
	}

	/**
	 * 
	 */
	public OnboardedAzureServiceAccount() {
		super();
	}
	
	

}

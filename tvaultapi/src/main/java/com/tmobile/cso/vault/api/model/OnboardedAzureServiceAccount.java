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
	private String servicePrinicipalName;

	@NotNull
	@Pattern(regexp = "^$|^[a-z0-9_-]+$", message = "Ownerntid can have alphabets, numbers, _ and - characters only")
	private String ownerNtid;

	public String getServicePrinicipalName() {
		return servicePrinicipalName;
	}

	public void setServicePrinicipalName(String servicePrinicipalName) {
		this.servicePrinicipalName = servicePrinicipalName;
	}

	public String getOwnerNtid() {
		return ownerNtid;
	}

	public void setOwnerNtid(String ownerNtid) {
		this.ownerNtid = ownerNtid;
	}

	@Override
	public String toString() {
		return "OnboardedAzureServiceAccount [servicePrinicipalName=" + servicePrinicipalName + ", ownerNtid="
				+ ownerNtid + "]";
	}

	/**
	 * 
	 * @param servicePrinicipalName
	 * @param ownerNtid
	 */
	public OnboardedAzureServiceAccount(String servicePrinicipalName, String ownerNtid) {
		super();
		this.servicePrinicipalName = servicePrinicipalName;
		this.ownerNtid = ownerNtid;
	}

	/**
	 * 
	 */
	public OnboardedAzureServiceAccount() {
		super();
	}
	
	

}

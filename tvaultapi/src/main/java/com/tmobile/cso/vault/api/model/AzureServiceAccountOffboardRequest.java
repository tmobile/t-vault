package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import org.hibernate.validator.constraints.NotBlank;

public class AzureServiceAccountOffboardRequest implements Serializable{
	
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 5001836557562126801L;
	
	@NotBlank
	private String azureSvcAccName;
	
	public AzureServiceAccountOffboardRequest(){
		super();
	}

	public AzureServiceAccountOffboardRequest(String azureSvcAccName) {
		super();
		this.azureSvcAccName = azureSvcAccName;
	}

	public String getAzureSvcAccName() {
		return azureSvcAccName;
	}

	public void setAzureSvcAccName(String azureSvcAccName) {
		this.azureSvcAccName = azureSvcAccName;
	}

	@Override
	public String toString() {
		return "AzureServiceAccountOffboardRequest [azureSvcAccName=" + azureSvcAccName + "]";
	}
	
	

}

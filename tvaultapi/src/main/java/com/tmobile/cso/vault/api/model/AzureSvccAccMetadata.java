package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AzureSvccAccMetadata implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 302696986780618715L;
	
	private String path;

	@JsonProperty("data")
	private AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails;

	public AzureSvccAccMetadata(String path, AzureServiceAccountMetadataDetails azureServiceAccountMetadataDetails) {
		super();
		this.path = path;
		this.azureServiceAccountMetadataDetails = azureServiceAccountMetadataDetails;
	}
}

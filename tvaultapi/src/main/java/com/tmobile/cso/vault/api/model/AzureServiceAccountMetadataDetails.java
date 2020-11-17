package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AzureServiceAccountMetadataDetails implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -7689315256335697595L;
	
	private String servicePrinicipalName;
	private String servicePrinicipalId;
	private String servicePrinicipalClientId;
	private String tenantId;
	private Long createdAtEpoch;

	@JsonProperty("owner_ntid")
	private String ownerNtid;

	@JsonProperty("owner_email")
	private String ownerEmail;

	@JsonProperty("application_id")
	private String applicationId;

	@JsonProperty("application_name")
	private String applicationName;

	@JsonProperty("application_tag")
	private String applicationTag;

	@JsonProperty("isActivated")
	private boolean accountActivated;

	private List<AzureSecretsMetadata> secret;

	public String getServicePrinicipalName() {
		return servicePrinicipalName;
	}

	public void setServicePrinicipalName(String servicePrinicipalName) {
		this.servicePrinicipalName = servicePrinicipalName;
	}

	public String getServicePrinicipalId() {
		return servicePrinicipalId;
	}

	public void setServicePrinicipalId(String servicePrinicipalId) {
		this.servicePrinicipalId = servicePrinicipalId;
	}

	public String getServicePrinicipalClientId() {
		return servicePrinicipalClientId;
	}

	public void setServicePrinicipalClientId(String servicePrinicipalClientId) {
		this.servicePrinicipalClientId = servicePrinicipalClientId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Long getCreatedAtEpoch() {
		return createdAtEpoch;
	}

	public void setCreatedAtEpoch(Long createdAtEpoch) {
		this.createdAtEpoch = createdAtEpoch;
	}

	public String getOwnerNtid() {
		return ownerNtid;
	}

	public void setOwnerNtid(String ownerNtid) {
		this.ownerNtid = ownerNtid;
	}

	public String getOwnerEmail() {
		return ownerEmail;
	}

	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getApplicationTag() {
		return applicationTag;
	}

	public void setApplicationTag(String applicationTag) {
		this.applicationTag = applicationTag;
	}

	public boolean isAccountActivated() {
		return accountActivated;
	}

	public void setAccountActivated(boolean accountActivated) {
		this.accountActivated = accountActivated;
	}

	public List<AzureSecretsMetadata> getSecret() {
		return secret;
	}

	public void setSecret(List<AzureSecretsMetadata> secret) {
		this.secret = secret;
	}

	@Override
	public String toString() {
		return "AzureServiceAccountMetadataDetails [servicePrinicipalName=" + servicePrinicipalName
				+ ", servicePrinicipalId=" + servicePrinicipalId + ", servicePrinicipalClientId="
				+ servicePrinicipalClientId + ", tenantId=" + tenantId + ", createdAtEpoch=" + createdAtEpoch
				+ ", ownerNtid=" + ownerNtid + ", ownerEmail=" + ownerEmail + ", applicationId=" + applicationId
				+ ", applicationName=" + applicationName + ", applicationTag=" + applicationTag + ", accountActivated="
				+ accountActivated + ", secret=" + secret + "]";
	}

	

}

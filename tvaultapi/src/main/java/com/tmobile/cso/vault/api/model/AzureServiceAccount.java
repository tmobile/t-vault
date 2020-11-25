package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AzureServiceAccount implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -5548453449664949292L;
	
	@NotBlank
	@Size(min = 11, message = "Azure service principal name specified should be minimum 11 chanracters only")
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Azure service principal name can have alphabets, numbers, _ and - characters only")
	private String servicePrinicipalName;

	@NotBlank
	@Size(min = 10, max = 128, message = "ServicePrinicipalId specified should be minimum 10 chanracters and maximum 128 characters only")
	private String servicePrinicipalId;

	@NotBlank
	@Size(min = 10, max = 128, message = "ServicePrinicipalClientId specified should be minimum 10 chanracters and maximum 128 characters only")
	private String servicePrinicipalClientId;
	
	@NotBlank
	@Size(min = 10, max = 128, message = "TenantId specified should be minimum 10 chanracters and maximum 128 characters only")
	private String tenantId;

	@NotNull
	@Min(1)
	private Long createdAtEpoch;

	@NotBlank
	@Pattern(regexp = "^$|^[a-zA-Z0-9_-]+$", message = "Owner can have alphabets, numbers, _ and - characters only")
	@JsonProperty("owner_ntid")
	private String ownerNtid;

	@NotBlank
	@Email
	@Size(min = 1, message = "Owner Email can not be null or empty")
	@Pattern(regexp = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$", message = "Owner Email is not valid")
	@JsonProperty("owner_email")
	private String ownerEmail;

	@NotBlank
	@Size(min = 1, message = "Application Id can not be null or empty")
	@JsonProperty("application_id")
	private String applicationId;

	@NotBlank
	@Size(min = 1, message = "Application Name can not be null or empty")
	@JsonProperty("application_name")
	private String applicationName;

	@NotBlank
	@Size(min = 1, message = "Application Tag can not be null or empty")
	@JsonProperty("application_tag")
	private String applicationTag;

	@Valid
	@NotEmpty
	private List<AzureSecrets> secret;
	
	
	/**
	 * Default Constructor
	 */
	public AzureServiceAccount() {
	}
	
	
	/**
	 * 
	 * @param servicePrinicipalName
	 * @param servicePrinicipalId
	 * @param servicePrinicipalClientId
	 * @param tenantId
	 * @param createdAtEpoch
	 * @param ownerNtid
	 * @param ownerEmail
	 * @param applicationId
	 * @param applicationName
	 * @param applicationTag
	 * @param secret
	 */
	public AzureServiceAccount(String servicePrinicipalName, String servicePrinicipalId,
			String servicePrinicipalClientId, String tenantId, Long createdAtEpoch, String ownerNtid, String ownerEmail,
			String applicationId, String applicationName, String applicationTag, List<AzureSecrets> secret) {
		super();
		this.servicePrinicipalName = servicePrinicipalName;
		this.servicePrinicipalId = servicePrinicipalId;
		this.servicePrinicipalClientId = servicePrinicipalClientId;
		this.tenantId = tenantId;
		this.createdAtEpoch = createdAtEpoch;
		this.ownerNtid = ownerNtid;
		this.ownerEmail = ownerEmail;
		this.applicationId = applicationId;
		this.applicationName = applicationName;
		this.applicationTag = applicationTag;
		this.secret = secret;
	}



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

	public List<AzureSecrets> getSecret() {
		return secret;
	}

	public void setSecret(List<AzureSecrets> secret) {
		this.secret = secret;
	}

	@Override
	public String toString() {
		return "AzureServicePrinicipalAccount [servicePrinicipalName=" + servicePrinicipalName
				+ ", servicePrinicipalId=" + servicePrinicipalId + ", servicePrinicipalClientId="
				+ servicePrinicipalClientId + ", tenantId=" + tenantId + ", createdAtEpoch=" + createdAtEpoch
				+ ", ownerNtid=" + ownerNtid + ", ownerEmail=" + ownerEmail + ", applicationId=" + applicationId
				+ ", applicationName=" + applicationName + ", applicationTag=" + applicationTag + ", secret=" + secret
				+ "]";
	}
	
}

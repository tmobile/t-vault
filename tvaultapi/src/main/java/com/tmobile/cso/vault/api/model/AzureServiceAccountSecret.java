package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AzureServiceAccountSecret implements Serializable{
	
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 2498387310154866937L;
	
	
		private String secretKeyId;
	    private String secretText;
	    private Long expiryDateEpoch;
	    private String expiryDate;
	    private String servicePrinicipalId;
		private String tenantId;
		public String getSecretKeyId() {
			return secretKeyId;
		}
		public void setSecretKeyId(String secretKeyId) {
			this.secretKeyId = secretKeyId;
		}
		public String getSecretText() {
			return secretText;
		}
		public void setSecretText(String secretText) {
			this.secretText = secretText;
		}
		public Long getExpiryDateEpoch() {
			return expiryDateEpoch;
		}
		public void setExpiryDateEpoch(Long expiryDateEpoch) {
			this.expiryDateEpoch = expiryDateEpoch;
		}
		public String getExpiryDate() {
			return expiryDate;
		}
		public void setExpiryDate(String expiryDate) {
			this.expiryDate = expiryDate;
		}	
		public String getServicePrinicipalId() {
			return servicePrinicipalId;
		}
		public void setServicePrinicipalId(String servicePrinicipalId) {
			this.servicePrinicipalId = servicePrinicipalId;
		}
		public String getTenantId() {
			return tenantId;
		}
		public void setTenantId(String tenantId) {
			this.tenantId = tenantId;
		}
		@Override
		public String toString() {
			return "AzureServiceAccountSecret [secretKeyId=" + secretKeyId + ", secretText=" + secretText
					+ ", expiryDateEpoch=" + expiryDateEpoch + ", expiryDate=" + expiryDate + ", servicePrinicipalId="
					+ servicePrinicipalId + ", tenantId=" + tenantId + "]";
		}
		
		
}

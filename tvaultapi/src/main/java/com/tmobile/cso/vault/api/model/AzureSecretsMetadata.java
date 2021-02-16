package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AzureSecretsMetadata implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 4930100961630629120L;
	
	private String secretKeyId;
	private Long expiryDuration;
	
	
	public AzureSecretsMetadata(){
		super();
	}
	
	/**
	 * 
	 * @param secretKeyId
	 * @param expiryDuration
	 */
	public AzureSecretsMetadata(String secretKeyId, Long expiryDuration) {
		super();
		this.secretKeyId = secretKeyId;
		this.expiryDuration = expiryDuration;
	}
	public String getSecretKeyId() {
		return secretKeyId;
	}
	public void setSecretKeyId(String secretKeyId) {
		this.secretKeyId = secretKeyId;
	}
	public Long getExpiryDuration() {
		return expiryDuration;
	}
	public void setExpiryDuration(Long expiryDuration) {
		this.expiryDuration = expiryDuration;
	}
	@Override
	public String toString() {
		return "AzureSecretsMetadata [secretKeyId=" + secretKeyId + ", expiryDuration=" + expiryDuration + "]";
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AzureSecretsMetadata other = (AzureSecretsMetadata) obj;
		if (expiryDuration == null) {
			if (other.expiryDuration != null)
				return false;
		} else if (!expiryDuration.equals(other.expiryDuration))
			return false;
		if (secretKeyId == null) {
			if (other.secretKeyId != null)
				return false;
		} else if (!secretKeyId.equals(other.secretKeyId))
			return false;
		return true;
	}
}

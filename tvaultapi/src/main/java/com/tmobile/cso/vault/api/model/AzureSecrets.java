package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

public class AzureSecrets implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 5577893721805556543L;
	
	@NotNull
	@Min(604800000L)
	@Max(7776000000L)
	private Long expiryDuration;
	
	@NotBlank
	@Size(min = 10, max = 128, message = "SecretKeyId specified should be minimum 10 chanracters and maximum 128 characters only")
	private String secretKeyId;
	
	public AzureSecrets(){
		super();
	}

	public AzureSecrets(Long expiryDuration, String secretKeyId) {
		super();
		this.expiryDuration = expiryDuration;
		this.secretKeyId = secretKeyId;
	}

	public Long getExpiryDuration() {
		return expiryDuration;
	}

	public void setExpiryDuration(Long expiryDuration) {
		this.expiryDuration = expiryDuration;
	}

	public String getSecretKeyId() {
		return secretKeyId;
	}

	public void setSecretKeyId(String secretKeyId) {
		this.secretKeyId = secretKeyId;
	}

	@Override
	public String toString() {
		return "AzureSecrets [expiryDuration=" + expiryDuration + ", secretKeyId=" + secretKeyId + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AzureSecrets other = (AzureSecrets) obj;
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

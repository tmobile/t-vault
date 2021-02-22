package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * OIDCEntityRequest
 */
public class OIDCEntityRequest implements Serializable{
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 4640948642713702095L;
	private String name;
	private Map<String, String> metadata;
	private List<String> policies;
	private Boolean disabled;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public List<String> getPolicies() {
		return policies;
	}
	public void setPolicies(List<String> policies) {
		this.policies = policies;
	}
	public Boolean getDisabled() {
		return disabled;
	}
	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OIDCEntityRequest other = (OIDCEntityRequest) obj;
		if (disabled == null) {
			if (other.disabled != null)
				return false;
		} else if (!disabled.equals(other.disabled))
			return false;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (policies == null) {
			if (other.policies != null)
				return false;
		} else if (!policies.equals(other.policies))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "OIDCEntityRequest [name=" + name + ", metadata=" + metadata + ", policies=" + policies + ", disabled="
				+ disabled + "]";
	}
}

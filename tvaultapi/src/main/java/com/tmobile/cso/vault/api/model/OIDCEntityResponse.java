package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.List;

/**
 * OIDCEntityResponse
 */
public class OIDCEntityResponse implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1861628672544424089L;
	
	private String entityName;
	
	private List<String> policies;

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public List<String> getPolicies() {
		return policies;
	}

	public void setPolicies(List<String> policies) {
		this.policies = policies;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OIDCEntityResponse other = (OIDCEntityResponse) obj;
		if (entityName == null) {
			if (other.entityName != null)
				return false;
		} else if (!entityName.equals(other.entityName))
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
		return "OIDCEntityResponse [entityName=" + entityName + ", policies=" + policies + "]";
	}
		
}

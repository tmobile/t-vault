package com.tmobile.cso.vault.api.model;

import java.util.List;
import java.util.Map;

public class OIDCEntityRequest {
	
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

}

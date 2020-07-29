package com.tmobile.cso.vault.api.model;

import java.util.List;
import java.util.Map;

public class OIDCIdentityGroupRequest {
	
	private String name;
	private String type;
	private Map<String, String> metadata;
	private List<String> policies;
	private List<String> member_group_ids;
	private List<String> member_entity_ids;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
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
	public List<String> getMember_group_ids() {
		return member_group_ids;
	}
	public void setMember_group_ids(List<String> member_group_ids) {
		this.member_group_ids = member_group_ids;
	}
	public List<String> getMember_entity_ids() {
		return member_entity_ids;
	}
	public void setMember_entity_ids(List<String> member_entity_ids) {
		this.member_entity_ids = member_entity_ids;
	}
	
	

}

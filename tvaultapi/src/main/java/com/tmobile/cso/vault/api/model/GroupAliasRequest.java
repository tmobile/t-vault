package com.tmobile.cso.vault.api.model;

public class GroupAliasRequest {
	
	private String name;
	private String id;
	private String mount_accessor;
	private String canonical_id;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMount_accessor() {
		return mount_accessor;
	}
	public void setMount_accessor(String mount_accessor) {
		this.mount_accessor = mount_accessor;
	}
	public String getCanonical_id() {
		return canonical_id;
	}
	public void setCanonical_id(String canonical_id) {
		this.canonical_id = canonical_id;
	}
	
	

}

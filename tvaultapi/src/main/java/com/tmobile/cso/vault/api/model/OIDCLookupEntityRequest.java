package com.tmobile.cso.vault.api.model;

public class OIDCLookupEntityRequest {
	
	
	private String name;
	
	private String id;
	
	private String alias_id;
	
	private String alias_name;
	
	private String alias_mount_accessor;
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
	public String getAlias_id() {
		return alias_id;
	}
	public void setAlias_id(String alias_id) {
		this.alias_id = alias_id;
	}
	public String getAlias_name() {
		return alias_name;
	}
	public void setAlias_name(String alias_name) {
		this.alias_name = alias_name;
	}
	public String getAlias_mount_accessor() {
		return alias_mount_accessor;
	}
	public void setAlias_mount_accessor(String alias_mount_accessor) {
		this.alias_mount_accessor = alias_mount_accessor;
	}
}

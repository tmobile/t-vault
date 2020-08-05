package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

/**
 * OIDCLookupEntityRequest
 */
public class OIDCLookupEntityRequest implements Serializable{
	
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1415874991096885258L;

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
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OIDCLookupEntityRequest other = (OIDCLookupEntityRequest) obj;
		if (alias_id == null) {
			if (other.alias_id != null)
				return false;
		} else if (!alias_id.equals(other.alias_id))
			return false;
		if (alias_mount_accessor == null) {
			if (other.alias_mount_accessor != null)
				return false;
		} else if (!alias_mount_accessor.equals(other.alias_mount_accessor))
			return false;
		if (alias_name == null) {
			if (other.alias_name != null)
				return false;
		} else if (!alias_name.equals(other.alias_name))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "OIDCLookupEntityRequest [name=" + name + ", id=" + id + ", alias_id=" + alias_id + ", alias_name="
				+ alias_name + ", alias_mount_accessor=" + alias_mount_accessor + "]";
	}
	
}

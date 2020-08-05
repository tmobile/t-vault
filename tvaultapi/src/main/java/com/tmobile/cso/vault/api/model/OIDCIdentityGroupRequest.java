package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * OIDCIdentityGroupRequest
 */
public class OIDCIdentityGroupRequest implements Serializable{
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -4558541545000234768L;
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
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OIDCIdentityGroupRequest other = (OIDCIdentityGroupRequest) obj;
		if (member_entity_ids == null) {
			if (other.member_entity_ids != null)
				return false;
		} else if (!member_entity_ids.equals(other.member_entity_ids))
			return false;
		if (member_group_ids == null) {
			if (other.member_group_ids != null)
				return false;
		} else if (!member_group_ids.equals(other.member_group_ids))
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
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "OIDCIdentityGroupRequest [name=" + name + ", type=" + type + ", metadata=" + metadata + ", policies="
				+ policies + ", member_group_ids=" + member_group_ids + ", member_entity_ids=" + member_entity_ids
				+ "]";
	}

}

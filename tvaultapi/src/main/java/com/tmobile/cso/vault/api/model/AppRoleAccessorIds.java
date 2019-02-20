
package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AppRoleAccessorIds implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2762825126992163009L;
	private String role_name;
	private String[] accessorIds;
	
	public AppRoleAccessorIds() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the role_name
	 */
	public String getRole_name() {
		return role_name;
	}


	/**
	 * @param role_name the role_name to set
	 */
	public void setRole_name(String role_name) {
		this.role_name = role_name;
	}


	/**
	 * @return the accessorIds
	 */
	public String[] getAccessorIds() {
		return accessorIds;
	}

	/**
	 * @param accessorIds the accessorIds to set
	 */
	public void setAccessorIds(String[] accessorIds) {
		this.accessorIds = accessorIds;
	}

}

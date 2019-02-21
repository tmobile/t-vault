
package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AppRoleAccessorId implements Serializable{


	/**
	 * 
	 */
	private static final long serialVersionUID = -9177351719913214492L;
	private String role_name;
	private String secret_id_accessor;
	
	public AppRoleAccessorId() {
		// TODO Auto-generated constructor stub
	}

	public AppRoleAccessorId(String role_name, String secret_id_accessor) {
		super();
		this.role_name = role_name;
		this.secret_id_accessor = secret_id_accessor;
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
	 * @return the secret_id_accessor
	 */
	public String getSecret_id_accessor() {
		return secret_id_accessor;
	}

	/**
	 * @param secret_id_accessor the secret_id_accessor to set
	 */
	public void setSecret_id_accessor(String secret_id_accessor) {
		this.secret_id_accessor = secret_id_accessor;
	}

}

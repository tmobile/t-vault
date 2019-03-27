package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class ServiceAccountTTL implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2232306968530527947L;
	private String role_name;
	private String service_account_name;
	private Long ttl;
	public ServiceAccountTTL() {
		
	}
	public ServiceAccountTTL(String role_name, String service_account_name, Long ttl) {
		super();
		this.role_name = role_name;
		this.service_account_name = service_account_name;
		this.ttl = ttl;
	}
	/**
	 * @return the service_account_name
	 */
	public String getService_account_name() {
		return service_account_name;
	}
	/**
	 * @return the ttl
	 */
	public Long getTtl() {
		return ttl;
	}
	/**
	 * @param service_account_name the service_account_name to set
	 */
	public void setService_account_name(String service_account_name) {
		this.service_account_name = service_account_name;
	}
	/**
	 * @param ttl the ttl to set
	 */
	public void setTtl(Long ttl) {
		this.ttl = ttl;
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

	@Override
	public String toString() {
		return "ServiceAccountTTL [role_name=" + role_name + ", service_account_name=" + service_account_name + ", ttl="
				+ ttl + "]";
	}

	
	
}

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AWSClientConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2052708057284340456L;

	public AWSClientConfiguration() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * AWS Access key with permissions to query AWS APIs.
	 */
	private String access_key;
	/**
	 * AWS Secret key with permissions to query AWS APIs.
	 */
	private String secret_key;

	/**
	 * @return the access_key
	 */
	public String getAccess_key() {
		return access_key;
	}
	/**
	 * @return the secret_key
	 */
	public String getSecret_key() {
		return secret_key;
	}
	/**
	 * @param access_key the access_key to set
	 */
	public void setAccess_key(String access_key) {
		this.access_key = access_key;
	}
	/**
	 * @param secret_key the secret_key to set
	 */
	public void setSecret_key(String secret_key) {
		this.secret_key = secret_key;
	}
	
}

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class DirecotryGroupEmail implements Serializable {
	/** Id
	 * 
	 */
	private String id;
	/** Email
	 * 
	 */
	private String email;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	@Override
	public String toString() {
		return "DirecotryGroupEmail [id=" + id + ", email=" + email + "]";
	}
	
	
}

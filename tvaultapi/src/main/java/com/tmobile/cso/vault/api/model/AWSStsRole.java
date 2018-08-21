package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AWSStsRole implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7575730485063336784L;

	public AWSStsRole() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * AWS account ID to be associated with STS role.
	 */
	private String account_id;
	/**
	 * AWS ARN for STS role to be assumed
	 */
	private String sts_role;

	/**
	 * @return the account_id
	 */
	public String getAccount_id() {
		return account_id;
	}
	/**
	 * @return the sts_role
	 */
	public String getSts_role() {
		return sts_role;
	}
	/**
	 * @param account_id the account_id to set
	 */
	public void setAccount_id(String account_id) {
		this.account_id = account_id;
	}
	/**
	 * @param sts_role the sts_role to set
	 */
	public void setSts_role(String sts_role) {
		this.sts_role = sts_role;
	}
	
	
}

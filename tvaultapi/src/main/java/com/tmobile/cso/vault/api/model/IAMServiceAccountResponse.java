package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class IAMServiceAccountResponse implements Serializable{

	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -4323418135392796008L;
	private String userName;
	private String metaDataName;
	private String accountID;
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getMetaDataName() {
		return metaDataName;
	}
	public void setMetaDataName(String metaDataName) {
		this.metaDataName = metaDataName;
	}
	public String getAccountID() {
		return accountID;
	}
	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}
	@Override
	public String toString() {
		return "IAMServiceAccountResponse [userName=" + userName + ", metaDataName=" + metaDataName + ", accountID="
				+ accountID + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accountID == null) ? 0 : accountID.hashCode());
		result = prime * result + ((metaDataName == null) ? 0 : metaDataName.hashCode());
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IAMServiceAccountResponse other = (IAMServiceAccountResponse) obj;
		if (accountID == null) {
			if (other.accountID != null)
				return false;
		} else if (!accountID.equals(other.accountID))
			return false;
		if (metaDataName == null) {
			if (other.metaDataName != null)
				return false;
		} else if (!metaDataName.equals(other.metaDataName))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}
	
	
}
	
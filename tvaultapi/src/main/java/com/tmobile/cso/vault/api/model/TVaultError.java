package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class TVaultError implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4066651543785254000L;
	private String error;
	private String additionalInfo;
	
	public TVaultError()  {
		
	}

	public TVaultError(String error, String additionalInfo) {
		super();
		this.error = error;
		this.additionalInfo = additionalInfo;
	}

	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}

	/**
	 * @return the additionalInfo
	 */
	public String getAdditionalInfo() {
		return additionalInfo;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * @param additionalInfo the additionalInfo to set
	 */
	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LoginError [error=" + error + ", additionalInfo=" + additionalInfo + "]";
	}

}

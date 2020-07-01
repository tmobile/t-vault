package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 
 * @author NNazeer1
 *
 */
public class RevocationRequest implements Serializable{
	
	private static final long serialVersionUID = -2386135857129157386L;
	
	/**
	 * 
	 */
	public RevocationRequest(){	
	}
	
	private String reason;
	@JsonIgnore
	private String time;
	/**
	 * 
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}
	/**
	 * 
	 * @param reason
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}
	/**
	 * 
	 * @return the time
	 */
	public String getTime() {
		return time;
	}
	/**
	 * 
	 * @param time
	 */
	public void setTime(String time) {
		this.time = time;
	}
	
	@Override
	public String toString() {
		return "RevocationRequest{" +
				"reason=" + reason +
				", time=" + time + '\'' +
				'}';
	}

}

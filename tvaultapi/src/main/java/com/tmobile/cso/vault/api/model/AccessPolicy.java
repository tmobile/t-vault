package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

public class AccessPolicy implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2396965762900281226L;

	private String accessid;
	private HashMap<String, String> access;

	
	public AccessPolicy() {
		// TODO Auto-generated constructor stub
	}

	public AccessPolicy(String accessid, HashMap<String, String> access) {
		super();
		this.accessid = accessid;
		this.access = access;
	}

	/**
	 * @return the access
	 */
	@ApiModelProperty(example="{\r\n" + 
			"     \"users/*\":\"read\",\r\n" + 
			"     \"apps/*\":\"read\",\r\n" + 
			"     \"shared/*\":\"read\"\r\n" + 
			"  }", position=1, required=true)
	public HashMap<String, String> getAccess() {
		return access;
	}

	/**
	 * @param access the access to set
	 */
	public void setAccess(HashMap<String, String> access) {
		this.access = access;
	}

	/**
	 * @return the accessid
	 */
	@ApiModelProperty(example="my-test-policy", position=1, required=true)
	public String getAccessid() {
		return accessid;
	}

	/**
	 * @param accessid the accessid to set
	 */
	public void setAccessid(String accessid) {
		this.accessid = accessid;
	}



	
}

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

public class AccessPolicyRules implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5786669847468667873L;

	private String name;
	private String path;
	public AccessPolicyRules() {
	}
	public AccessPolicyRules(String name, String path) {
		super();
		this.name = name;
		this.path = path;
	}
	/**
	 * @return the name
	 */
	@ApiModelProperty(example="my-test-policy", position=1)
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the path
	 */
	@ApiModelProperty(example="\"rules\": \"path \\\"secret/hello*\\\" { capabilities = [\\\"create\\\", \\\"read\\\", \\\"sudo\\\", \\\"update\\\"] }\"", position=2)
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

}

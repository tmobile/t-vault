package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

public class CertificateGroup implements Serializable{

	
	@NotNull
	private String certificatename;
	@NotNull
	private String groupname;
	@NotNull
	private String access;
	
	public CertificateGroup() {
		super();
	}

	/**
	 * @param certificatename
	 * @param groupname
	 * @param access
	 */

	public CertificateGroup(String certificatename, String groupname, String access) {
		super();
		this.certificatename = certificatename;
		this.groupname = groupname;
		this.access = access;
	}

	@ApiModelProperty(example="cert1", position=1)
	public String getCertificatename() {
		return certificatename;
	}

	public void setCertificatename(String certificatename) {
		this.certificatename = certificatename;
	}
	
	@ApiModelProperty(example="r_vault_demo", position=2)
	public String getGroupname() {
		return groupname;
	}

	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	public String getAccess() {
		return access;
	}
	@ApiModelProperty(example="read", position=3)
	public void setAccess(String access) {
		this.access = access;
	}
	
	
	
	
	
}

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

public class CertificateGroup implements Serializable{

	private static final long serialVersionUID = 4382873160455605317L;
	@NotNull
	private String certificateName;
	@NotNull
	private String groupname;
	@NotNull
	private String access;
	@NotNull
	private String certType;
	
	public CertificateGroup() {
		super();
	}

	/**
	 * @param certificateName
	 * @param groupname
	 * @param access
	 */
	
	
	public CertificateGroup(String certificateName, String groupname, String access, String certType) {
		super();
		this.certificateName = certificateName;
		this.groupname = groupname;
		this.access = access;
		this.certType = certType;
	}

	
	
	@ApiModelProperty(example="cert1", position=1)
	public String getCertificateName() {
		return certificateName;
	}

	public void setCertificateName(String certificateName) {
		this.certificateName = certificateName;
	}

	@ApiModelProperty(example="r_vault_demo", position=2)
	public String getGroupname() {
		return groupname;
	}

	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	@ApiModelProperty(example="read", position=3)
	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}

	@ApiModelProperty(example="internal", position=4)
	public String getCertType() {
		return certType;
	}

	public void setCertType(String certType) {
		this.certType = certType;
	}
}

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

public class AWSLogin implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1850106471444613271L;
	private String role;
	private String pkcs7;
	//private String nonce;
	
	public AWSLogin() {
		// TODO Auto-generated constructor stub
	}

	public AWSLogin(String role, String pkcs7/*, String nonce*/) {
		super();
		this.role = role;
		this.pkcs7 = pkcs7;
//		this.nonce = nonce;
	}

	/**
	 * @return the role
	 */
	@ApiModelProperty(example="testawsrole", position=1)
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @return the pkcs7
	 */
	@ApiModelProperty(example="MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====", position=2)
	public String getPkcs7() {
		return pkcs7;
	}

	/**
	 * @param pkcs7 the pkcs7 to set
	 */
	public void setPkcs7(String pkcs7) {
		this.pkcs7 = pkcs7;
	}

//	/**
//	 * @return the nonce
//	 */
//	@ApiModelProperty(example="MIAGCSqGSIb3DQEHAqCAMIACAQExCzAJBgUrDgMCGgUAMIAGC", position=3)
//	public String getNonce() {
//		return nonce;
//	}
//
//	/**
//	 * @param nonce the nonce to set
//	 */
//	public void setNonce(String nonce) {
//		this.nonce = nonce;
//	}

}

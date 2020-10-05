package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModelProperty;

public class IAMServiceAccountApprole implements Serializable{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -3124680126093146363L;
	@NotNull
	private String iamSvcAccName;
	@NotNull
	private String approlename;
	@NotNull
	private String access;
	
	@NotNull
	@Pattern(regexp = "^$|^[0-9]+$", message = "Invalid AWS account id")
	private String awsAccountId;
	
	public IAMServiceAccountApprole() {
		super();
	}


	/**
	 * @param iamsvcAccName
	 * @param approlename
	 * @param access
	 * @param awsAccountId
	 */
	public IAMServiceAccountApprole(String iamSvcAccName, String approlename, String access, String awsAccountId) {
		super();
		this.iamSvcAccName = iamSvcAccName;
		this.approlename = approlename;
		this.access = access;
		this.awsAccountId = awsAccountId;
	}

	/**
	 * @return the groupname
	 */
	@ApiModelProperty(example="role1", position=2)
	public String getApprolename() {
		return approlename;
	}


	/**
	 * @param approlename the username to set
	 */
	public void setApprolename(String approlename) {
		this.approlename = approlename;
	}


	/**
	 * @return the access
	 */
	@ApiModelProperty(example="read", position=3, allowableValues="read,write,deny,owner")
	public String getAccess() {
		return access;
	}


	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

	@ApiModelProperty(example="iam_svc_vault_test2", position=1)
	public String getIamSvcAccName() {
		return iamSvcAccName;
	}


	public void setIamSvcAccName(String iamSvcAccName) {
		this.iamSvcAccName = iamSvcAccName;
	}


	public String getAwsAccountId() {
		return awsAccountId;
	}


	public void setAwsAccountId(String awsAccountId) {
		this.awsAccountId = awsAccountId;
	}


	@Override
	public String toString() {
		return "IAMServiceAccountApprole [iamSvcAccName=" + iamSvcAccName + ", approlename=" + approlename + ", access="
				+ access + ", awsAccountId=" + awsAccountId + "]";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((access == null) ? 0 : access.hashCode());
		result = prime * result + ((approlename == null) ? 0 : approlename.hashCode());
		result = prime * result + ((awsAccountId == null) ? 0 : awsAccountId.hashCode());
		result = prime * result + ((iamSvcAccName == null) ? 0 : iamSvcAccName.hashCode());
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
		IAMServiceAccountApprole other = (IAMServiceAccountApprole) obj;
		if (access == null) {
			if (other.access != null)
				return false;
		} else if (!access.equals(other.access))
			return false;
		if (approlename == null) {
			if (other.approlename != null)
				return false;
		} else if (!approlename.equals(other.approlename))
			return false;
		if (awsAccountId == null) {
			if (other.awsAccountId != null)
				return false;
		} else if (!awsAccountId.equals(other.awsAccountId))
			return false;
		if (iamSvcAccName == null) {
			if (other.iamSvcAccName != null)
				return false;
		} else if (!iamSvcAccName.equals(other.iamSvcAccName))
			return false;
		return true;
	}
}

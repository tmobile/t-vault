/**
 * 
 */
package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import io.swagger.annotations.ApiModelProperty;


public class IAMServiceAccountAWSRole implements Serializable{


	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 6673811289922222609L;
	
	public IAMServiceAccountAWSRole(){
		super();
	}

	/**
	 * 
	 * @param iamSvcAccName
	 * @param rolename
	 * @param access
	 * @param awsAccountId
	 */
	public IAMServiceAccountAWSRole(String iamSvcAccName, String rolename, String access, String awsAccountId) {
		super();
		this.iamSvcAccName = iamSvcAccName;
		this.rolename = rolename;
		this.access = access;
		this.awsAccountId = awsAccountId;
	}

	@NotBlank
	@Size(min = 11, max = 30, message = "IAM SvcAccName specified should be minimum 11 characters and maximum 30 characters only")
	@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Name can have alphabets, numbers, _ and - characters only")
	private String iamSvcAccName;

	@NotBlank
	@Size(min = 1, message = "Role name can not be null or empty")
	private String rolename;

	@NotBlank
	@Size(min = 1, message = "Access can not be null or empty")
	private String access;

	@NotBlank
	@Pattern(regexp = "^$|^[0-9]+$", message = "Invalid AWS account id")
	private String awsAccountId;

	/**
	 * @return the iamSvcAccName
	 */
	@ApiModelProperty(example = "123123_svc_vlt_test2", position = 1)
	public String getIamSvcAccName() {
		return iamSvcAccName.toLowerCase();
	}

	/**
	 * @param iamSvcAccName the iamSvcAccName to set
	 */
	public void setIamSvcAccName(String iamSvcAccName) {
		this.iamSvcAccName = iamSvcAccName;
	}
	
	/**
	 * @return the role name
	 */
	@ApiModelProperty(example = "role1", position = 2)
	public String getRolename() {
		return rolename;
	}

	public void setRolename(String rolename) {
		this.rolename = rolename;
	}

	/**
	 * @return the access
	 */
	@ApiModelProperty(example = "read", position = 3, allowableValues = "read,reset,deny,owner")
	public String getAccess() {
		return access.toLowerCase();
	}

	/**
	 * @param access the access to set
	 */
	public void setAccess(String access) {
		this.access = access;
	}

	/**
	 * @return the awsAccountId
	 */
	@ApiModelProperty(example = "123456789012", position = 4)
	public String getAwsAccountId() {
		return awsAccountId;
	}

	/**
	 * @param awsAccountId the awsAccountId to set
	 */
	public void setAwsAccountId(String awsAccountId) {
		this.awsAccountId = awsAccountId;
	}

	@Override
	public String toString() {
		return "IAMServiceAccountAWSRole [iamSvcAccName=" + iamSvcAccName + ", rolename=" + rolename + ", access="
				+ access + ", awsAccountId=" + awsAccountId + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IAMServiceAccountAWSRole other = (IAMServiceAccountAWSRole) obj;
		if (access == null) {
			if (other.access != null)
				return false;
		} else if (!access.equals(other.access))
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
		if (rolename == null) {
			if (other.rolename != null)
				return false;
		} else if (!rolename.equals(other.rolename))
			return false;
		return true;
	}

}

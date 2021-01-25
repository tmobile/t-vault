package com.tmobile.cso.vault.api.model;

public class AWSIAMMetadataDetails {

	private String auth_type;
	private String[] bound_ami_id;
	private String role;
	private String[] policies;
	private String[] bound_iam_principal_arn;
	private String[] bound_iam_role_arn;
	private Integer max_ttl;
	private boolean disallow_reauthentication;
	private boolean allow_instance_migration;
	private boolean resolve_aws_unique_ids;
	
	public AWSIAMMetadataDetails() {
		super();
	}

	public AWSIAMMetadataDetails(String auth_type, String[] bound_ami_id, String role, String[] policies,
			String[] bound_iam_principal_arn, String[] bound_iam_role_arn, Integer max_ttl,
			boolean disallow_reauthentication, boolean allow_instance_migration, boolean resolve_aws_unique_ids) {
		super();
		this.auth_type = auth_type;
		this.bound_ami_id = bound_ami_id;
		this.role = role;
		this.policies = policies;
		this.bound_iam_principal_arn = bound_iam_principal_arn;
		this.bound_iam_role_arn = bound_iam_role_arn;
		this.max_ttl = max_ttl;
		this.disallow_reauthentication = disallow_reauthentication;
		this.allow_instance_migration = allow_instance_migration;
		this.resolve_aws_unique_ids = resolve_aws_unique_ids;
	}

	public String getAuth_type() {
		return auth_type;
	}

	public void setAuth_type(String auth_type) {
		this.auth_type = auth_type;
	}

	public String[] getBound_ami_id() {
		return bound_ami_id;
	}

	public void setBound_ami_id(String[] bound_ami_id) {
		this.bound_ami_id = bound_ami_id;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String[] getPolicies() {
		return policies;
	}

	public void setPolicies(String[] policies) {
		this.policies = policies;
	}

	public String[] getBound_iam_principal_arn() {
		return bound_iam_principal_arn;
	}

	public void setBound_iam_principal_arn(String[] bound_iam_principal_arn) {
		this.bound_iam_principal_arn = bound_iam_principal_arn;
	}

	public String[] getBound_iam_role_arn() {
		return bound_iam_role_arn;
	}

	public void setBound_iam_role_arn(String[] bound_iam_role_arn) {
		this.bound_iam_role_arn = bound_iam_role_arn;
	}

	public Integer getMax_ttl() {
		return max_ttl;
	}

	public void setMax_ttl(Integer max_ttl) {
		this.max_ttl = max_ttl;
	}

	public boolean isDisallow_reauthentication() {
		return disallow_reauthentication;
	}

	public void setDisallow_reauthentication(boolean disallow_reauthentication) {
		this.disallow_reauthentication = disallow_reauthentication;
	}

	public boolean isAllow_instance_migration() {
		return allow_instance_migration;
	}

	public void setAllow_instance_migration(boolean allow_instance_migration) {
		this.allow_instance_migration = allow_instance_migration;
	}

	public boolean isResolve_aws_unique_ids() {
		return resolve_aws_unique_ids;
	}

	public void setResolve_aws_unique_ids(boolean resolve_aws_unique_ids) {
		this.resolve_aws_unique_ids = resolve_aws_unique_ids;
	}
	
	
	
}

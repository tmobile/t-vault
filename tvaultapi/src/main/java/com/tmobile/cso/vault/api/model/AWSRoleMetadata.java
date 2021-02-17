package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AWSRoleMetadata implements Serializable{
	private String path;
    /**
     * AWSMetadataDetails details
     */
    @JsonProperty("data")
    private AWSMetadataDetails awsRoleMetadataDetails;
    
	public AWSRoleMetadata() {
		super();
	}

	public AWSRoleMetadata(String path, AWSMetadataDetails awsRoleMetadataDetails) {
		super();
		this.path = path;
		this.awsRoleMetadataDetails = awsRoleMetadataDetails;
	}
	/**
     * @return the path
     */
	public String getPath() {
		return path;
	}
	/**
     * @param path the path to set
     */
	public void setPath(String path) {
		this.path = path;
	}
	/**
     * @return the awsRoleMetadataDetails
     */
	public AWSMetadataDetails getAwsRoleMetadataDetails() {
		return awsRoleMetadataDetails;
	}
	 /**
     * @param awsRoleMetadataDetails the awsRoleMetadataDetails to set
     */
	public void setAwsRoleMetadataDetails(AWSMetadataDetails awsRoleMetadataDetails) {
		this.awsRoleMetadataDetails = awsRoleMetadataDetails;
	}
    
}

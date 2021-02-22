package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AWSMetadataDetails implements Serializable{
	 /**
    *
    */
	private String name;

    private String createdBy;
    
    private String type;
    /**
    *
    */
    public AWSMetadataDetails() {
		super();
	}
	/**
    *
    */
	public AWSMetadataDetails(String name, String createdBy, String type) {
		super();
		this.name = name;
		this.createdBy = createdBy;
		this.type = type;
	}
	/**
     * @return the name
     */
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
     * @return the createdBy
     */
	public String getCreatedBy() {
		return createdBy;
	}
	 /**
     * @param name the createdBy to set
     */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	/**
     * @return the type
     */
	public String getType() {
		return type;
	}
	/**
     * @param name the type to set
     */
	public void setType(String type) {
		this.type = type;
	}
}

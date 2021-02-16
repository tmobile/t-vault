package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.List;

public class AzureServiceAccountNode implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 634537093213616398L;

	private List<String> folders;

	private String path;

	private String servicePrincipalName;

	public List<String> getFolders() {
		return folders;
	}

	public void setFolders(List<String> folders) {
		this.folders = folders;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getServicePrincipalName() {
		return servicePrincipalName;
	}

	public void setServicePrincipalName(String servicePrincipalName) {
		this.servicePrincipalName = servicePrincipalName;
	}

	@Override
	public String toString() {
		return "AzureServiceAccountNode [folders=" + folders + ", path=" + path + ", servicePrincipalName="
				+ servicePrincipalName + "]";
	}
	
	
	

}

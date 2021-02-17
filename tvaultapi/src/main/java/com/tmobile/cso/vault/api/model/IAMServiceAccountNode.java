package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * IAMServiceAccountNode
 */
public class IAMServiceAccountNode implements Serializable{
	
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 6091750877139888371L;
	
	private List<String> folders;
	
	private String path;
	
	private String iamsvcaccName;

	public List<String> getFolders() {
		return folders;
	}

	public void setFolders(List<String> folders) {
		this.folders = folders;
	}

	public String getIamsvcaccName() {
		return iamsvcaccName;
	}

	public void setIamsvcaccName(String iamsvcaccName) {
		this.iamsvcaccName = iamsvcaccName;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return "IAMServiceAccountNode [folders=" + folders + ", path=" + path + ", iamsvcaccName=" + iamsvcaccName
				+ "]";
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IAMServiceAccountNode other = (IAMServiceAccountNode) obj;
		if (folders == null) {
			if (other.folders != null)
				return false;
		} 	else if (!folders.equals(other.folders))
				return false;
		if (iamsvcaccName == null) {
			if (other.iamsvcaccName != null)
				return false;
		} 	else if (!iamsvcaccName.equals(other.iamsvcaccName))
				return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} 	else if (!path.equals(other.path))
				return false;
		return true;
	}
	
}

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
	
	private String path;
	
	private String value;
	
	private String type;
	
	private List<String> folders;
	
	private String iamsvcaccName;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

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

	@Override
	public String toString() {
		return "IAMServiceAccountNode [path=" + path + ", value=" + value + ", type=" + type + ", folders=" + folders
				+ ", iamsvcaccName=" + iamsvcaccName + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((folders == null) ? 0 : folders.hashCode());
		result = prime * result + ((iamsvcaccName == null) ? 0 : iamsvcaccName.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		IAMServiceAccountNode other = (IAMServiceAccountNode) obj;
		if (folders == null) {
			if (other.folders != null)
				return false;
		} else if (!folders.equals(other.folders))
			return false;
		if (iamsvcaccName == null) {
			if (other.iamsvcaccName != null)
				return false;
		} else if (!iamsvcaccName.equals(other.iamsvcaccName))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	
	
}

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class DirectoryObjects implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4433531527128247538L;
	private DirectoryObjectsList data;

	public DirectoryObjects() {
		// TODO Auto-generated constructor stub
	}


	/**
	 * @return the data
	 */
	public DirectoryObjectsList getData() {
		return data;
	}


	/**
	 * @param data the data to set
	 */
	public void setData(DirectoryObjectsList data) {
		this.data = data;
	}



}

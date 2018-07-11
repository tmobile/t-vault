package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class DirectoryObjectsList implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1679844807263329466L;
	private Object[] values;
	
	public DirectoryObjectsList() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the values
	 */
	public Object[] getValues() {
		return values;
	}

	/**
	 * @param values the values to set
	 */
	public void setValues(Object[] values) {
		this.values = values;
	}

}

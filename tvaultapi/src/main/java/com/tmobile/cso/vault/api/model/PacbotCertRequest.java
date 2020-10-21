package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class PacbotCertRequest implements Serializable {

	private static final long serialVersionUID = 8693508595218686587L;
	private String ag;
	private FilterDetails filter;
	private int from;
	private int size;
    
	public String getAg() {
		return ag;
	}
	public void setAg(String ag) {
		this.ag = ag;
	}
	
	public FilterDetails getFilterDetails() {
		return filter;
	}
	public void setFilterDetails(FilterDetails filter) {
		this.filter = filter;
	}
	public int getFrom() {
		return from;
	}
	public void setFrom(int from) {
		this.from = from;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	@Override
	public String toString() {
		return "PacbotCertRequest [ag=" + ag + ", filter=" + filter + ", from=" + from + ", size=" + size
				+ "]";
	}


}

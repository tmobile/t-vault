package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class AWSIAMLogin implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7218951490321236578L;

	

	private String iam_http_request_method;
	private String iam_request_url;
	private String iam_request_body;
	private String iam_request_headers;
	private String role;
	
	
	public AWSIAMLogin() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @return the iam_http_request_method
	 */
	public String getIam_http_request_method() {
		return iam_http_request_method;
	}


	/**
	 * @return the iam_request_url
	 */
	public String getIam_request_url() {
		return iam_request_url;
	}


	/**
	 * @return the iam_request_body
	 */
	public String getIam_request_body() {
		return iam_request_body;
	}


	/**
	 * @return the iam_request_headers
	 */
	public String getIam_request_headers() {
		return iam_request_headers;
	}


	/**
	 * @param iam_http_request_method the iam_http_request_method to set
	 */
	public void setIam_http_request_method(String iam_http_request_method) {
		this.iam_http_request_method = iam_http_request_method;
	}


	/**
	 * @param iam_request_url the iam_request_url to set
	 */
	public void setIam_request_url(String iam_request_url) {
		this.iam_request_url = iam_request_url;
	}


	/**
	 * @param iam_request_body the iam_request_body to set
	 */
	public void setIam_request_body(String iam_request_body) {
		this.iam_request_body = iam_request_body;
	}


	/**
	 * @param iam_request_headers the iam_request_headers to set
	 */
	public void setIam_request_headers(String iam_request_headers) {
		this.iam_request_headers = iam_request_headers;
	}

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}



}

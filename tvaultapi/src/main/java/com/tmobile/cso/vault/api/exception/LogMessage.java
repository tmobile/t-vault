package com.tmobile.cso.vault.api.exception;

import java.io.Serializable;
import java.util.Map;

public class LogMessage implements Serializable {

	public static final String TRACEID="traceid";
	public static final String DATETIME="date";
	public static final String USER="user";
	public static final String ACTION="action";
	public static final String RESPONSE="response";
	public static final String STATUS="httpstatus";
	public static final String MESSAGE="message";
	public static final String STACKTRACE="stacktrace";
	public static final String APIURL="apiurl";

	/**
	 * 
	 */
	private static final long serialVersionUID = 7117086432693787101L;
	private Map<String,Object> messages;
	
	public LogMessage() {
		// TODO Auto-generated constructor stub
	}

	public LogMessage(Map<String, Object> messages) {
		super();
		this.messages = messages;
	}

	/**
	 * @return the messages
	 */
	public Map<String, Object> getMessages() {
		return messages;
	}

	/**
	 * @param messages the messages to set
	 */
	public void setMessages(Map<String, Object> messages) {
		this.messages = messages;
	}

}

// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

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

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
package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

public class Unseal implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8123752328060724218L;
	/**
	 * Server IP Address
	 */
	private String serverip;
	/**
	 * Server port
	 */
	private String port;
	/**
	 * Vault unseal Key
	 */
	private String key;
	/**
	 * reset or not
	 */
	private boolean reset;
	
	public Unseal() {
		
	}

	public Unseal(String serverip, String port, String key, boolean reset) {
		super();
		this.serverip = serverip;
		this.port = port;
		this.key = key;
		this.reset = reset;
	}

	/**
	 * @return the serverip
	 */
	@ApiModelProperty(example="localhost", position=1, required=true)
	public String getServerip() {
		return serverip;
	}

	/**
	 * @param serverip the serverip to set
	 */
	public void setServerip(String serverip) {
		this.serverip = serverip;
	}

	/**
	 * @return the port
	 */
	@ApiModelProperty(example="8200", position=2, required=true)
	public String getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @return the key
	 */
	@ApiModelProperty(example="LwRw+j0I7L4ff4no50fduaF5OeTZJEOQFwdjWZcee0s=", position=3, required=true)
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return the reset
	 */
	@ApiModelProperty(example="true", position=4)
	public boolean isReset() {
		return reset;
	}

	/**
	 * @param reset the reset to set
	 */
	public void setReset(boolean reset) {
		this.reset = reset;
	}

}

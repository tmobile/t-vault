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

package com.tmobile.cso.vault.api.service;

import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSecretService {

	@Autowired
	private RequestProcessor reqProcessor;

	/**
	 * Read MySql database temporary credentials.
	 * @param role_name
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getTemporaryCredentials(String role_name, String token) {
		Response response = reqProcessor.process("/database/creds/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
}

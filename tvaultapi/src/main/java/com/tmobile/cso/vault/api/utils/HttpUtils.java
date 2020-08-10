// =========================================================================
// Copyright 2020 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License")
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

package com.tmobile.cso.vault.api.utils;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Component;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
public class HttpUtils {

	private Logger log = LogManager.getLogger(HttpUtils.class);

	public HttpUtils() {
		// Auto-generated constructor stub
	}

	/**
	 * To get HttpClinet for AAD rest api calls.
	 * @return
	 */
	public HttpClient getHttpClient() {
		HttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
					NoopHostnameVerifier.INSTANCE).
					setSSLContext(
							new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
								@Override
								public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
									return true;
								}
							}).build()
					).setRedirectStrategy(new LaxRedirectStrategy()).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "getHttpClient").
					put(LogMessage.MESSAGE, "Failed to initialize httpClient").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return httpClient;
	}
}

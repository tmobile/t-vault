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

package com.tmobile.cso.vault.api.process;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.CertManagerLoginRequest;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@Component()
public class CertRestProcessor {
	private static Logger log = LogManager.getLogger(CertRestProcessor.class);
	
//	@Value("${vault.api.url}")
//	private String vaultApiUrl;

	@Value("${vault.ssl.verify:true}")
	private boolean sslVerify;
	
	public CertRestProcessor(){
		
	}
	
	public ResponseEntity<String> post(String endpoint,String token,Object payload ){

		RestTemplate restTemplate = getRestTemplate(sslVerify, token);
		ResponseEntity<String> response;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Invoke Vault API").
			      put(LogMessage.MESSAGE, String.format("Calling the Cert end point [%s] using POST method", endpoint)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		try{
			response= restTemplate.postForEntity(endpoint,payload,String.class);
		}catch(HttpStatusCodeException e){
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}catch(RestClientException e){
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
		return response;
		
	}
	public ResponseEntity<String> put(String endpoint,String token,Object payload ){

		RestTemplate restTemplate = getRestTemplate(sslVerify, token);
		ResponseEntity<String> response;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Invoke Vault API").
				put(LogMessage.MESSAGE, String.format("Calling the Cert end point [%s] using POST method", endpoint)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		try{
			response= restTemplate.exchange(endpoint,HttpMethod.PUT, new HttpEntity<>(payload),String.class);
		}catch(HttpStatusCodeException e){
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}catch(RestClientException e){
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
		return response;

	}



	public ResponseEntity<String> get(String endpoint,String token){
		RestTemplate restTemplate = getRestTemplate(sslVerify, token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Invoke Vault API").
			      put(LogMessage.MESSAGE, String.format("Calling the Cert end point [%s] using GET method", endpoint)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		ResponseEntity<String> response;
		try{
			response= restTemplate.getForEntity(endpoint,String.class);
		}catch(HttpStatusCodeException e){
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}catch(RestClientException e){
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
		return response;
	}
	public ResponseEntity<String> delete(String endpoint,String token){
		RestTemplate restTemplate = getRestTemplate(sslVerify, token);
		ResponseEntity<String> response = null;
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				  put(LogMessage.ACTION, "Invoke Vault API").
			      put(LogMessage.MESSAGE, String.format("Calling the Cert end point [%s] using DELETE method", endpoint)).
			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
			      build()));
		
		try{
			response = restTemplate.exchange(endpoint,HttpMethod.DELETE,null,String.class);
		}catch(HttpStatusCodeException e){
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}catch(RestClientException e){
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}catch(Exception e){
			log.debug(e.getMessage());
		}
		
		return response;
	}
	
	private static RestTemplate getRestTemplate(boolean sslVerify,String token){
		
		HttpClient httpClient =null;
		try {
			if(!sslVerify)
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
			else
				httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
				
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
			log.debug(e1.getMessage());
		}
		
		RestTemplate restTemplate  = new RestTemplate();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setHttpClient(httpClient);
		restTemplate.setRequestFactory(factory);
		HttpCertRequestInterceptor interceptor = new HttpCertRequestInterceptor(token);
		restTemplate.getInterceptors().add(interceptor);
		return restTemplate;
	}
}

class HttpCertRequestInterceptor implements ClientHttpRequestInterceptor {
	  private String token;
	  public HttpCertRequestInterceptor(String token){
		  this.token= token;
	  }
	  @Override
	  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
	      ClientHttpRequestExecution execution) throws IOException {
	    HttpRequestWrapper requestWrapper = new HttpRequestWrapper(request);
	    requestWrapper.getHeaders().add("Authorization", "Bearer " + token);
	    return execution.execute(requestWrapper, body);
	  }
}




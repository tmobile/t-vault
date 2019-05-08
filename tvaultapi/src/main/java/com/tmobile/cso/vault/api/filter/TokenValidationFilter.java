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

package com.tmobile.cso.vault.api.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.GenericFilterBean;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.model.VaultTokenLookupDetails;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;
import com.tmobile.cso.vault.api.validator.TokenValidator;


public class TokenValidationFilter extends GenericFilterBean {

	private static Logger log = LogManager.getLogger(TokenValidationFilter.class);

	@Autowired
	private TokenValidator tokenValidator;
	
	@Autowired
	private TokenUtils tokenUtils;

	@Value("${selfservice.enable:true}")
	private boolean isSelfServiceEnabled;

	@Value("${ad.passwordrotation.enable:true}")
	private boolean isAdPswdRotationEnabled;

	public TokenValidationFilter() {
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String clientToken = ((HttpServletRequest) request).getParameter("vault-token");
		if (clientToken == null) {
			clientToken = ((HttpServletRequest) request).getHeader("vault-token");
		}
		String requestUri = ((HttpServletRequest) request).getRequestURI();
		// ignore the client token in the login request header
		if (requestUri.equals("/vault/v2/auth/tvault/login")) {
			clientToken= null;
		}
		// Skip the request if requested feature is disabled
		if (!isSelfServiceEnabled && requestUri.startsWith("/vault/v2/ss/")) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.setContentType("application/json");
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't perform the required operation. Self Service feature is disabled");
			return;
		}
		if (!isAdPswdRotationEnabled && (requestUri.startsWith("/vault/v2/ad") || requestUri.startsWith("/vault/v2/serviceaccounts"))) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.setContentType("application/json");
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't perform the required operation. Service Account feature is disabled");
			return;
		}
		VaultTokenLookupDetails  vaultTokenLookupDetails = null;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "TokenValidationFilter - doFilter").
				put(LogMessage.MESSAGE, String.format ("Validating token")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (!StringUtils.isEmpty(clientToken) && !"null".equalsIgnoreCase(clientToken)) {
			try {
				vaultTokenLookupDetails = tokenValidator.getVaultTokenLookupDetails(clientToken);
			} catch (TVaultValidationException e) {
				HttpServletResponse httpResponse = (HttpServletResponse) response;
				httpResponse.setContentType("application/json");
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				return;
			}
		}
		UserDetails userDetails = null;
		if (vaultTokenLookupDetails != null && vaultTokenLookupDetails.isValid()) {
			userDetails = new UserDetails();
			userDetails.setClientToken(clientToken);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "TokenValidationFilter - doFilter").
					put(LogMessage.MESSAGE, String.format ("Generating SelfService token for the user [%s]", vaultTokenLookupDetails.getUsername())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			if (!vaultTokenLookupDetails.isAdmin()) {
				userDetails.setSelfSupportToken(tokenUtils.getSelfServiceToken());
			}
			userDetails.setPolicies(vaultTokenLookupDetails.getPolicies());
			userDetails.setAdmin(vaultTokenLookupDetails.isAdmin());
			userDetails.setSudoPolicies(null); //TODO: Pre-flight
			userDetails.setUsername(vaultTokenLookupDetails.getUsername());
			userDetails.setLeaseDuration(null); //TODO: Pre-flight
			((HttpServletRequest) request).setAttribute("UserDetails", userDetails);
		}
		chain.doFilter(request, response);
		if (userDetails != null && userDetails.getSelfSupportToken() != null) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "TokenValidationFilter - doFilter").
					put(LogMessage.MESSAGE, String.format ("Revoking SelfService token from the user [%s]", vaultTokenLookupDetails.getUsername())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			tokenUtils.revokePowerToken(userDetails.getSelfSupportToken());
		}
	}


}

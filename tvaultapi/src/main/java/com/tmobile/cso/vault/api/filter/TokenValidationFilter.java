package com.tmobile.cso.vault.api.filter;

import java.io.IOException;
import java.util.Enumeration;

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
import org.springframework.web.filter.GenericFilterBean;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.validator.TokenValidator;


public class TokenValidationFilter extends GenericFilterBean {

	private static Logger log = LogManager.getLogger(TokenValidationFilter.class);

	@Autowired
	private TokenValidator tokenValidator;

	public TokenValidationFilter() {
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String clientToken = ((HttpServletRequest) request).getParameter("vault-token");
		if (clientToken == null) {
			clientToken = ((HttpServletRequest) request).getHeader("vault-token");
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "TokenValidationFilter - doFilter").
				put(LogMessage.MESSAGE, String.format ("Validating token")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if (!StringUtils.isEmpty(clientToken) && !"null".equalsIgnoreCase(clientToken)) {
			try {
				tokenValidator.getVaultTokenLookupDetails(clientToken);
			} catch (TVaultValidationException e) {
				HttpServletResponse httpResponse = (HttpServletResponse) response;
				httpResponse.setContentType("application/json");
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				return;
			}
		}	
		chain.doFilter(request, response);
	}


}

/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */
package com.tmobile.cso.vault.api.filter;

import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tmobile.cso.vault.api.config.Slf4jMDCFilterConfiguration;

@Component
public class Slf4jMDCFilter extends OncePerRequestFilter {

    private final String responseHeader;
    private final String mdcTokenKey;
    private final String requestHeader;

    public Slf4jMDCFilter() {
        responseHeader = Slf4jMDCFilterConfiguration.DEFAULT_RESPONSE_TOKEN_HEADER;
        mdcTokenKey = Slf4jMDCFilterConfiguration.DEFAULT_MDC_UUID_TOKEN_KEY;
        requestHeader = null;
    }

    public Slf4jMDCFilter(final String responseHeader, final String mdcTokenKey, final String requestHeader) {
        this.responseHeader = responseHeader;
        this.mdcTokenKey = mdcTokenKey;
        this.requestHeader = requestHeader;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws java.io.IOException, ServletException {
        try {
            final String token = extractToken(request);
            MDC.put(mdcTokenKey, token);
            if (!StringUtils.isEmpty(responseHeader)) {
                response.addHeader(responseHeader, token);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(mdcTokenKey);
        }
    }

    private String extractToken(final HttpServletRequest request) {
        final String token;
        if (!StringUtils.isEmpty(requestHeader) && !StringUtils.isEmpty(request.getHeader(requestHeader))) {
            token = request.getHeader(requestHeader);
        } else {
            token = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        }
        return token;
    }

    @Override
    protected boolean isAsyncDispatch(final HttpServletRequest request) {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

	@Override
	public String toString() {
		return "Slf4jMDCFilter [responseHeader=" + responseHeader + ", mdcTokenKey=" + mdcTokenKey + ", requestHeader="
				+ requestHeader + "]";
	}    
}
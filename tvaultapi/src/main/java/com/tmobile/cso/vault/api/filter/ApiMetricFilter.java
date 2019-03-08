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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

public class ApiMetricFilter implements Filter {
	
	private static Logger auditlog = LogManager.getLogger("metric-logger");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
      throws java.io.IOException, ServletException {
    	
    	ThreadLocalContext.getCurrentMap().put(LogMessage.APIURL, ((HttpServletRequest) request).getRequestURL().toString());
    	ThreadLocalContext.getCurrentMap().put(LogMessage.USER, "");
 
    	
    	Map<String,Object> metricmap = new LinkedHashMap<String,Object>();
    	metricmap.put("invokedat", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        HttpServletRequest httpRequest = ((HttpServletRequest) request);
        String reqURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        long startTime= System.currentTimeMillis();
        chain.doFilter(request, response);
        long endTime= System.currentTimeMillis();
        int status = ((HttpServletResponse) response).getStatus();
        metricmap.put("uri", reqURI);
        metricmap.put("method",method);
        metricmap.put("status",status);
        metricmap.put("timetaken", endTime-startTime);
        ObjectMapper objMapper = new ObjectMapper();
        if(!reqURI.equalsIgnoreCase("/vault/health"))
        	auditlog.info(objMapper.writeValueAsString(metricmap));
    }

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
}
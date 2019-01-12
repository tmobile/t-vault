// =========================================================================
// Copyright 2018 T-Mobile, US
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

package com.tmobile.cso.vault.api.utils;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.apache.logging.log4j.util.StringMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;

@Plugin(name = "InjectMarkerPolicy", category = "Core",
elementType = "rewritePolicy", printObject = true)
public final class LogMessageUpdater implements RewritePolicy {

	@Override
	public LogEvent rewrite(final LogEvent event) {
		Log4jLogEvent.Builder builder = new Log4jLogEvent.Builder();
		if (event.getMarker() != null) {
			StringMap contextData = ContextDataFactory.createContextData();
			contextData.putValue("_marker", event.getMarker().getName());
			builder.setContextData(contextData);
		}
		builder.setLoggerName(event.getLoggerName());
		builder.setMarker(event.getMarker());
		builder.setLoggerFqcn(event.getLoggerFqcn());
		builder.setLevel(event.getLevel());
		builder.setMessage(event.getMessage());
		String message = event.getMessage().getFormattedMessage();
		if(!isValidJson(message)){
			builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(JSONUtil.getJSON(ImmutableMap.<String, String>builder().put(LogMessage.MESSAGE,message).build())));
		}
		builder.setThrown(event.getThrown());
		builder.setContextStack(event.getContextStack());
		builder.setThreadName(event.getThreadName());
		builder.setSource(event.getSource());
		builder.setTimeMillis(event.getTimeMillis());
		return builder.build();
	}

	@PluginFactory
	public static LogMessageUpdater createPolicy() {
		return new LogMessageUpdater();
	}
	
	private static boolean isValidJson(String message){
		try{
			new ObjectMapper().readTree(message);
		}catch(Exception e){
			return false;
		}
		
		return true;
	}
}
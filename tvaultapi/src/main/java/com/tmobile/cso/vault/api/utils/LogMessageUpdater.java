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
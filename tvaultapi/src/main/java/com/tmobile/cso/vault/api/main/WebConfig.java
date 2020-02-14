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

package com.tmobile.cso.vault.api.main;

import javax.servlet.Filter;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.tmobile.cso.vault.api.filter.ApiMetricFilter;
import com.tmobile.cso.vault.api.filter.TokenValidationFilter;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class WebConfig {

	@Value("${spring.mail.host}")
	private String emailHost;

	@Value("${spring.mail.port}")
	private Integer emailPort;

	@Value("${ad.notification.mail.template.mode}")
	private String templateMode;

	@Bean
	public Filter ApiMetricFilter() {
		return new ApiMetricFilter();
	}
	@Bean
	public Filter TokenValidationFilter() {
		return new TokenValidationFilter();
	}
	@Bean
	public JavaMailSender getJavaMailSender() {
	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	    mailSender.setHost(emailHost);
	    mailSender.setPort(emailPort);
	    return mailSender;
	}
	@Bean
	public ITemplateResolver templateResolver()	{
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix(TVaultConstants.EMAIL_TEMPLATE_PREFIX);
		templateResolver.setSuffix(TVaultConstants.EMAIL_TEMPLATE_SUFFIX);
		templateResolver.setTemplateMode(templateMode);
		return templateResolver;
	}
	@Bean
	public TemplateEngine templateEngine() {
		TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(this.templateResolver());
		return templateEngine;
	}
}
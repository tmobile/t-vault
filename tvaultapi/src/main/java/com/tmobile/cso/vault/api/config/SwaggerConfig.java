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

package com.tmobile.cso.vault.api.config;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.StringVendorExtension;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@EnableAutoConfiguration
@PropertySource("classpath:swaggernotes.properties")
@ComponentScan(basePackages="com.tmobile.cso.vault.api.*")
public class SwaggerConfig {

	@Value("${selfservice.enable:true}")
	private boolean isSelfServiceEnabled;

	@Value("${ad.passwordrotation.enable:true}")
	private boolean isAdPswdRotationEnabled;

	private static Logger log = LogManager.getLogger(SwaggerConfig.class);

	@Bean
	public Docket tvaultapi() {

		Docket docket = new Docket(DocumentationType.SWAGGER_2);
		ApiSelectorBuilder apiSelectorBuilder = new ApiSelectorBuilder(docket);
		apiSelectorBuilder.apis(RequestHandlerSelectors.any());
		apiSelectorBuilder.paths(PathSelectors.any());
		apiSelectorBuilder.paths(Predicates.not(PathSelectors.regex("/error.*")));
		log.debug("Checking enable/disable status for features");
		if (!isSelfServiceEnabled) {
			log.debug("Self Service is disabled");
			apiSelectorBuilder.paths(PathSelectors.regex("(?!/v2/ss).+"));
		}
		if (!isAdPswdRotationEnabled) {
			log.debug("AD Password rotation is disabled");
			apiSelectorBuilder.paths(PathSelectors.regex("(?!/v2/ad).+"));
			apiSelectorBuilder.paths(PathSelectors.regex("(?!/v2/serviceaccounts).+"));
		}
		return apiSelectorBuilder.build().pathMapping("/").apiInfo(metadata());
	}
	/**
	 * TODO: Values to be corrected later
	 * @return
	 */
	private ApiInfo metadata() {
		List<VendorExtension> vxtensions = new ArrayList<VendorExtension>();
		vxtensions.add((new StringVendorExtension("","")));
		ApiInfo apiInfo = new ApiInfo(
				"TVault API - Simplified Secret Management System",
				"T-Vault is built to simplify the process of secrets management. We wanted to build an intuitive and easy to use tool that application developers can easily adopt without sacrificing their agility while still following best practices for secrets management. It uses a few open source products internally including, at its heart Hashicorp Vault. Hashicorp vault provides the core functionality of safely storing secrets at rest and access control to those secrets. T-Vault builds on that base to provide a higher-level of abstraction called Safe. Safes are logical abstractions, internally using the concept of paths within vault. T-Vault simplifies the access management to secrets by hiding away all the complexities of managing polices.",
				"2.0",
				"https://github.com/tmobile/t-vault/blob/dev/CODE_OF_CONDUCT.md",
				new Contact("","",""),
				"Apache License Version 2.0",
				"https://github.com/tmobile/t-vault/blob/dev/LICENSE", 
				vxtensions) ;
		return apiInfo;
	}
}

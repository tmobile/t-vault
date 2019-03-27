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

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.pool.factory.PoolingContextSource;
import org.springframework.ldap.pool.validation.DefaultDirContextValidator;
import org.springframework.ldap.transaction.compensating.manager.TransactionAwareContextSourceProxy;

@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api.*"})
@EnableLdapRepositories(basePackages = "com.tmobile.cso.vault.api.**")
public class ServiceAccountsConfiguration {

    @Autowired
    private Environment env;

    @Bean
    public LdapContextSource svcAccContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(env.getRequiredProperty("ad.url"));
        contextSource.setBase(env.getRequiredProperty("ad.base"));
        contextSource.setUserDn("CN="+env.getRequiredProperty("ad.username")+","+env.getRequiredProperty("ad.userdn"));
        contextSource.setPassword(new String(Base64.getDecoder().decode(env.getRequiredProperty("ad.password"))));
        return contextSource;
    }
    @Bean
    public ContextSource poolingLdapContextSource() {
        PoolingContextSource poolingContextSource = new PoolingContextSource();
        poolingContextSource.setDirContextValidator(new DefaultDirContextValidator());
        poolingContextSource.setContextSource(svcAccContextSource());
        poolingContextSource.setTestOnBorrow(true);
        poolingContextSource.setTestWhileIdle(true);

        TransactionAwareContextSourceProxy proxy = new TransactionAwareContextSourceProxy(poolingContextSource);
        return proxy;
    }
    @Bean(name="svcAccLdapTemplate")
    public LdapTemplate svcAccLdapTemplate() {
		LdapTemplate ldapTemplate = new LdapTemplate(poolingLdapContextSource());
		ldapTemplate.setIgnorePartialResultException(true);
		return ldapTemplate;
    }
}
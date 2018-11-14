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

package com.tmobile.cso.vault.api.main;

import javax.servlet.Filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tmobile.cso.vault.api.filter.ApiMetricFilter;
import com.tmobile.cso.vault.api.filter.TokenValidationFilter;

@Configuration
public class WebConfig {

	@Bean
	public Filter ApiMetricFilter() {
		return new ApiMetricFilter();
	}
	@Bean
	public Filter TokenValidationFilter() {
		return new TokenValidationFilter();
	}

}
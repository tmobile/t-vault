// =========================================================================
// Copyright 2021 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License")
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

package com.tmobile.cso.vault.api.v2.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tmobile.cso.vault.api.service.BuildDetailsService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class BuildDetailsControllerTest {

	
	@Mock
	public BuildDetailsService buildDetailsService;

	private MockMvc mockMvc;

	@InjectMocks
	public BuildDetailsController buildDetailsController;
	
	 @Mock
	    HttpServletRequest httpServletRequest;

	    @Before
	    public void setUp() {
	        MockitoAnnotations.initMocks(this);
	        this.mockMvc = MockMvcBuilders.standaloneSetup(buildDetailsController).build();
	    }
	    
	@Test
	public void test_getAuthenticationMounts_successful() throws Exception {
		when(buildDetailsService.getBuildDetails()).thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				buildDetailsController.getBuildDetails().getStatusCode());
	}
	
	
}

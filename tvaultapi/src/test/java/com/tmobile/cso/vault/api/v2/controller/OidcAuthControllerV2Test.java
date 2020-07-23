// =========================================================================
// Copyright 2020 T-Mobile, US
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.OidcRequest;
import com.tmobile.cso.vault.api.service.OidcAuthService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@WebAppConfiguration
public class OidcAuthControllerV2Test {

    private MockMvc mockMvc;

    @Mock
    private OidcAuthService oidcAuthService;

    @InjectMocks
    private OidcAuthControllerV2 oidcAuthControllerV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(oidcAuthControllerV2).build();
    }

    @Test
    public void test_getAuthUrl() throws Exception {

        OidcRequest oidcRequest = new OidcRequest("default", "http://localhost:3000");
        String inputJson =new ObjectMapper().writeValueAsString(oidcRequest);
        String responseMessage = "{\n" +
                "  \"request_id\": \"test-b184-88b8-6ac68f0ab58d\",\n" +
                "  \"lease_id\": \"\",\n" +
                "  \"renewable\": false,\n" +
                "  \"lease_duration\": 0,\n" +
                "  \"data\": {\n" +
                "    \"auth_url\": \"https://login.authdomain.com/test123123/oauth2/v2.0/authorize?client_id=test123123&nonce=123123&redirect_uri=http%3A%2F%2Flocalhost%3A3000&response_type=code&scope=openid+https%3A%2F%2Fgraph.authdomain.com%2F.default+profile&state=test4343545\"\n" +
                "  },\n" +
                "  \"wrap_info\": null,\n" +
                "  \"warnings\": null,\n" +
                "  \"auth\": null\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(oidcAuthService.getAuthUrl(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/oidc/auth_url")
                .header("vault-token", "testy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }


    @Test
    public void test_proessCallback() throws Exception {

        String responseMessage = "{\n" +
                "\"client_token\": \"testmioJHmaUB1k7PB2wUDh\",\n" +
                "\"admin\": \"yes\",\n" +
                "\"access\": {\n" +
                "\"cert\": [{\n" +
                "\"test.company.com\": \"read\"\n" +
                "}],\n" +
                "\"svcacct\": [{\n" +
                "\"svc_test04\": \"write\"\n" +
                "}],\n" +
                "\"users\": [{\n" +
                "\"testsafe1\": \"read\"\n" +
                "}]\n" +
                "},\n" +
                "\"policies\": [\"default\"],\n" +
                "\"lease_duration\": 1800\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(oidcAuthService.processCallback(Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/oidc/callback")
                .header("vault-token", "test4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

}

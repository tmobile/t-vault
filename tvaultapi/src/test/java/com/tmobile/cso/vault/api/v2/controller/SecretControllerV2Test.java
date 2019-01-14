package com.tmobile.cso.vault.api.v2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.service.LDAPAuthService;
import com.tmobile.cso.vault.api.service.SecretService;
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@WebAppConfiguration
public class SecretControllerV2Test {

    private MockMvc mockMvc;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private SecretControllerV2 secretControllerV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(secretControllerV2).build();
    }

    @Test
    public void test_readFromVault() throws Exception {

        String responseMessage = "{  \"data\": {    \"secret1\": \"value1\",    \"secret2\": \"value2\"  }}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(secretService.readFromVault(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), eq("users/safe1"))).thenReturn(responseEntityExpected);
        when(secretService.readFoldersAndSecrets(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), eq("users/safe1"))).thenReturn(responseEntityExpected);
        when(secretService.readFromVaultRecursive(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), eq("users/safe1"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/safes/folders/secrets?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/safes/folders/secrets?path=users/safe1&fetchOption=all")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/safes/folders/secrets?path=users/safe1&fetchOption=recursive")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_write() throws Exception {

        String inputJson ="{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        String responseMessage = "{\"messages\":[\"Secret saved to vault\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(secretService.write(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/write")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_deleteFromVault() throws Exception {

        String responseMessage = "{\"messages\":[\"Secrets deleted\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(secretService.deleteFromVault(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), eq("users/safe1"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/safes/folders/secrets?path=users/safe1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }
}

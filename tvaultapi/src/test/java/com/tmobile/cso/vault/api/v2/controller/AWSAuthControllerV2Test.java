package com.tmobile.cso.vault.api.v2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.service.AWSAuthService;
import com.tmobile.cso.vault.api.service.AppRoleService;
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
public class AWSAuthControllerV2Test {

    private MockMvc mockMvc;

    @Mock
    private AWSAuthService awsAuthService;

    @InjectMocks
    private AWSAuthControllerV2 awsAuthControllerV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(awsAuthControllerV2).build();
    }

    @Test
    public void test_authenticateEC2() throws Exception {
        AWSLogin awsLogin = new AWSLogin("role1", "pkcs7");

        String inputJson =new ObjectMapper().writeValueAsString(awsLogin);
        String responseMessage = "sample message";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.authenticateEC2(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/aws/login")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_createRole() throws Exception {
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String inputJson =new ObjectMapper().writeValueAsString(awsLoginRole);
        String responseMessage = "{\"messages\":[\"AWS Role created \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.createRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"),Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/aws/role")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_updateRole() throws Exception {
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String inputJson =new ObjectMapper().writeValueAsString(awsLoginRole);
        String responseMessage = "{\"messages\":[\"AWS Role updated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.updateRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"),Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/auth/aws/role")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_deleteRole() throws Exception {
        String responseMessage = "{\"messages\":[\"Role deleted \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.deleteRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"),Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/auth/aws/role/role1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_fetchRole() throws Exception {

        String responseMessage = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\": [ \"\\\"[prod\",\"dev\\\"]\" ]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.fetchRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/aws/role/role1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_listRoles() throws Exception {

        String responseMessage = "{ \"keys\": [\"mytestawsrole\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.listRoles(eq("5PDrOhsy4ig8L3EpsJZSLAMg"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/roles")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_configureClient() throws Exception {
        AWSClientConfiguration awsClientConfiguration = new AWSClientConfiguration();
        awsClientConfiguration.setAccess_key("accesskey");
        awsClientConfiguration.setSecret_key("secretKey");

        String inputJson =new ObjectMapper().writeValueAsString(awsClientConfiguration);
        String responseMessage = "{\"messages\":[\"AWS Client successfully configured \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.configureClient(Mockito.any(), eq("5PDrOhsy4ig8L3EpsJZSLAMg"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/aws/config/client")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_readClientConfiguration() throws Exception {

        String responseMessage = "{ \"access_key\": \"string\", \"secret_key\": null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.readClientConfiguration(eq("5PDrOhsy4ig8L3EpsJZSLAMg"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/auth/aws/config/client")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_createSTSRole() throws Exception {
        AWSStsRole awsStsRole = new AWSStsRole();
        awsStsRole.setAccount_id("account_id1");
        awsStsRole.setSts_role("sts_role1");

        String inputJson =new ObjectMapper().writeValueAsString(awsStsRole);
        String responseMessage = "{\"messages\":[\"STS Role created successfully \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.createSTSRole(Mockito.any(), eq("5PDrOhsy4ig8L3EpsJZSLAMg"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/aws/config/sts")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_authenticateIAM() throws Exception {
        AWSIAMLogin awsiamLogin = new AWSIAMLogin();
        awsiamLogin.setIam_http_request_method("POST");
        awsiamLogin.setIam_request_body("{}");
        awsiamLogin.setIam_request_headers("{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}");
        awsiamLogin.setIam_request_url("http://testurl.com");
        awsiamLogin.setRole("testawsrole");
        awsiamLogin.setPkcs7("MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx");

        String inputJson =new ObjectMapper().writeValueAsString(awsiamLogin);
        String responseMessage = "sample response";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.authenticateIAM(Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/aws/iam/login")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_authenticate() throws Exception {
        AWSAuthLogin awsAuthLogin = new AWSIAMLogin();
        awsAuthLogin.setIam_http_request_method("POST");
        awsAuthLogin.setIam_request_body("{}");
        awsAuthLogin.setIam_request_headers("{\"token\":\"5PDrOhsy4ig8L3EpsJZSLAMg\"}");
        awsAuthLogin.setIam_request_url("http://testurl.com");
        awsAuthLogin.setRole("testawsrole");
        awsAuthLogin.setPkcs7("MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====");

        String inputJson =new ObjectMapper().writeValueAsString(awsAuthLogin);
        String responseMessage = "sample response";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(awsAuthService.authenticate(Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/auth/aws/login/EC2")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }
}

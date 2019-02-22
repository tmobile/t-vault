package com.tmobile.cso.vault.api.v2.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.AWSRole;
import com.tmobile.cso.vault.api.model.AppRole;
import com.tmobile.cso.vault.api.model.AppRoleAccessorIds;
import com.tmobile.cso.vault.api.model.Safe;
import com.tmobile.cso.vault.api.model.SafeBasicDetails;
import com.tmobile.cso.vault.api.model.SafeGroup;
import com.tmobile.cso.vault.api.model.SafeUser;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.SelfSupportService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@WebAppConfiguration
@WebMvcTest
public class SelfSupportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SelfSupportService selfSupportService;

    @InjectMocks
    private SelfSupportController selfSupportController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(selfSupportController).build();
    }

    UserDetails getMockUser(boolean isAdmin) {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        return userDetails;
    }

    @Test
    public void test_getFoldersRecursively() throws Exception {
        String responseJson = "{  \"keys\": [    \"mysafe01\"  ]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.getFoldersRecursively(userDetails, "5PDrOhsy4ig8L3EpsJZSLAMg", "users/safe1")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/sdb/list?path=users/safe1").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getSafe() throws Exception {
        String responseJson = "{  \"keys\": [    \"mysafe01\"  ]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.getSafe(userDetails, "5PDrOhsy4ig8L3EpsJZSLAMg", "users/safe1")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/sdb?path=users/safe1").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_addUsertoSafe() throws Exception {
        String responseJson = "{\"messages\":[\"User is successfully associated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeUser);
        when(selfSupportService.addUserToSafe(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(SafeUser.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/sdb/user").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_deleteUserFromSafe() throws Exception {
        String responseJson = "{\"messages\":[\"User is successfully associated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        SafeUser safeUser = new SafeUser("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeUser);
        when(selfSupportService.removeUserFromSafe(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(SafeUser.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/ss/sdb/user").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getInfo() throws Exception {
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.getInfo(userDetails,"5PDrOhsy4ig8L3EpsJZSLAMg", "users")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/sdb/folder/users?path=users").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createSafe() throws Exception {
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String inputJson =new ObjectMapper().writeValueAsString(safe);
        String responseJson = "{\"messages\":[\"Safe updated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.createSafe(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(Safe.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/sdb").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_isAuthorized() throws Exception {
        String responseJson = "true";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(String.valueOf(true));
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.isAuthorized(userDetails, "users/mysafe01")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/auth/tvault/isauthorized?path=users/mysafe01").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_updateSafe() throws Exception {
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String inputJson =new ObjectMapper().writeValueAsString(safe);
        String responseJson = "{\"messages\":[\"Safe updated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.updateSafe(eq(userDetails),eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(Safe.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/ss/sdb").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_deletefolder() throws Exception {
        String responseJson = "{\"messages\":[\"SDB deleted\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.deletefolder(userDetails,"5PDrOhsy4ig8L3EpsJZSLAMg", "users/safe1")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/ss/sdb/delete?path=users/safe1").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_addGroupToSafe() throws Exception {
        SafeGroup safeGroup = new SafeGroup("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeGroup);
        String responseJson = "{\"messages\":[\"Group is successfully associated with Safe\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.addGroupToSafe(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(SafeGroup.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/sdb/group").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_removeGroupFromSafe() throws Exception {
        SafeGroup safeGroup = new SafeGroup("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(safeGroup);
        String responseJson = "{\"messages\":[\"Group association is removed \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.removeGroupFromSafe(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(SafeGroup.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/ss/sdb/group").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_addAwsRoleToSafe() throws Exception {
        AWSRole awsRole = new AWSRole("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(awsRole);
        String responseJson = "{\"messages\":[\"Role is successfully associated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.addAwsRoleToSafe(eq(userDetails),eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AWSRole.class))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/sdb/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_deleteAwsRoleFromSafe() throws Exception {
        AWSRole awsRole = new AWSRole("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(awsRole);
        String responseJson = "{\"messages\":[\"Role association is removed \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.removeAWSRoleFromSafe(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AWSRole.class), eq(false))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/ss/sdb/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_detachAwsRoleFromSafe() throws Exception {
        AWSRole awsRole = new AWSRole("users/safe1", "testuser1","write");

        String inputJson =new ObjectMapper().writeValueAsString(awsRole);
        String responseJson = "{\"messages\":[\"Role association is removed \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.removeAWSRoleFromSafe(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AWSRole.class), eq(true))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/ss/sdb/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_associateApproletoSDB() throws Exception {
        String responseJson = "{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}";
        String inputJson = "{\"role\":\"approle1\",\"path\":\"users/safe1\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.associateApproletoSDB(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/sdb/approle").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_deleteApproleFromSDB() throws Exception {
        String responseJson = "{\"messages\":[\"Role association is removed \"]}";
        String inputJson = "{\"role\":\"approle1\",\"path\":\"users/safe1\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.deleteApproleFromSDB(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/ss/sdb/approle").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
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
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.createRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"),Mockito.any(AWSLoginRole.class), eq("users/mysafe01"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/auth/aws/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
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
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.updateRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"),Mockito.any(AWSLoginRole.class), eq("users/mysafe01"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/ss/auth/aws/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_createIAMRole() throws Exception {
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String inputJson =new ObjectMapper().writeValueAsString(awsiamRole);
        String responseMessage = "{\"messages\":[\"AWS IAM Role created successfully \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.createIAMRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AWSIAMRole.class), eq("users/mysafe01"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/auth/aws/iam/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_updateIAMRole() throws Exception {
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String inputJson =new ObjectMapper().writeValueAsString(awsiamRole);
        String responseMessage = "{\"messages\":[\"AWS Role updated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.updateIAMRole(eq(userDetails), eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AWSIAMRole.class), eq("users/mysafe01"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.put("/v2/ss/auth/aws/iam/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_getSafeNames() throws Exception {
        String responseJson = "{\"shared\":[\"safe5\",\"safe6\"],\"users\":[\"safe3\",\"safe4\"],\"apps\":[\"safe1\",\"safe2\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.getAllSafeNames(userDetails)).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/sdb/names").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_createAppRole() throws Exception {
        String responseJson = "{\"messages\":[\"AppRole created successfully\"]}";
        ResponseEntity<String> responseEntityExpected =ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.createAppRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AppRole.class), eq(userDetails))).thenReturn(responseEntityExpected);
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, 1, 100, 0);
        String inputJson =new ObjectMapper().writeValueAsString(appRole);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/auth/approle/role").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_deleteAppRole() throws Exception {
        String responseJson = "{\"messages\":[\"AppRole deleted\"]}";
        ResponseEntity<String> responseEntityExpected =ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.deleteAppRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(AppRole.class), eq(userDetails))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/ss/auth/approle/role/approle1").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_getsafes() throws Exception {
        String responseJson = "{\"shared\":[{\"s2\":\"read\"}],\"users\":[{\"s1\":\"read\"},{\"s5\":\"read\"}]}";
        ResponseEntity<String> responseEntityExpected =ResponseEntity.status(HttpStatus.OK).body(responseJson);
        UserDetails userDetails = getMockUser(false);
        when(selfSupportService.getSafes(userDetails, "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/sdb/safes").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void test_readAppRole() throws Exception {

        String responseMessage = "sample response";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(selfSupportService.readAppRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/approle/role/approle1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_readAppRoles() throws Exception {

        // Mock response
        String responseMessage = "sample response";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(selfSupportService.readAppRoles(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/approle/role")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }
    
    @Test
    public void test_deleteSecretIds() throws Exception {
		ArrayList<String> failedAccessorIds = new ArrayList<String>();
		ArrayList<String> deletedAccessorIds = new ArrayList<String>();
		deletedAccessorIds.add("deleted01");
		failedAccessorIds.add("failed01");
		StringBuilder responseMessage = new StringBuilder("Deletion of secret_ids completed as: ");
		String vaultToken = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String role_name = "testapprole01";
		AppRoleAccessorIds appRoleAccessorIds = new AppRoleAccessorIds();
		appRoleAccessorIds.setRole_name(role_name);
		appRoleAccessorIds.setAccessorIds(new String[] {"deleted01", "failed01"});
		if (!CollectionUtils.isEmpty(deletedAccessorIds)) {
			responseMessage.append(String.format("Succssfully deleted the secret_ids for the following accessor_ids: [%s]. ",StringUtils.join(deletedAccessorIds.toArray(), ",")));
		}
		if (!CollectionUtils.isEmpty(failedAccessorIds)) {
			responseMessage.append(String.format("Failed to delete the secret_ids for the following accessor_ids: [%s]",StringUtils.join(failedAccessorIds.toArray(), ",")));
		}
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage.toString());
        when(selfSupportService.deleteSecretIds(eq(vaultToken), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/ss/approle/"+role_name+"/secret_id").content(new ObjectMapper().writeValueAsString(appRoleAccessorIds))
                .header("vault-token", vaultToken)
                .header("Content-Type", "application/json;charset=UTF-8"))
        		.andExpect(status().isOk())
        		.andExpect(content().string(containsString(responseMessage.toString())));
    }
    
    @Test
    public void test_readAppRoleRoleId() throws Exception {
		String role_name = "testapprole01";
		String vaultToken = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String role_id_response = "{\n" + 
    			"  \"data\": {\n" + 
    			"    \"role_id\": \"generated-role-id\"\n" + 
    			"  }\n" + 
    			"}";
    	StringBuilder responseMessage = new StringBuilder(role_id_response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage.toString());
        when(selfSupportService.readAppRoleRoleId(eq(vaultToken), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/approle/"+role_name+"/role_id")
                .header("vault-token", vaultToken)
                .header("Content-Type", "application/json;charset=UTF-8"))
        		.andExpect(status().isOk())
        		.andExpect(content().string(containsString(responseMessage.toString())));
    }
    @Test
    public void test_readAppRoleSecretId() throws Exception {
		String role_name = "testapprole01";
		String vaultToken = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String role_id_response = "{\n" + 
    			"  \"data\": {\n" + 
    			"    \"secret_id\": \"generated-role-id\",\n" + 
    			"    \"secret_id_accessor\": \"accesssor-for-generated-role-id\"\n" + 
    			"  }\n" + 
    			"}";
    	StringBuilder responseMessage = new StringBuilder(role_id_response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage.toString());
        when(selfSupportService.readAppRoleSecretId(eq(vaultToken), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/approle/"+role_name+"/secret_id")
                .header("vault-token", vaultToken)
                .header("Content-Type", "application/json;charset=UTF-8"))
        		.andExpect(status().isOk())
        		.andExpect(content().string(containsString(responseMessage.toString())));
    }
    
    @Test
    public void test_readAppRoleDetails() throws Exception {
		String role_name = "testapprole01";
		String vaultToken = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String role_id_response = "{\n" + 
    			"  \"appRole\": {\n" + 
    			"    \"role_name\": \"testapprole01\",\n" + 
    			"    \"policies\": [\n" + 
    			"      \"string\"\n" + 
    			"    ],\n" + 
    			"    \"bind_secret_id\": true,\n" + 
    			"    \"secret_id_num_uses\": \"0\",\n" + 
    			"    \"secret_id_ttl\": \"0\",\n" + 
    			"    \"token_num_uses\": 0,\n" + 
    			"    \"token_ttl\": 0,\n" + 
    			"    \"token_max_ttl\": 0\n" + 
    			"  },\n" + 
    			"  \"role_id\": \"generated_role_id\",\n" + 
    			"  \"accessorIds\": [\n" + 
    			"    \"accesssor-for-generated-role-id\"\n" + 
    			"  ],\n" + 
    			"  \"appRoleMetadata\": {\n" + 
    			"    \"path\": \"metadata/approle/testapprole01\",\n" + 
    			"    \"data\": {\n" + 
    			"      \"name\": \"myvaultapprole\",\n" + 
    			"      \"createdBy\": \"testuser1\"\n" + 
    			"    }\n" + 
    			"  }\n" + 
    			"}";
    	StringBuilder responseMessage = new StringBuilder(role_id_response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage.toString());
        when(selfSupportService.readAppRoleDetails(eq(vaultToken), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/approle/"+role_name)
                .header("vault-token", vaultToken)
                .header("Content-Type", "application/json;charset=UTF-8"))
        		.andExpect(status().isOk())
        		.andExpect(content().string(containsString(responseMessage.toString())));
    }
    
    @Test
    public void test_readSecretIdAccessors() throws Exception {
		String role_name = "testapprole01";
		String vaultToken = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String role_id_response = "{\n" + 
    			"  \"keys\": [\n" + 
    			"    \"accesssor-for-generated-role-id\"\n" + 
    			"  ]\n" + 
    			"}";
    	StringBuilder responseMessage = new StringBuilder(role_id_response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage.toString());
        when(selfSupportService.readSecretIdAccessors(eq(vaultToken), Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/approle/"+role_name+"/accessors")
                .header("vault-token", vaultToken)
                .header("Content-Type", "application/json;charset=UTF-8"))
        		.andExpect(status().isOk())
        		.andExpect(content().string(containsString(responseMessage.toString())));
    }
    
    @Test
    public void test_listAppRoles() throws Exception {
		String vaultToken = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String role_id_response = "{\n" + 
    			"  \"keys\": [\n" + 
    			"    \"testapprole01\"\n" + 
    			"  ]\n" + 
    			"}";
    	StringBuilder responseMessage = new StringBuilder(role_id_response);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage.toString());
        when(selfSupportService.listAppRoles(eq(vaultToken), Mockito.any())).thenReturn(responseEntityExpected);
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/ss/approle")
                .header("vault-token", vaultToken)
                .header("Content-Type", "application/json;charset=UTF-8"))
        		.andExpect(status().isOk())
        		.andExpect(content().string(containsString(responseMessage.toString())));
    }
}

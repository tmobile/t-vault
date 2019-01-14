package com.tmobile.cso.vault.api.v3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.service.SafesService;
import com.tmobile.cso.vault.api.service.SelfSupportService;
import com.tmobile.cso.vault.api.v2.controller.SDBControllerV2;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@WebAppConfiguration
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

        mockMvc.perform(MockMvcRequestBuilders.get("/v3/sdb/list?path=users/safe1").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.get("/v3/sdb?path=users/safe1").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/sdb/user").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.delete("/v3/sdb/user").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.get("/v3/sdb/folder/users?path=users").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/sdb").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.put("/v3/sdb").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.delete("/v3/sdb/delete?path=users/safe1").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/sdb/group").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.delete("/v3/sdb/group").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/sdb/role").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.delete("/v3/sdb/role").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.put("/v3/sdb/role").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/sdb/approle").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.delete("/v3/sdb/approle").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/auth/aws/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.put("/v3/auth/aws/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/auth/aws/iam/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.put("/v3/auth/aws/iam/role?path=users/mysafe01").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.get("/v3/sdb/names").requestAttr("UserDetails", userDetails)
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
        AppRole appRole = new AppRole("approle1", policies, true, "1", "100m", 0);
        String inputJson =new ObjectMapper().writeValueAsString(appRole);

        mockMvc.perform(MockMvcRequestBuilders.post("/v3/auth/approle/role").requestAttr("UserDetails", userDetails)
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

        mockMvc.perform(MockMvcRequestBuilders.delete("/v3/auth/approle/role/approle1").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }
}

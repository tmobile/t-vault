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
package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class AWSIAMAuthServiceTest {

    @InjectMocks
    AWSIAMAuthService awsIamAuthService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }

    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (expectedBody != "") {
            response.setResponse(expectedBody);
        }
        return response;
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
    public void testcreateIAMRolesuccessfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String jsonStr = "{\"auth_type\": \"iam\",\"bound_iam_principal_arn\":" +
                " [\"arn:aws:iam::123456789012:user/tst\"],\"policies\": " +
                "[\"string\"],\"resolve_aws_unique_ids\": true,\"role\": \"string\"}";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");


        when(reqProcessor.process("/auth/aws/iam/role/create",jsonStr, token)).thenReturn(response);
        when(JSONUtil.getJSON(awsiamRole)).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        try {
            when(ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)).thenReturn(true);
            when(ControllerUtil.populateUserMetaJson(Mockito.any(), Mockito.any())).thenReturn("awsiamroleUsermetadataJson");
            ResponseEntity<String> responseEntity = awsIamAuthService.createIAMRole(awsiamRole, token, userDetails);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            assertEquals(responseEntityExpected, responseEntity);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void test_createIAMRole_failure_revert() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String jsonStr = "{\"auth_type\": \"iam\",\"bound_iam_principal_arn\":" +
                " [\"arn:aws:iam::123456789012:user/tst\"],\"policies\": " +
                "[\"string\"],\"resolve_aws_unique_ids\": true,\"role\": \"string\"}";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS IAM role creation failed.\"]}");


        when(reqProcessor.process("/auth/aws/iam/role/create",jsonStr, token)).thenReturn(response);
        when(JSONUtil.getJSON(awsiamRole)).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(false);
        when(reqProcessor.process("/auth/aws/iam/roles/delete","{\"role\":\""+awsiamRole.getRole()+"\"}",token)).thenReturn(response);
        try {
            when(ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)).thenReturn(true);
            ResponseEntity<String> responseEntity = awsIamAuthService.createIAMRole(awsiamRole, token, userDetails);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
            assertEquals(responseEntityExpected, responseEntity);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_createIAMRole_revert_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String jsonStr = "{\"auth_type\": \"iam\",\"bound_iam_principal_arn\":" +
                " [\"arn:aws:iam::123456789012:user/tst\"],\"policies\": " +
                "[\"string\"],\"resolve_aws_unique_ids\": true,\"role\": \"string\"}";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM role created however metadata update failed. Please try with AWS role/update \"]}");


        when(reqProcessor.process("/auth/aws/iam/role/create",jsonStr, token)).thenReturn(response);
        when(JSONUtil.getJSON(awsiamRole)).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(false);
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/auth/aws/iam/roles/delete","{\"role\":\""+awsiamRole.getRole()+"\"}",token)).thenReturn(response404);
        try {
            when(ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)).thenReturn(true);
            ResponseEntity<String> responseEntity = awsIamAuthService.createIAMRole(awsiamRole, token, userDetails);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            assertEquals(responseEntityExpected, responseEntity);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test_createIAMRole_revert_failure_with_error_code() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String jsonStr = "{\"auth_type\": \"iam\",\"bound_iam_principal_arn\":" +
                " [\"arn:aws:iam::123456789012:user/tst\"],\"policies\": " +
                "[\"string\"],\"resolve_aws_unique_ids\": true,\"role\": \"string\"}";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages" +
                "\":[\"AWS IAM role created however metadata update failed. Please try with AWS role/update \"]}");


        when(reqProcessor.process("/auth/aws/iam/role/create",jsonStr, token)).thenReturn(response);
        when(JSONUtil.getJSON(awsiamRole)).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(false);
        Response response404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(reqProcessor.process("/auth/aws/iam/roles/delete","{\"role\":\""+awsiamRole.getRole()+"\"}",token)).thenReturn(response404);
        try {
            when(ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)).thenReturn(true);
            ResponseEntity<String> responseEntity = awsIamAuthService.createIAMRole(awsiamRole, token, userDetails);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
           } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = TVaultValidationException.class)
    public void test_createIAMRole_failure_400() throws TVaultValidationException{
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        when(ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)).thenReturn(false);
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntity = awsIamAuthService.createIAMRole(awsiamRole, token, userDetails);
    }

    @Test
    public void test_updateIAMRole_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String jsonStr = "{\"auth_type\": \"iam\",\"bound_iam_principal_arn\":" +
                " [\"arn:aws:iam::123456789012:user/tst\"],\"policies\": " +
                "[\"string\"],\"resolve_aws_unique_ids\": true,\"role\": \"mytestawsrole\"}";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response updateResponse = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"AWS Role updated \"]}");

        String jsonGetStr = "{\"bound_ami_id\": [\"ami-fce3c696\"],\"role_tag\": \"\",\"policies\": " +
                "[\"\\\"[prod\", \"dev\\\"]\" ],\"bound_iam_principal_arn\": [],\"bound_iam_role_arn\":" +
                "[ \"arn:aws:iam::8987887:role/test-role\"],\"max_ttl\": 0,\"disallow_reauthentication\": " +
                "false,\"allow_instance_migration\": false}";
        Response getResponse = getMockResponse(HttpStatus.OK, true, jsonGetStr);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");

        when(JSONUtil.getJSON(awsiamRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/aws/iam/roles","{\"role\":\"mytestawsrole\"}",token)).thenReturn(getResponse);
        when(reqProcessor.process("/auth/aws/roles/delete",jsonStr,token)).thenReturn(response);
        when(reqProcessor.process("/auth/aws/iam/roles/update",jsonStr,token)).thenReturn(response);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(updateResponse);
        try {
            when(ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)).thenReturn(true);
            ResponseEntity<String> responseEntity = awsIamAuthService.updateIAMRole(token, awsiamRole);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            assertEquals(responseEntityExpected, responseEntity);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void test_updateIAMRole_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        String jsonStr = "{\"auth_type\": \"iam\",\"bound_iam_principal_arn\":" +
                " [\"arn:aws:iam::123456789012:user/tst\"],\"policies\": " +
                "[\"string\"],\"resolve_aws_unique_ids\": true,\"role\": \"mytestawsrole\"}";
        Response getResponse = getMockResponse(HttpStatus.NOT_FOUND, false, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Update failed . AWS Role does not exist \"]}");

        when(JSONUtil.getJSON(awsiamRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/aws/iam/roles","{\"role\":\"mytestawsrole\"}",token)).thenReturn(getResponse);

        try {
            when(ControllerUtil.areAWSIAMRoleInputsValid(awsiamRole)).thenReturn(true);
            ResponseEntity<String> responseEntity = awsIamAuthService.updateIAMRole(token, awsiamRole);
            assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
            assertEquals(responseEntityExpected, responseEntity);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
    }



    @Test
    public void test_fetchIAMRole_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsoninput= "{\"role\":\"mytestawsrole\"}";
        String jsonGetStr = "{\"bound_ami_id\": [\"ami-fce3c696\"],\"role_tag\": \"\",\"policies\": " +
                "[\"\\\"[prod\", \"dev\\\"]\" ],\"bound_iam_principal_arn\": [],\"bound_iam_role_arn\":" +
                "[ \"arn:aws:iam::8987887:role/test-role\"],\"max_ttl\": 0,\"disallow_reauthentication\": " +
                "false,\"allow_instance_migration\": false}";
        Response getResponse = getMockResponse(HttpStatus.OK, true, jsonGetStr);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(jsonGetStr);

        when(reqProcessor.process("/auth/aws/iam/roles",jsoninput,token)).thenReturn(getResponse);
        ResponseEntity<String> responseEntity = awsIamAuthService.fetchIAMRole(token, "mytestawsrole");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_listIAMRole_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonGetStr = "{  \"keys\": [ \"mytestawsrole\" ]}";
        Response listResponse = getMockResponse(HttpStatus.OK, true, jsonGetStr);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(jsonGetStr);

        when(reqProcessor.process("/auth/aws/iam/roles/list","{}",token)).thenReturn(listResponse);
        ResponseEntity<String> responseEntity = awsIamAuthService.listIAMRoles(token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteIAMRole_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"IAM Role deleted \"]}");

        when(reqProcessor.process("/auth/aws/iam/roles/delete","{\"role\":\"mytestawsrole\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = awsIamAuthService.deleteIAMRole(token, "mytestawsrole");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteIAMRole_failure_500() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "{\"errors\":[\"Internal Server Error\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Internal Server Error\"]}");

        when(reqProcessor.process("/auth/aws/iam/roles/delete","{\"role\":\"mytestawsrole\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = awsIamAuthService.deleteIAMRole(token, "mytestawsrole");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_configureAWSIAMRole_successfully() {
        String roleName = "role1";
        String policies = "{\"default\"}";
        String token = "7QPMPIGiyDFlJkrK3jFykUqa";

        Response responsemock = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process(eq("/auth/aws/iam/roles/update"),Mockito.any(),eq(token))).thenReturn(responsemock);
        Response response = awsIamAuthService.configureAWSIAMRole(roleName, policies, token);
        assertEquals(HttpStatus.NO_CONTENT, response.getHttpstatus());
    }
}
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
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class AWSAuthServiceTest {

    @InjectMocks
    AWSAuthService awsAuthService;

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
    public void test_createRole_successfully() {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String jsonStr = "{\"auth_type\": \"ec2\", \"role\": \"mytestawsrole\", \"bound_ami_id\": \"ami-fce3c696\", " +
                "\"bound_account_id\": 1234567890123, \"bound_region\": \"us-east-2\",\"bound_vpc_id\": " +
                "\"vpc-2f09a348\", \"bound_subnet_id\": \"subnet-1122aabb\", \"bound_iam_role_arn\": " +
                "\"arn:aws:iam::8987887:role/test-role\",  \"bound_iam_instance_profile_arn\":" +
                "\"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\",  " +
                "\"policies\": \"\\\"[prod, dev\\\"]\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        when(reqProcessor.process("/auth/aws/roles/create", jsonStr, token)).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSON(awsLoginRole)).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(responseNoContent);
        ResponseEntity<String> responseEntity = null;
        try {
            when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(true);
            responseEntity = awsAuthService.createRole(token, awsLoginRole, userDetails);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createRole_failure_revert() {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String jsonStr = "{\"auth_type\": \"ec2\", \"role\": \"mytestawsrole\", \"bound_ami_id\": \"ami-fce3c696\", " +
                "\"bound_account_id\": 1234567890123, \"bound_region\": \"us-east-2\",\"bound_vpc_id\": " +
                "\"vpc-2f09a348\", \"bound_subnet_id\": \"subnet-1122aabb\", \"bound_iam_role_arn\": " +
                "\"arn:aws:iam::8987887:role/test-role\",  \"bound_iam_instance_profile_arn\":" +
                "\"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\",  " +
                "\"policies\": \"\\\"[prod, dev\\\"]\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS role creation failed.\"]}");
        when(reqProcessor.process("/auth/aws/roles/create", jsonStr, token)).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSON(awsLoginRole)).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(false);
        when(reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+awsLoginRole.getRole()+"\"}",token)).thenReturn(responseNoContent);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(responseNoContent);
        ResponseEntity<String> responseEntity = null;
        try {
            when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(true);
            responseEntity = awsAuthService.createRole(token, awsLoginRole, userDetails);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createRole_revert_failure() {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response response500 = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String jsonStr = "{\"auth_type\": \"ec2\", \"role\": \"mytestawsrole\", \"bound_ami_id\": \"ami-fce3c696\", " +
                "\"bound_account_id\": 1234567890123, \"bound_region\": \"us-east-2\",\"bound_vpc_id\": " +
                "\"vpc-2f09a348\", \"bound_subnet_id\": \"subnet-1122aabb\", \"bound_iam_role_arn\": " +
                "\"arn:aws:iam::8987887:role/test-role\",  \"bound_iam_instance_profile_arn\":" +
                "\"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\",  " +
                "\"policies\": \"\\\"[prod, dev\\\"]\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS role created however metadata update failed. Please try with AWS role/update \"]}");
        when(reqProcessor.process("/auth/aws/roles/create", jsonStr, token)).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSON(awsLoginRole)).thenReturn(jsonStr);
        UserDetails userDetails = getMockUser(true);
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(false);
        when(reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+awsLoginRole.getRole()+"\"}",token)).thenReturn(response500);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(responseNoContent);
        ResponseEntity<String> responseEntity = null;
        try {
            when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(true);
            responseEntity = awsAuthService.createRole(token, awsLoginRole, userDetails);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = TVaultValidationException.class)
    public void test_createRole_failure_400() throws TVaultValidationException{

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> responseEntity = null;
        when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(false);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        UserDetails userDetails = getMockUser(true);
        when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(responseNoContent);
        responseEntity = awsAuthService.createRole(token, awsLoginRole, userDetails);
    }

    @Test
    public void test_createRole_failure_500() {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        Response responseServerError = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Internal Server Error\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String jsonStr = "{\"auth_type\": \"ec2\", \"role\": \"mytestawsrole\", \"bound_ami_id\": \"ami-fce3c696\", " +
                "\"bound_account_id\": 1234567890123, \"bound_region\": \"us-east-2\",\"bound_vpc_id\": " +
                "\"vpc-2f09a348\", \"bound_subnet_id\": \"subnet-1122aabb\", \"bound_iam_role_arn\": " +
                "\"arn:aws:iam::8987887:role/test-role\",  \"bound_iam_instance_profile_arn\":" +
                "\"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\",  " +
                "\"policies\": \"\\\"[prod, dev\\\"]\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Internal Server Error\"]}");
        when(reqProcessor.process("/auth/aws/roles/create", jsonStr, token)).thenReturn(responseServerError);

        when(JSONUtil.getJSON(awsLoginRole)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = null;
        try {
            when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(true);
            Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
            UserDetails userDetails = getMockUser(true);
            when(reqProcessor.process(eq("/write"),Mockito.any(),eq(token))).thenReturn(responseNoContent);
            responseEntity = awsAuthService.createRole(token, awsLoginRole, userDetails);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateRole_successfully() {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);

        String roleName = "mytestawsrole";
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\": [ \"\\\"[prod\",\"dev\\\"]\" ]}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String jsonStr = "{\"auth_type\": \"ec2\", \"role\": \"mytestawsrole\", \"bound_ami_id\": \"ami-fce3c696\", " +
                "\"bound_account_id\": 1234567890123, \"bound_region\": \"us-east-2\",\"bound_vpc_id\": " +
                "\"vpc-2f09a348\", \"bound_subnet_id\": \"subnet-1122aabb\", \"bound_iam_role_arn\": " +
                "\"arn:aws:iam::8987887:role/test-role\",  \"bound_iam_instance_profile_arn\":" +
                "\"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\",  " +
                "\"policies\": \"\\\"[prod, dev\\\"]\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");

        when(reqProcessor.process("/auth/aws/roles", "{\"role\":\"" + roleName + "\"}", token)).thenReturn(readResponse);
        when(reqProcessor.process("/auth/aws/roles/delete", jsonStr, token)).thenReturn(responseNoContent);
        when(reqProcessor.process("/auth/aws/roles/update", jsonStr, token)).thenReturn(responseNoContent);

        when(ControllerUtil.updateMetaDataOnConfigChanges(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSON(awsLoginRole)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = null;
        try {
            when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(true);
            responseEntity = awsAuthService.updateRole(token, awsLoginRole);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = TVaultValidationException.class)
    public void test_updateRole_failure_400() throws TVaultValidationException{

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> responseEntity = null;
        when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(false);
        responseEntity = awsAuthService.updateRole(token, awsLoginRole);
    }

    @Test
    public void test_updateRole_failure_404() {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        String roleName = "mytestawsrole";

        Response responseError = getMockResponse(HttpStatus.BAD_REQUEST, true, "{\"messages\":[\"Update failed . AWS Role does not exist \"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        String jsonStr = "{\"auth_type\": \"ec2\", \"role\": \"mytestawsrole\", \"bound_ami_id\": \"ami-fce3c696\", " +
                "\"bound_account_id\": 1234567890123, \"bound_region\": \"us-east-2\",\"bound_vpc_id\": " +
                "\"vpc-2f09a348\", \"bound_subnet_id\": \"subnet-1122aabb\", \"bound_iam_role_arn\": " +
                "\"arn:aws:iam::8987887:role/test-role\",  \"bound_iam_instance_profile_arn\":" +
                "\"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\",  " +
                "\"policies\": \"\\\"[prod, dev\\\"]\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Update failed . AWS Role does not exist \"]}");
        when(reqProcessor.process("/auth/aws/roles", "{\"role\":\"" + roleName + "\"}", token)).thenReturn(responseError);

        when(JSONUtil.getJSON(awsLoginRole)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = null;
        try {
            when(ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)).thenReturn(true);
            responseEntity = awsAuthService.updateRole(token, awsLoginRole);
        } catch (TVaultValidationException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteRole_successfully() {

        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role deleted \"]}");
        when(reqProcessor.process("/auth/aws/roles/delete", "{\"role\":\"mytestawsrole\"}", token)).thenReturn(response);
        UserDetails userDetails = getMockUser(false);
        String metadatajson = "{ \"path\": \"metadata/awsrole/mytestawsrole\", \"data\": {\"createdBy\":\"normaluser\"}}";
        when(ControllerUtil.populateAWSMetaJson("mytestawsrole", userDetails.getUsername())).thenReturn(metadatajson);
        when(reqProcessor.process("/delete",metadatajson,token)).thenReturn(response);
        Response permissonResponse = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole("mytestawsrole", token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH)).thenReturn(permissonResponse);
        ResponseEntity<String> responseEntity = awsAuthService.deleteRole(token, "mytestawsrole", userDetails);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_readRole_successfully() {
        String roleName = "mytestawsrole";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\": [ \"\\\"[prod\",\"dev\\\"]\" ]}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        String jsoninput = "{\"role\":\"" + roleName + "\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseBody);
        when(reqProcessor.process("/auth/aws/roles", jsoninput, token)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = awsAuthService.fetchRole(token, "mytestawsrole");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_listRoles_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseBody = "{ \"keys\": [\"mytestawsrole\"]}";
        Response listResponse = getMockResponse(HttpStatus.OK, true, responseBody);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseBody);
        when(reqProcessor.process("/auth/aws/roles/list", "{}", token)).thenReturn(listResponse);

        ResponseEntity<String> responseEntity = awsAuthService.listRoles(token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_configureClient_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSClientConfiguration awsClientConfiguration = new AWSClientConfiguration();
        awsClientConfiguration.setAccess_key("accesskey");
        awsClientConfiguration.setSecret_key("secretKey");

        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String jsonStr = "{ \"access_key\": \"string\", \"secret_key\": \"string\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Client successfully configured \"]}");
        when(reqProcessor.process("/auth/aws/config/configureclient", jsonStr, token)).thenReturn(response);
        when(JSONUtil.getJSON(awsClientConfiguration)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = awsAuthService.configureClient(awsClientConfiguration, token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_readClientConfiguration_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseBody = "{ \"access_key\": \"string\", \"secret_key\": null}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseBody);
        when(reqProcessor.process("/auth/aws/config/readclientconfig", "{}", token)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = awsAuthService.readClientConfiguration(token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_createSTSRole_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AWSStsRole awsStsRole = new AWSStsRole();
        awsStsRole.setAccount_id("account_id1");
        awsStsRole.setSts_role("sts_role1");

        String jsonStr = "{ \"account_id\": \"account_id1\", \"sts_role\": \"sts_role1\"}";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"STS Role created successfully \"]}");

        when(reqProcessor.process("/auth/aws/config/sts/create",jsonStr, token)).thenReturn(response);
        when(JSONUtil.getJSON(awsStsRole)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntity = awsAuthService.createSTSRole(awsStsRole, token);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_authenticateIAM_failure_500() {

        String jsonStr = "{\n" +
                "  \"iam_http_request_method\": \"POST\",\n" +
                "  \"iam_request_body\": \"{}\",\n" +
                "  \"iam_request_headers\": \"{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}\",\n" +
                "  \"iam_request_url\": \"http://testurl.com\",\n" +
                "  \"role\": \"testawsrole\",\n" +
                "  \"pkcs7\": \"MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====\"\n" +
                "}";
        AWSIAMLogin awsiamLogin = new AWSIAMLogin();
        awsiamLogin.setIam_http_request_method("POST");
        awsiamLogin.setIam_request_body("{}");
        awsiamLogin.setIam_request_headers("{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}");
        awsiamLogin.setIam_request_url("http://testurl.com");
        awsiamLogin.setRole("testawsrole");
        awsiamLogin.setPkcs7("MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx");

        String responseBody = "{  \"errors\": [ \"failed to base64 decode iam_request_url\" ]}";
        Response response = getMockResponse(HttpStatus.BAD_REQUEST, false, responseBody);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);

        when(JSONUtil.getJSON(awsiamLogin)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/aws/iam/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntity = awsAuthService.authenticateIAM(awsiamLogin);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_authenticate_AWS_EC2_successfully() {

        String jsonLoginStr = "{\n" +
                "  \"role\": \"testawsrole\",\n" +
                "  \"pkcs7\": \"MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====\"\n" +
                "}";
        AWSAuthLogin awsAuthLogin = new AWSIAMLogin();
        awsAuthLogin.setIam_http_request_method("POST");
        awsAuthLogin.setIam_request_body("{}");
        awsAuthLogin.setIam_request_headers("{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}");
        awsAuthLogin.setIam_request_url("http://testurl.com");
        awsAuthLogin.setRole("testawsrole");
        awsAuthLogin.setPkcs7("MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====");

        Response response = getMockResponse(HttpStatus.NO_CONTENT, false, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NO_CONTENT).body("{} \t");

        AWSLogin login = new AWSLogin();
        login.setPkcs7(awsAuthLogin.getPkcs7());
        login.setRole(awsAuthLogin.getRole());
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonLoginStr);

        when(ControllerUtil.areAwsLoginInputsValid(AWSAuthType.EC2, awsAuthLogin)).thenReturn(true);
        when(reqProcessor.process(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(response);

        ResponseEntity<String> responseEntity = awsAuthService.authenticate(AWSAuthType.EC2, awsAuthLogin);

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }

    @Test
    public void test_authenticate_AWS_IAM_successfully() {

        String jsonLoginStr = "{\n" +
                "  \"role\": \"testawsrole\",\n" +
                "  \"pkcs7\": \"MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====\"\n" +
                "}";
        String jsonStr = "{\n" +
                "  \"iam_http_request_method\": \"POST\",\n" +
                "  \"iam_request_body\": \"{}\",\n" +
                "  \"iam_request_headers\": \"{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}\",\n" +
                "  \"iam_request_url\": \"http://testurl.com\",\n" +
                "  \"role\": \"testawsrole\",\n" +
                "  \"pkcs7\": \"MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====\"\n" +
                "}";
        AWSAuthLogin awsAuthLogin = new AWSIAMLogin();
        awsAuthLogin.setIam_http_request_method("POST");
        awsAuthLogin.setIam_request_body("{}");
        awsAuthLogin.setIam_request_headers("{\"token\":\"4qJC0tWjMDIKjRDDmtcUAZBt\"}");
        awsAuthLogin.setIam_request_url("http://testurl.com");
        awsAuthLogin.setRole("testawsrole");
        awsAuthLogin.setPkcs7("MIIBjwYJKoZIhvcNAQcDoIIBgDCCAXwCAQAxggE4MIIBNAIBADCBnDCBlDELMAkGA1UEBhMCWkEx====");

        Response response = getMockResponse(HttpStatus.NO_CONTENT, false, "");
        Response responseOk = getMockResponse(HttpStatus.OK, false, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{} \t");

        AWSLogin login = new AWSLogin();
        login.setPkcs7(awsAuthLogin.getPkcs7());
        login.setRole(awsAuthLogin.getRole());


        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/aws/iam/login",jsonStr,"")).thenReturn(responseOk);

        when(ControllerUtil.areAwsLoginInputsValid(AWSAuthType.IAM, awsAuthLogin)).thenReturn(true);
        //when(reqProcessor.process(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(response);

        ResponseEntity<String> responseEntity = awsAuthService.authenticate(AWSAuthType.IAM, awsAuthLogin);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }
}
/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */
package com.tmobile.cso.vault.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.model.AWSIAMRole;
import com.tmobile.cso.vault.api.model.AWSLoginRole;
import com.tmobile.cso.vault.api.model.CertificateAWSRole;
import com.tmobile.cso.vault.api.model.CertificateAWSRoleRequest;
import com.tmobile.cso.vault.api.model.CertificateData;
import com.tmobile.cso.vault.api.model.SSLCertificateMetadataDetails;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.CertificateUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;
import com.tmobile.cso.vault.api.validator.TokenValidator;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class,EntityUtils.class,HttpClientBuilder.class, OIDCUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class SSLCertificateAWSRoleServiceTest {

    private MockMvc mockMvc;

    @Mock
    DirectoryService directoryService;

    @Mock
    LdapTemplate ldapTemplate;

    @InjectMocks
    SSLCertificateAWSRoleService sslCertificateAWSRoleService;

    @Mock
    ControllerUtil controllerUtil;

    @Mock
    private RequestProcessor reqProcessor;

    @Mock
    UserDetails userDetails;

    @Mock
    VaultAuthService vaultAuthService;

    @Mock
    PolicyUtils policyUtils;

    @Mock
    TokenValidator tokenValidator;

    String token;

    @Mock
    CertificateData certificateData;

    @Mock
    private WorkloadDetailsService workloadDetailsService;

    @Mock
    CertificateUtils certificateUtils;

    @Mock
    ObjectMapper obj;

    @Mock
    TokenUtils tokenUtils;

    @Mock
    AWSAuthService awsAuthService;

    @Mock
    AWSIAMAuthService awsiamAuthService;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(OIDCUtil.class, "log", LogManager.getLogger(OIDCUtil.class));
        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/v2/sslcert");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
        ReflectionTestUtils.setField(sslCertificateAWSRoleService, "certificateNameTailText", ".t-mobile.com");
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
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        return userDetails;
    }

    SSLCertificateMetadataDetails getSSLCertificateMetadataDetails() {
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("testuser1");
        certDetails.setCertificateName("certificatename.t-mobile.com");
        certDetails.setCertOwnerNtid("testuser1");
        certDetails.setCertOwnerEmailId("owneremail@test.com");
        certDetails.setExpiryDate("10-20-2030");
        certDetails.setCreateDate("10-20-2030");
        return certDetails;
    }

    SSLCertificateMetadataDetails getSSLExternalCertificateRequest() {
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("external");
        certDetails.setCertCreatedBy("testuser1");
        certDetails.setCertificateName("certificatename.t-mobile.com");
        certDetails.setCertOwnerNtid("testuser1");
        certDetails.setCertOwnerEmailId("owneremail@test.com");
        certDetails.setExpiryDate("10-20-2030");
        certDetails.setRequestStatus(SSLCertificateConstants.REQUEST_PENDING_APPROVAL);
        return certDetails;
    }

    @Test
    public void testCreateAWSRoleForSSLSuccess() throws Exception {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");
        when(awsAuthService.createRole(token, awsLoginRole, userDetails)).thenReturn(responseEntityExpected);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.createAWSRoleForSSL(userDetails, token, awsLoginRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testCreateIAMRoleForSSLSuccess() throws Exception {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");
        when(awsiamAuthService.createIAMRole(awsiamRole, token, userDetails)).thenReturn(responseEntityExpected);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.createIAMRoleForSSL(userDetails, token, awsiamRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testAddAwsIAMRoleToSSLCertificateSuccssfully() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with SSL Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testAddAwsEC2RoleToSSLCertificateSuccssfully() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role successfully associated with SSL Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "external");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "external")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testAddAwsEC2RoleToSSLCertificateMetadataFailure() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testAddAwsIAMRoleToSSLCertificateMetadataFailure() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testAddAwsRoleToSSLCertificateFailure() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed. Try Again\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testAddAwsRoleToCertificateRoleNotExistsFailure() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response awsRoleResponse = getMockResponse(HttpStatus.UNPROCESSABLE_ENTITY, true, "");
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntityActual.getStatusCode());
    }

    @Test
	public void testAddAwsRoleToSSLCertificateFailure403() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add AWS Role to this certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "internal");
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(false);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testAddAwsRoleToSSLCertificateInitialValidationFailure() throws Exception {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "inte");
        when(tokenUtils.getSelfServiceToken()).thenReturn("5PDrOhsy4ig8L3EpsJZSLAMg");
        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails)).thenReturn(policies);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(userDetails, token, certificateAWSRole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testAddAwsRoleToSSLCertificateUserNotExists() throws Exception {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add AWS Role to this certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRole certificateAWSRole = new CertificateAWSRole("certificatename.t-mobile.com", "role1", "read", "internal");
        when(tokenUtils.getSelfServiceToken()).thenReturn("5PDrOhsy4ig8L3EpsJZSLAMg");
        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails)).thenReturn(policies);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.addAwsRoleToSSLCertificate(null, token, certificateAWSRole);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testRemoveAWSIAMRoleFromCertificateSuccssfully() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from SSL Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"iam\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsiamAuthService.configureAWSIAMRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(userDetails, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testRemoveAwsRoleToSSLCertificateInitialValidationFailure() throws Exception {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "inter");
        when(tokenUtils.getSelfServiceToken()).thenReturn("5PDrOhsy4ig8L3EpsJZSLAMg");
        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails)).thenReturn(policies);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(userDetails, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void testRemoveAwsRoleToSSLCertificateEmptyUserDetail() throws Exception {
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove AWS role from this certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "internal");
        when(tokenUtils.getSelfServiceToken()).thenReturn("5PDrOhsy4ig8L3EpsJZSLAMg");
        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails)).thenReturn(policies);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(null, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testRemoveAWSEC2RoleFromSSLCertificateSuccssfully() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLExternalCertificateRequest();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role is successfully removed from SSL Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "external");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "external")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(userDetails, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testRemoveAWSRoleFromSSLCertificateMetadataFailure() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS Role configuration failed. Please try again\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(userDetails, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testRemoveAWSRoleFromSSLCertificateFailure() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove AWS Role from the SSL Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"w_svcacct_testsvcname\" ], \"auth_type\":\"ec2\"}";
        Response awsRoleResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(userDetails, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testRemoveAWSRoleFromSSLRoleNotExists() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"AWS Role doesn't exist\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "internal");

        String [] policies = {"o_externalcerts_certificatename.t-mobile.com"};
        when(policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails)).thenReturn(policies);
        Response awsRoleResponse = getMockResponse(HttpStatus.UNPROCESSABLE_ENTITY, true, "");
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"role1\"}",token)).thenReturn(awsRoleResponse);
        Response configureAWSRoleResponse = getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        when(awsAuthService.configureAWSRole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAWSRoleResponse);
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(userDetails, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
	public void testRemoveAWSRoleFromSSLCertificateFailure403() throws Exception {
		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove AWS Role from SSL Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateAWSRoleRequest certificateAWSRoleRequest = new CertificateAWSRoleRequest("certificatename.t-mobile.com", "role1", "internal");
        when(tokenUtils.getSelfServiceToken()).thenReturn(token);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(false);
        ResponseEntity<String> responseEntityActual =  sslCertificateAWSRoleService.removeAWSRoleFromSSLCertificate(userDetails, token, certificateAWSRoleRequest);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }
}

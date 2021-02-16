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
package com.tmobile.cso.vault.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.SSLCertificateMetadataDetails;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class, ControllerUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class CertificateUtilsTest {
	
    @InjectMocks
    CertificateUtils certificateUtils;

    @Mock
    RequestProcessor reqProcessor;

    @Mock
    Response response;
    
    private static final String CERT_NAME = "certificatename.t-mobile.com";
    private static final String GET_CERT_DETAIL = "/certmanager";
    private static final String GET_CERT_DETAIL_VAL = "{\"path\":\"";
    private static final String ADMIN_USER = "testuser1";

    @Before
    public void setUp() {
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(ControllerUtil.class);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        Whitebox.setInternalState(ControllerUtil.class, "reqProcessor", reqProcessor);
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
        when(ControllerUtil.getReqProcessor()).thenReturn(reqProcessor);
        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }


    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        response.setResponse("");
        if (!StringUtils.isEmpty(expectedBody)) {
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
    
    SSLCertificateMetadataDetails getSSLCertificateMetadataDetails() {        
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setAkmid("103001");
        certDetails.setApplicationName("tvt");
        certDetails.setApplicationTag("T-Vault");
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy(ADMIN_USER);
        certDetails.setCertificateName(CERT_NAME);
        certDetails.setCertOwnerNtid(ADMIN_USER);
        certDetails.setCertOwnerEmailId("owneremail@test.com");
        certDetails.setApplicationOwnerEmailId("appowneremail@test.com");
        certDetails.setNotificationEmails("appowneremail@test.com");
        certDetails.setCreateDate("2020-06-24");
        certDetails.setExpiryDate("2021-06-24");
        certDetails.setCertificateStatus("Active");
        certDetails.setActionId(2);
        return certDetails;
    } 


    @Test
    public void testHasAddOrRemovePermissionForAdminSuccessfully() {
        UserDetails userDetails = getMockUser(true);
        userDetails.setUsername(ADMIN_USER);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        boolean canAdd = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata);
        assertTrue(canAdd);
    }
    
    @Test
    public void testHasAddOrRemovePermissionForAdminFailed() {
        UserDetails userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        boolean canAdd = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata);
        assertFalse(canAdd);
    }
    
    @Test
    public void testHasAddOrRemovePermissionForAdminIfEmptyOwner() {
        UserDetails userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        certificateMetadata.setCertOwnerNtid(null);
        boolean canAdd = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata);
        assertTrue(canAdd);
    }
    
    @Test
    public void testHasAddOrRemovePermissionForNonAdminSuccessfully() {
        UserDetails userDetails = getMockUser(false);
        userDetails.setUsername(ADMIN_USER);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        boolean canAdd = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata);
        assertTrue(canAdd);
    }

    @Test
    public void testHasAddOrRemovePermissionForNormalUserSuccessfully() {
        UserDetails userDetails = getMockUser(false);
        userDetails.setUsername(null);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        boolean canAdd = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata);
        assertFalse(canAdd);
    }
    
    @Test
    public void testHasAddOrRemovePermissionForNonAdminFailed() {
        UserDetails userDetails = getMockUser(false);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        boolean canAdd = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata);
        assertFalse(canAdd);
    }
    
    @Test
    public void testHasAddOrRemovePermissionForEmptyMetadata() {
        UserDetails userDetails = getMockUser(false);
        SSLCertificateMetadataDetails certificateMetadata = null;
        boolean canAdd = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata);
        assertFalse(canAdd);
    }
    
    @Test
    public void testGetCertificateMetaDataSuccessfully() {
        UserDetails userDetails = getMockUser(true);
        userDetails.setUsername(ADMIN_USER);        
        String certificatePath = SSLCertificateConstants.SSL_CERT_PATH + '/' + CERT_NAME;

		Response responseObj = getMockResponse(HttpStatus.OK, true,
				"{ \"data\": {\"akmid\":\"103001\",\"applicationName\":\"tvt\", "
						+ " \"applicationOwnerEmailId\":\"appowneremail@test.com\", \"applicationTag\":\"T-Vault\", "
						+ " \"authority\":\"T-Mobile Issuing CA 01 - SHA2\", \"certCreatedBy\": \"testuser1\", "
						+ " \"certOwnerEmailId\":\"owneremail@test.com\", \"certOwnerNtid\": \"testuser1\", \"certType\": \"internal\", "
						+ " \"certificateId\":\"62765\",\"certificateName\":\"certificatename.t-mobile.com\", "
						+ " \"certificateStatus\":\"Active\", \"containerName\":\"VenafiBin_12345\", "
						+ " \"createDate\":\"2020-06-24\", \"expiryDate\":\"2021-06-24\", "
						+ " \"dnsNames\":[\"test.t-mobile.com, test1.t-mobile.com, certtestest.t-mobile.com\"],"
						+ " \"projectLeadEmailId\":\"project@email.com\", \"notificationEmail\":\"appowneremail@test.com\", \"actionId\":2}}");

        when(ControllerUtil.getReqProcessor().process(GET_CERT_DETAIL, GET_CERT_DETAIL_VAL +certificatePath+"\"}",userDetails.getClientToken())).thenReturn(responseObj);
        
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        String certType = "internal";
        SSLCertificateMetadataDetails certificateMetadataObj = certificateUtils.getCertificateMetaData(userDetails.getClientToken(),CERT_NAME, certType);
        assertEquals(certificateMetadataObj.getCertificateName(), certificateMetadata.getCertificateName());
        assertEquals(certificateMetadataObj.getAkmid(), certificateMetadata.getAkmid());
        assertEquals(certificateMetadataObj.getApplicationName(), certificateMetadata.getApplicationName());
        assertEquals(certificateMetadataObj.getCreateDate(), certificateMetadata.getCreateDate());
        assertEquals(certificateMetadataObj.getApplicationTag(), certificateMetadata.getApplicationTag());
        assertEquals(certificateMetadataObj.getCertOwnerNtid(), certificateMetadata.getCertOwnerNtid());
        assertEquals(certificateMetadataObj.getNotificationEmails(), certificateMetadata.getNotificationEmails());
        assertEquals(certificateMetadataObj.getActionId(), certificateMetadata.getActionId());
    }

	@Test
	public void testGetCertificateMetaDataEmptySuccessfully() {
		UserDetails userDetails = getMockUser(true);
		userDetails.setUsername(ADMIN_USER);
		String certificatePath = SSLCertificateConstants.SSL_CERT_PATH + '/' + CERT_NAME;

		Response responseObj = getMockResponse(HttpStatus.OK, true,
				"{ \"data\": {\"certificateId\":\"62765\",\"certificateName\":\"certificatename.t-mobile.com\"}}");

		when(ControllerUtil.getReqProcessor().process(GET_CERT_DETAIL, GET_CERT_DETAIL_VAL + certificatePath + "\"}",
				userDetails.getClientToken())).thenReturn(responseObj);

		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
		String certType = "internal";
		SSLCertificateMetadataDetails certificateMetadataObj = certificateUtils
				.getCertificateMetaData(userDetails.getClientToken(), CERT_NAME, certType);
		assertEquals(certificateMetadataObj.getCertificateName(), certificateMetadata.getCertificateName());
	}

	@Test
	public void testGetExternalCertificateMetaDataSuccessfully() {
		UserDetails userDetails = getMockUser(true);
		userDetails.setUsername(ADMIN_USER);
		String certificatePath = SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + '/' + CERT_NAME;

		Response responseObj = getMockResponse(HttpStatus.OK, true,
				"{ \"data\": {\"akmid\":\"103001\",\"applicationName\":\"tvt\", "
						+ " \"applicationOwnerEmailId\":\"appowneremail@test.com\", \"applicationTag\":\"T-Vault\", "
						+ " \"authority\":\"T-Mobile Issuing CA 01 - SHA2\", \"certCreatedBy\": \"testuser1\", "
						+ " \"certOwnerEmailId\":\"owneremail@test.com\", \"certOwnerNtid\": \"testuser1\", \"certType\": \"external\", "
						+ " \"certificateId\":\"62765\",\"certificateName\":\"certificatename.t-mobile.com\", "
						+ " \"certificateStatus\":\"Active\", \"containerName\":\"VenafiBin_12345\", \"containerId\": \"99\", "
						+ " \"createDate\":\"2020-06-24\", \"expiryDate\":\"2021-06-24\", \"requestStatus\": \"Approved\", "
                        + " \"dnsNames\":\"test.t-mobile.com, test1.t-mobile.com, certtestest.t-mobile.com\","
						+ " \"projectLeadEmailId\":\"project@email.com\"}}");

		when(ControllerUtil.getReqProcessor().process(GET_CERT_DETAIL, GET_CERT_DETAIL_VAL + certificatePath + "\"}",
				userDetails.getClientToken())).thenReturn(responseObj);

		SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
		certificateMetadata.setRequestStatus("Approved");
		certificateMetadata.setCertType("external");
		certificateMetadata.setContainerId(99);
		String certType = "external";
		SSLCertificateMetadataDetails certificateMetadataObj = certificateUtils
				.getCertificateMetaData(userDetails.getClientToken(), CERT_NAME, certType);
		assertEquals(certificateMetadataObj.getCertificateName(), certificateMetadata.getCertificateName());
		assertEquals(certificateMetadataObj.getAkmid(), certificateMetadata.getAkmid());
		assertEquals(certificateMetadataObj.getApplicationName(), certificateMetadata.getApplicationName());
		assertEquals(certificateMetadataObj.getCreateDate(), certificateMetadata.getCreateDate());
		assertEquals(certificateMetadataObj.getApplicationTag(), certificateMetadata.getApplicationTag());
		assertEquals(certificateMetadataObj.getCertOwnerNtid(), certificateMetadata.getCertOwnerNtid());
		assertEquals(certificateMetadataObj.getRequestStatus(), certificateMetadata.getRequestStatus());
		assertEquals(certificateMetadataObj.getContainerId(), certificateMetadata.getContainerId());
	}

    @Test(expected = Exception.class)
    public void testGetCertificateMetaDataFailed() {
        UserDetails userDetails = getMockUser(true);
        userDetails.setUsername(ADMIN_USER);        
        String certificatePath = SSLCertificateConstants.SSL_CERT_PATH + '/' + CERT_NAME;
        
        Response responseObj = getMockResponse(HttpStatus.OK, true, "{ \"data\": [\"akmid\":\"103001\",\"applicationName\":\"tvt\", "
          		+ " \"applicationOwnerEmailId\":\"appowneremail@test.com\", \"applicationTag\":\"T-Vault\", "
          		+ " \"authority\":\"T-Mobile Issuing CA 01 - SHA2\", \"certCreatedBy\": \"testuser1\", "
          		+ " \"certOwnerEmailId\":\"owneremail@test.com\", \"certOwnerNtid\": \"testuser1\", \"certType\": \"internal\", "
          		+ " \"certificateId\":\"62765\",\"certificateName\":\"certificatename.t-mobile.com\", "
          		+ " \"certificateStatus\":\"Active\", \"containerName\":\"VenafiBin_12345\", "
          		+ " \"createDate\":\"2020-06-24\", \"expiryDate\":\"2021-06-24\", "
          		+ " \"projectLeadEmailId\":\"project@email.com\"]}");
         
        when(ControllerUtil.getReqProcessor().process(GET_CERT_DETAIL, GET_CERT_DETAIL_VAL +certificatePath+"\"}",userDetails.getClientToken())).thenReturn(responseObj);
        String certType = "internal";
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        SSLCertificateMetadataDetails certificateMetadataObj = certificateUtils.getCertificateMetaData(userDetails.getClientToken(),CERT_NAME,certType);
        assertEquals(certificateMetadataObj.getCertificateName(), certificateMetadata.getCertificateName());
    }
    
    @Test
    public void testGetCertificateMetaDataInvalidResponse() {
        UserDetails userDetails = getMockUser(true);
        userDetails.setUsername(ADMIN_USER);        
        String certType = "internal";
        String certificatePath = SSLCertificateConstants.SSL_CERT_PATH  + '/' + CERT_NAME;       
        Response responseObj = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{}");         
        when(ControllerUtil.getReqProcessor().process(GET_CERT_DETAIL, GET_CERT_DETAIL_VAL +certificatePath+"\"}",userDetails.getClientToken())).thenReturn(responseObj);
        SSLCertificateMetadataDetails certificateMetadataObj = certificateUtils.getCertificateMetaData(userDetails.getClientToken(),CERT_NAME,certType);
        assertEquals(certificateMetadataObj, null);
    }
    
}

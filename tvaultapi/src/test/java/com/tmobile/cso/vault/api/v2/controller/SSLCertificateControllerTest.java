package com.tmobile.cso.vault.api.v2.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.service.SSLCertificateService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class SSLCertificateControllerTest {
    @Mock
    public SSLCertificateService sslCertificateService;

    private MockMvc mockMvc;

    @Mock
    RequestProcessor reqProcessor;

    @InjectMocks
    public SSLCertificateController SslCertificateController;

    @Mock
    private SSLCertificateRequest sSLCertificateRequest;
    
    @Mock
    private SSLCertificateMetadataDetails sSLCertificateMetadataRequest;
    
    @Mock
    private RevocationRequest revocationRequest;
    @Mock
    UserDetails userDetails;

    @Mock
    HttpServletRequest httpServletRequest;
    String token;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(SslCertificateController).build();
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
    }

    @Test
    public void test_authenticate_successful() throws Exception {
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(sslCertificateService.authenticate(certManagerLoginRequest)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        assertEquals(HttpStatus.OK, SslCertificateController.authenticate(certManagerLoginRequest).getStatusCode());
    }

    @Test
    public void test_authenticate_Unauthorized() throws Exception {
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername1", "testpassword1");
        when(sslCertificateService.authenticate(certManagerLoginRequest)).thenReturn(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        assertEquals(HttpStatus.UNAUTHORIZED, SslCertificateController.authenticate(certManagerLoginRequest).getStatusCode());
    }

    @Test
    public void test_generateSSLCertificate_success() {
        TargetSystem targetSystem = new TargetSystem();
        targetSystem.setAddress("Target System address");
        targetSystem.setDescription("Target System Description");
        targetSystem.setName("Target Name");

        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setHostname("Target System Service Host name");
        targetSystemServiceRequest.setName("Target System Service Name");
        targetSystemServiceRequest.setPort(443);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(false);
        targetSystemServiceRequest.setMonitoringEnabled(false);
        targetSystemServiceRequest.setDescription("Target Service Description");

        sSLCertificateRequest.setCertificateName("CertificateName");
        sSLCertificateRequest.setTargetSystem(targetSystem);
        sSLCertificateRequest.setTargetSystemServiceRequest(targetSystemServiceRequest);


       when(sslCertificateService.generateSSLCertificate(sSLCertificateRequest,userDetails,token)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
       assertEquals(HttpStatus.OK, sslCertificateService.generateSSLCertificate(sSLCertificateRequest,userDetails,token).getStatusCode());
    }

    @Test
    public void test_generateSSLCertificate_success_Test() {
        TargetSystem targetSystem = new TargetSystem();
        targetSystem.setAddress("Target System address");
        targetSystem.setDescription("Target System Description");
        targetSystem.setName("Target Name");

        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setHostname("Target System Service Host name");
        targetSystemServiceRequest.setName("Target System Service Name");
        targetSystemServiceRequest.setPort(443);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(false);
        targetSystemServiceRequest.setMonitoringEnabled(false);
        targetSystemServiceRequest.setDescription("Target Service Description");

        sSLCertificateRequest.setCertificateName("CertificateName");
        sSLCertificateRequest.setTargetSystem(targetSystem);
        sSLCertificateRequest.setTargetSystemServiceRequest(targetSystemServiceRequest);


        when(sslCertificateService.generateSSLCertificate(sSLCertificateRequest, userDetails, token)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(httpServletRequest.getAttribute("UserDetails")).thenReturn(userDetails);
        assertEquals(HttpStatus.OK, SslCertificateController.generateSSLCertificate(httpServletRequest, token, sSLCertificateRequest).getStatusCode());
    }

    @Test
    public void test_generateSSLCertificate_Error() {
        TargetSystem targetSystem = new TargetSystem();
        targetSystem.setAddress("Target System address");
        targetSystem.setDescription("Target System Description");
        targetSystem.setName("Target Name");

        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setHostname("Target System Service Host name");
        targetSystemServiceRequest.setName("Target System Service Name");
        targetSystemServiceRequest.setPort(443);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(false);
        targetSystemServiceRequest.setMonitoringEnabled(false);
        targetSystemServiceRequest.setDescription("Target Service Description");

        sSLCertificateRequest.setCertificateName("CertificateName");
        sSLCertificateRequest.setTargetSystem(targetSystem);
        sSLCertificateRequest.setTargetSystemServiceRequest(targetSystemServiceRequest);

        when(sslCertificateService.generateSSLCertificate(sSLCertificateRequest,userDetails,token)).thenReturn(new ResponseEntity<>
             (HttpStatus.INTERNAL_SERVER_ERROR));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                sslCertificateService.generateSSLCertificate(sSLCertificateRequest,userDetails,token).getStatusCode());

    }
    
    @Test
    public void test_getCertificates() throws Exception {
        // Mock response        
        when(sslCertificateService.getServiceCertificates("5PDrOhsy4ig8L3EpsJZSLAMg", userDetails, "",1,0,"internal")).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        assertEquals(HttpStatus.OK, sslCertificateService.getServiceCertificates("5PDrOhsy4ig8L3EpsJZSLAMg",userDetails,"",1,0,"internal").getStatusCode());
    }
    
    @Test
    public void test_getCertificates_external() throws Exception {
        // Mock response        
        when(sslCertificateService.getServiceCertificates("5PDrOhsy4ig8L3EpsJZSLAMg", userDetails, "",1,0,"external")).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        assertEquals(HttpStatus.OK, sslCertificateService.getServiceCertificates("5PDrOhsy4ig8L3EpsJZSLAMg",userDetails,"",1,0,"external").getStatusCode());
    }
    
	@Test
	public void test_getRevocationReasons_Success() {
		Integer certifcateId = 56123;

		when(sslCertificateService.getRevocationReasons(certifcateId, token))
				.thenReturn(new ResponseEntity<>(HttpStatus.OK));
		assertEquals(HttpStatus.OK,
				SslCertificateController.getRevocationReasons(httpServletRequest, token, certifcateId).getStatusCode());

	}
	
	@Test
	public void test_issueRevocationRequest_Success() {
		String certName = "test@t-mobile.com";
		String certficateType = "internal";
		revocationRequest.setReason("unspecified");
		when(sslCertificateService.issueRevocationRequest(certficateType, certName, userDetails, token, revocationRequest))
				.thenReturn(new ResponseEntity<>(HttpStatus.OK));
		when(httpServletRequest.getAttribute("UserDetails")).thenReturn(userDetails);
		assertEquals(HttpStatus.OK,
				SslCertificateController.issueRevocationRequest(httpServletRequest, token, certficateType,certName, revocationRequest).getStatusCode());

	}
       
	@Test
    public void testAddUsertoCertificate() throws Exception {
        String responseJson = "{\"messages\":[\"User is successfully associated \"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);       
        CertificateUser certUser = new CertificateUser("testuser1","read", "certificatename.t-mobile.com", "internal");

        String inputJson =new ObjectMapper().writeValueAsString(certUser);
        when(sslCertificateService.addUserToCertificate(Mockito.any(CertificateUser.class), eq(userDetails), Mockito.anyBoolean())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sslcert/user").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
    }

    @Test
    public void testAssociateApproletoCertificate() throws Exception {
    	CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read", "internal");

        String inputJson =new ObjectMapper().writeValueAsString(certificateApprole);
        String responseJson = "{\"messages\":[\"Approle successfully associated with Certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(sslCertificateService.associateApproletoCertificate(Mockito.any(CertificateApprole.class), eq(userDetails))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sslcert/approle").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseJson)));
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
    public void test_downloadCertificateWithPrivateKey() throws Exception {
        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "abc.com", "password", "pembundle", false);

        String inputJson =new ObjectMapper().writeValueAsString(certificateDownloadRequest);
        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(resource);

        UserDetails userDetails = getMockUser(true);
        when(sslCertificateService.downloadCertificateWithPrivateKey(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), eq(userDetails))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/sslcert/certificates/download").requestAttr("UserDetails", userDetails)
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk());
    }

    @Test
    public void test_downloadCertificate() throws Exception {

        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(resource);

        when(sslCertificateService.downloadCertificate(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(), eq("12345"), eq("pem"))).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/sslcert/certificates/12345/pem")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk());
    }
    
    @Test
    public void testRemoveUserFromCertificate() throws Exception {
        CertificateUser certUser = new CertificateUser("testuser1","read", "certificatename.t-mobile.com", "internal");   	
        String expected = "{\"message\":[\"Successfully removed user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.removeUserFromCertificate(Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        String inputJson = getJSON(certUser);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/v2/sslcert/user")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .content(inputJson))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
    }
    
    @Test
    public void testRemoveGroupFromCertificate() throws Exception {
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read", "internal");   	
        String expected = "{\"message\":[\"Group is successfully removed from certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
        when(sslCertificateService.removeGroupFromCertificate(Mockito.any(), Mockito.any())).thenReturn(responseEntityExpected);
        String inputJson = getJSON(certGroup);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/v2/sslcert/group")
                .header("vault-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .requestAttr("UserDetails", userDetails)
                .content(inputJson))
        		.andExpect(status().isOk()).andReturn();

        String actual = result.getResponse().getContentAsString();
        assertEquals(expected, actual);
    }
    
    private String getJSON(Object obj)  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return TVaultConstants.EMPTY_JSON;
		}
	}

	@Test
	public void test_renewCertificate_Success() {
		String certName = "test@t-mobile.com";		
		String certficateType = "internal";
		when(sslCertificateService.renewCertificate(certficateType, certName, userDetails, token))
				.thenReturn(new ResponseEntity<>(HttpStatus.OK));
		when(httpServletRequest.getAttribute("UserDetails")).thenReturn(userDetails);
		assertEquals(HttpStatus.OK,
				SslCertificateController.renewCertificate(httpServletRequest, token, certficateType, certName).getStatusCode());

	}

	@Test
    public void testAddGrouptoCertificate() throws Exception {
		String responseJson = "{\"messages\":[\"Group is successfully associated with Certificate\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
		CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com","testgroup","read", "internal");
		String inputJson =new ObjectMapper().writeValueAsString(certGroup);
		when(sslCertificateService.addingGroupToCertificate(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any(CertificateGroup.class), Mockito.any(UserDetails.class))).thenReturn(responseEntityExpected);
		 mockMvc.perform(MockMvcRequestBuilders.post("/v2/ss/certificate/group").requestAttr("UserDetails", userDetails)
	                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
	                .header("Content-Type", "application/json;charset=UTF-8")
	                .content(inputJson));
	}
	
	@Test
    public void test_getListOfCertificates() throws Exception {
        // Mock response        
        when(sslCertificateService.getListOfCertificates("5PDrOhsy4ig8L3EpsJZSLAMg","internal")).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        assertEquals(HttpStatus.OK, sslCertificateService.getListOfCertificates("5PDrOhsy4ig8L3EpsJZSLAMg","internal").getStatusCode());
    }
	
	@Test
    public void updateCertOwner() throws Exception {
        // Mock response     
		TargetSystem targetSystem = new TargetSystem();
        targetSystem.setAddress("Target System address");
        targetSystem.setDescription("Target System Description");
        targetSystem.setName("Target Name");

        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setHostname("Target System Service Host name");
        targetSystemServiceRequest.setName("Target System Service Name");
        targetSystemServiceRequest.setPort(443);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(false);
        targetSystemServiceRequest.setMonitoringEnabled(false);
        targetSystemServiceRequest.setDescription("Target Service Description");

        sSLCertificateMetadataRequest.setCertificateName("CertificateName");
        when(sslCertificateService.updateCertOwner("5PDrOhsy4ig8L3EpsJZSLAMg","internal","certificatename.t-mobile.com","owneremail@test.com",userDetails)).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        assertEquals(HttpStatus.OK, sslCertificateService.updateCertOwner("5PDrOhsy4ig8L3EpsJZSLAMg","internal","certificatename.t-mobile.com","owneremail@test.com",userDetails).getStatusCode());
    }

	@Test
	public void testValidateApprovalStatusAndGetCertificateDetails() throws Exception {
		String expected = "{\"message\":[\"Certificate approved and metadata successfully updated\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);
		when(sslCertificateService.validateApprovalStatusAndGetCertificateDetails(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);
		MvcResult result = mockMvc
				.perform(MockMvcRequestBuilders.get("/v2/sslcert/validate/certificatename.t-mobile.com/external")
						.header("vault-token", token).header("Content-Type", "application/json;charset=UTF-8")
						.requestAttr("UserDetails", userDetails).content(expected))
				.andExpect(status().isOk()).andReturn();

		String actual = result.getResponse().getContentAsString();
		assertEquals(expected, actual);
	}
	
	@Test
	public void testdeleteCertificate_Success() {
		String certName = "test@t-mobile.com";		
		String certficateType = "internal";
		when(sslCertificateService.deleteCertificate(token,certficateType, certName, userDetails ))
				.thenReturn(new ResponseEntity<>(HttpStatus.OK));
		when(httpServletRequest.getAttribute("UserDetails")).thenReturn(userDetails);
		assertEquals(HttpStatus.OK,
				sslCertificateService.deleteCertificate(token,certficateType, certName, userDetails).getStatusCode());

	}
	
	
	
	 @Test
	    public void test_getAllCertificatesOnCertType_success() throws Exception{
		  // Mock response        
	        when(sslCertificateService.getAllCertificatesOnCertType(userDetails, "internal")).thenReturn(new ResponseEntity<>(HttpStatus.OK));
	        assertEquals(HttpStatus.OK, sslCertificateService.getAllCertificatesOnCertType(userDetails, "internal").getStatusCode());
	    }
	 
	 @Test	
		public void testCheckCertificateStatus() throws Exception {	
			String expected = "{\"message\":[\"Certifictae is in Revoked status\"]}";	
			ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expected);	
			when(sslCertificateService.checkCertificateStatus(Mockito.anyString(),	
					Mockito.anyString(), Mockito.anyObject())).thenReturn(responseEntityExpected);	
			MvcResult result = mockMvc	
					.perform(MockMvcRequestBuilders.get("/v2/sslcert/checkstatus/certificatename.t-mobile.com/external")	
							.header("vault-token", token).header("Content-Type", "application/json;charset=UTF-8")	
							.requestAttr("UserDetails", userDetails).content(expected))	
					.andExpect(status().isOk()).andReturn();	
			String actual = result.getResponse().getContentAsString();	
			assertEquals(expected, actual);	
		}
}

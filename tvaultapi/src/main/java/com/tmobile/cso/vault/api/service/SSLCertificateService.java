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

package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
import com.tmobile.cso.vault.api.validator.TokenValidator;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SSLCertificateService {

    @Value("${vault.port}")
    private String vaultPort;

    @Autowired
    private RequestProcessor reqProcessor;

    @Autowired
    private WorkloadDetailsService workloadDetailsService;
    
    @Autowired
	private CertificateUtils certificateUtils;

    @Autowired
   	private PolicyUtils policyUtils;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
   	private AuthorizationUtils authorizationUtils;

    @Autowired
	private AppRoleService appRoleService;

    @Autowired
    private TokenValidator tokenValidator;

    @Autowired
    private NCLMMockUtil nclmMockUtil;
    
    @Autowired
    private TokenUtils tokenUtils;    

    @Value("${vault.auth.method}")
    private String vaultAuthMethod;

    @Value("${sslcertmanager.domain}")
    private String certManagerDomain;

    @Value("${sslcertmanager.endpoint.token_generator}")
    private String tokenGenerator;

    @Value("${sslcertmanager.endpoint.target_system_groups}")
    private String targetSystemGroups;

    @Value("${sslcertmanager.endpoint.certificate}")
    private String certificateEndpoint;

    @Value("${sslcertmanager.endpoint.targetsystems}")
    private String targetSystems;

    @Value("${sslcertmanager.endpoint.targetsystemservices}")
    private String targetSystemServies;

    @Value("${sslcertmanager.endpoint.enroll}")
    private String enrollUrl;
    @Value("${sslcertmanager.endpoint.approvalUrl}")
    private String approvalUrl;

    @Value("${sslcertmanager.endpoint.enrollCA}")
    private String enrollCAUrl;

    @Value("${sslcertmanager.endpoint.enrollTemplateUrl}")
    private String enrollTemplateUrl;
    @Value("${sslcertmanager.endpoint.enrollKeysUrl}")
    private String enrollKeysUrl;
    @Value("${sslcertmanager.endpoint.getTemplateParamUrl}")
    private String getTemplateParamUrl;
    @Value("${sslcertmanager.endpoint.putTemplateParamUrl}")
    private String putTemplateParamUrl;

    @Value("${sslcertmanager.endpoint.enrollCSRUrl}")
    private String enrollCSRUrl;

    @Value("${sslcertmanager.endpoint.findTargetSystem}")
    private String findTargetSystem;

    @Value("${sslcertmanager.endpoint.findTargetSystemService}")
    private String findTargetSystemService;

    @Value("${sslcertmanager.endpoint.enrollUpdateCSRUrl}")
    private String enrollUpdateCSRUrl;

    @Value("${sslcertmanager.endpoint.findCertificate}")
    private String findCertificate;

    @Value("${sslcertmanager.username}")
    private String certManagerUsername;

    @Value("${sslcertmanager.password}")
    private String certManagerPassword;

    @Value("${sslcertmanager.targetsystemgroup.private_single_san.ts_gp_id}")
    private int private_single_san_ts_gp_id;

    @Value("${sslcertmanager.targetsystemgroup.private_multi_san.ts_gp_id}")
    private int private_multi_san_ts_gp_id;

    @Value("${sslcertmanager.targetsystemgroup.public_single_san.ts_gp_id}")
    private int public_single_san_ts_gp_id;

    @Value("${sslcertmanager.targetsystemgroup.public_multi_san.ts_gp_id}")
    private int public_multi_san_ts_gp_id;

	@Value("${sslcertmanager.endpoint.getCertifcateReasons}")
	private String getCertifcateReasons;

	@Value("${sslcertmanager.endpoint.issueRevocationRequest}")
	private String issueRevocationRequest;
    @Value("${workload.endpoint}")
    private String workloadEndpoint;

    @Value("${workload.endpoint.token}")
    private String cwmEndpointToken;

    @Value("${certificate.retry.count}")
    private int retrycount;

    @Value("${certificate.delay.time.millsec}")
    private int delayTime;    
    
    @Value("${SSLCertificateController.certificatename.text}")
    private String certificateNameTailText;
    
    @Value("${sslcertmanager.endpoint.renewCertificate}")
	private String renewCertificateEndpoint;  

    @Value("${sslcertmanager.endpoint.unassignCertificate}")
	private String unassignCertificateEndpoint;
    
    @Value("${sslcertmanager.endpoint.deleteCertificate}")
	private String deleteCertificateEndpoint;   

    
    @Value("${certificate.renew.delay.time.millsec}")
    private int renewDelayTime;
    @Value("${sslcertmanager.external.certificate.telephonenumber}")
    private String externalCertificateTelephoneNumber;
    @Value("${sslcertmanager.external.certificate.requester.name}")
    private String externalCertificateRequesterName;
    @Value("${sslcertmanager.external.certificate.requester.email}")
    private String externalCertificateRequesterEmail;
    @Value("${sslcertmanager.external.certificate.lifetime}")
    private String externalCertificateLifeTime;

    @Value("${ad.notification.fromemail}")
    private String supportEmail;

    @Value("${ssl.notification.fromemail}")
    private String fromEmail;

    @Value("${nclm.service.down.message}")
    private String nclmErrorMessage;

    @Value("${sslcertmanager.endpoint.requestStatusUrl}")
    private String requestStatusUrl;
    
    @Autowired
    private EmailUtils emailUtils;

    @Value("${nclm.mock}")
    private String nclmMockEnabled;    
    
    @Value("${selfservice.ssfilelocation}")
    private String downloadLocation;
    
    @Value("${pacbot.endpoint.getToken}")
    private String pacbotGetTokenEndpoint;
    
    @Value("${pacbot.endpoint.getallcertificates}")
    private String pacbotGetCertEndpoint;
    
    @Value("${pacbot.client.id}")
    private String pacbotClientId;
    
    @Value("${pacbot.client.secret}")
    private String pacbotClientSecret;
    
    @Value("${sslcertmanager.targetsystemgroup.private_single_san.ts_gp_id_test}")
    private int private_single_san_ts_gp_id_test;

    @Value("${sslcertmanager.targetsystemgroup.private_multi_san.ts_gp_id_test}")
    private int private_multi_san_ts_gp_id_test;

    @Value("${sslcertmanager.targetsystemgroup.public_single_san.ts_gp_id_test}")
    private int public_single_san_ts_gp_id_test;

    @Value("${sslcertmanager.targetsystemgroup.public_multi_san.ts_gp_id_test}")
    private int public_multi_san_ts_gp_id_test;
    
    @Value("${sslcertmanager.container_name}")
    private String container_name;

    @Value("${sslcertmanager.endpoint.findAllCertificate}")
    private String findAllCertificate;

    @Autowired
	private OIDCUtil oidcUtil;

    private static Logger log = LogManager.getLogger(SSLCertificateService.class);

    private static final String[] PERMISSIONS = {"read", "write", "deny", "sudo"};
    private static final String MESSAGES = "{\"messages\":[\"";
    private static final String ERRORS = "{\"errors\":[\"";
    private static final String ERRORINVALID = "{\"errors\":[\"Invalid input values\"]}";
    private static final String CERTNAMEREGEX = "^\"+|\"+$";

    /**
     * Login using CertManager
     *
     * @param certManagerLoginRequest
     * @return
     */
    public ResponseEntity<String> authenticate(CertManagerLoginRequest certManagerLoginRequest) throws Exception {
        String certManagerAPIEndpoint = "/auth/certmanager/login";
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "CertManager Login  with User name").
                put(LogMessage.MESSAGE, String.format("Trying to authenticate with CertManager with user name = [%s]"
                        ,certManagerLoginRequest.getUsername())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        CertResponse response = reqProcessor.processCert(certManagerAPIEndpoint, certManagerLoginRequest, "", getCertmanagerEndPoint(tokenGenerator));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication Successful").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"CertManager Login Failed.\"]}" + "HTTP STATUSCODE  :" + response.getHttpstatus());
        }
    }

    /**
     * @param certManagerLoginRequest
     * @return
     */
    public CertManagerLogin login(CertManagerLoginRequest certManagerLoginRequest) throws Exception {
        CertManagerLogin certManagerLogin = null;
        String certManagerAPIEndpoint = "/auth/certmanager/login";
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                put(LogMessage.MESSAGE, "Trying to authenticate with CertManager").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        try {
        CertResponse response = reqProcessor.processCert(certManagerAPIEndpoint, certManagerLoginRequest, "", getCertmanagerEndPoint(tokenGenerator));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication Successful").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
            if (!MapUtils.isEmpty(responseMap)) {
                certManagerLogin = new CertManagerLogin();
                if (responseMap.get(SSLCertificateConstants.ACCESS_TOKEN) != null) {
                    certManagerLogin.setAccess_token((String) responseMap.get(SSLCertificateConstants.ACCESS_TOKEN));
                }
                if (responseMap.get(SSLCertificateConstants.TOKEN_TYPE) != null) {
                    certManagerLogin.setToken_type((String) responseMap.get(SSLCertificateConstants.TOKEN_TYPE));
                }
            }
            return certManagerLogin;
        } else if(HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())){
        	 log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                     put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                     put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                     put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).
                     put(LogMessage.RESPONSE, response.getResponse()).
                     put(LogMessage.STATUS, response.getHttpstatus().toString()).
                     put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                     build()));
             return null;
        }else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return null;
        }
        } catch (Exception e) {	
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                    put(LogMessage.ACTION, "getNclmToken").	
                    put(LogMessage.MESSAGE, "Failed to get nclm token").	
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                    build()));	
            return null;	
        }
    }

	/**
	 * This method will be used to update the owner email id
	 *
	 * @param sslCertificateRequest
	 * @return
	 */
	private boolean populateCertOwnerEmaild(SSLCertificateRequest sslCertificateRequest, UserDetails userDetails) {
		boolean isvalid = true;
		if (StringUtils.isEmpty(sslCertificateRequest.getCertOwnerNtid())) {
			sslCertificateRequest.setCertOwnerNtid(userDetails.getUsername());
		}

		if (StringUtils.isEmpty(sslCertificateRequest.getCertOwnerEmailId())) {
			isvalid = false;
			DirectoryUser directoryUser = getUserDetails(sslCertificateRequest.getCertOwnerNtid());
			if (directoryUser != null && !ObjectUtils.isEmpty(directoryUser)) {
				sslCertificateRequest.setCertOwnerEmailId(directoryUser.getUserEmail());
				isvalid = true;
			}
		}
		return isvalid;
	}

    /**
     * @param sslCertificateRequest
     * @return
     */
    public ResponseEntity<String> generateSSLCertificate(SSLCertificateRequest sslCertificateRequest,
                                                               UserDetails userDetails ,String token,String method) {
        CertResponse enrollResponse = new CertResponse();
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE)
        		.put(LogMessage.MESSAGE, "Trying to generate SSL Certificate")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
        		.build()));       
        
        //Validate the input data
        boolean isValidData = validateInputData(sslCertificateRequest, userDetails);
		if (!isValidData) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_INPUT_DATA)
					.put(LogMessage.MESSAGE, "Invalid input data")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		} else {
			String applicationName = getValidApplicationName(sslCertificateRequest);
			if (applicationName!= null && !StringUtils.isEmpty(applicationName)) {
				populateSSLCertificateRequest(sslCertificateRequest, applicationName);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_SSL_CERTIFICATE_REQUEST)
						.put(LogMessage.MESSAGE, "SSL Certifcate request is success")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.GET_VALID_APPLICATION_NAME)
						.put(LogMessage.MESSAGE, "Failed to get valid application name")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
			}
		}		
		 if(method.equalsIgnoreCase(SSLCertificateConstants.API)) {
	        	boolean isValidAppname = validateAppname(token,userDetails,sslCertificateRequest.getAppName());
	        	String errorInvalidApp =  "{\"errors\":[\"To create a certificate you must be a member of the applications Cloud Self-Service group. "
	        			+ "Please go to https://access.t-mobile.com/ and request access to the group r_selfservice_"+sslCertificateRequest.getAppName()+"_admin in the Cloud Access Portal.\"]}";
	        	if(!isValidAppname) {
	        		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
	    					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
	    					.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_INPUT_DATA)
	    					.put(LogMessage.MESSAGE, String.format("To create a certificate you must be a member of the applications Cloud Self-Service group. "
	    							+ "Please request access to the group [%s] in the Cloud Access Portal.",sslCertificateRequest.getAppName()))
	    					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
	    					.build()));
	    			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorInvalidApp);
	        	}
	        }

		try {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("CERTIFICATE REQUEST [%s]",
                            sslCertificateRequest.toString())).
                    put(LogMessage.APIURL,
                            ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));

           String username = (Objects.nonNull(ControllerUtil.getNclmUsername())) ?
                    (new String(Base64.getDecoder().decode(ControllerUtil.getNclmUsername()))) :
                    (new String(Base64.getDecoder().decode(certManagerUsername)));

            String password = (Objects.nonNull(ControllerUtil.getNclmPassword())) ?
                    (new String(Base64.getDecoder().decode(ControllerUtil.getNclmPassword()))) :
                    (new String(Base64.getDecoder().decode(certManagerPassword)));

            //Step-1 : Authenticate
            CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest(username, password);
            //Added mocking config
            CertManagerLogin certManagerLogin = (!isMockingEnabled(sslCertificateRequest.getCertType())) ?
            login(certManagerLoginRequest):nclmMockUtil.getMockLoginDetails();

            if(ObjectUtils.isEmpty(certManagerLogin)) {
            	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                        put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE).	
                        put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).	
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                        build()));	
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\" :[\"" + nclmErrorMessage + "\"]}");
            }
            //Added mocking config
            CertificateData certificateDetails = (!isMockingEnabled(sslCertificateRequest.getCertType())) ?
                    getCertificate(sslCertificateRequest, certManagerLogin):null;
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Details for Certificate name =[%s] = Certificate Details " +
                                    "[%s]", sslCertificateRequest.getCertificateName(), certificateDetails)).
                    build()));

            token = (userDetails.isAdmin())?token : userDetails.getSelfSupportToken();

            if (Objects.isNull(certificateDetails)) {
                //Validate the certificate in metadata path  for external certificate
                if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)) {
                    SSLCertificateMetadataDetails certMetaData =certificateUtils.getCertificateMetaData(token,
                            sslCertificateRequest.getCertificateName(), sslCertificateRequest.getCertType());

                    if ((Objects.nonNull(certMetaData)) && (Objects.nonNull(certMetaData.getRequestStatus()))
                            && (certMetaData.getRequestStatus().equalsIgnoreCase(SSLCertificateConstants.REQUEST_PENDING_APPROVAL))) {
                        String responseMessage = sslCertificateRequest.getCertificateName()+" is already" +
                                " requested by "+ certMetaData.getCertOwnerEmailId() + "  and it's " +
                                "waiting for approval" ;
                        enrollResponse.setSuccess(Boolean.FALSE);
                        enrollResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
                        enrollResponse.setResponse(responseMessage);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, String.format("Given Certificate is waiting for NCLM approval  " +
                                        "[%s] = Certificate name = [%s]", enrollResponse.toString(),
                                        sslCertificateRequest.getCertificateName())).
                                build()));
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\": [\"" + enrollResponse.getResponse() +
                                "\"]}");
                    }
                }

                //Added mocking configuration
                if(!isMockingEnabled(sslCertificateRequest.getCertType())) {
                    //Step-2 Validate targetSystem
                    int targetSystemId = getTargetSystem(sslCertificateRequest, certManagerLogin); 
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    		.put(LogMessage.ACTION, "validateTargetSystem")
                    		.put(LogMessage.MESSAGE, String.format("Validated target system for certificate name [%s]", sslCertificateRequest.getCertificateName()))
                    		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                    		.build()));

                    //Step-3:  CreateTargetSystem
                    if (targetSystemId == 0) {
                        targetSystemId = createTargetSystem(sslCertificateRequest.getTargetSystem(), certManagerLogin,
                                getContainerId(sslCertificateRequest));
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("createTargetSystem Completed Successfully [%s] for " +
                                                "certificate name = [%s]",
                                        targetSystemId, sslCertificateRequest.getCertificateName())).
                                build()));
                        if (targetSystemId == 0) {
                            enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                            enrollResponse.setSuccess(Boolean.FALSE);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"" + enrollResponse.getResponse() + "\"]}");
                        }
                    }

                    //Step-4 : Validate the Target System Service
                    int targetSystemServiceId = getTargetSystemServiceId(sslCertificateRequest, targetSystemId, certManagerLogin);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    		.put(LogMessage.ACTION, "validateTargetSystemServiceID")	
                    		.put(LogMessage.MESSAGE, String.format("Validated target system service id for certificate name [%s]", sslCertificateRequest.getCertificateName()))
                    		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                    		.build()));

                    //Step-5: Create Target System  Service
                    if (targetSystemServiceId == 0) {
                        TargetSystemServiceRequest targetSystemServiceRequest = prepareTargetSystemServiceRequest(sslCertificateRequest);
                        TargetSystemService targetSystemService = createTargetSystemService(targetSystemServiceRequest, targetSystemId, certManagerLogin);

                        if (Objects.nonNull(targetSystemService)) {
                            targetSystemServiceId = targetSystemService.getTargetSystemServiceId();
                        } else {
                            enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                            enrollResponse.setSuccess(Boolean.FALSE);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + enrollResponse.getResponse() + "\"]}");
                        }
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("createTargetSystem Service  Completed Successfully " +
                                                "[%s] = certificate name = [%s] ", targetSystemService.toString(),
                                        sslCertificateRequest.getCertificateName())).
                                put(LogMessage.MESSAGE, String.format("Target System Service ID  [%s]", targetSystemService.getTargetSystemServiceId())).
                                build()));
                    }

                    //Step-7 - Enroll Configuration
                    //getEnrollCA
                    CertResponse response = getEnrollCA(certManagerLogin, targetSystemServiceId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("getEnrollCA Completed Successfully [%s] = certificate name = [%s]",
                                    response.getResponse(), sslCertificateRequest.getCertificateName())).
                            build()));

                    ////Only for External -MultiSAN - Update the selectedId
                    if ((!StringUtils.isEmpty(sslCertificateRequest.getCertType())) && sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)
                            && sslCertificateRequest.getDnsList().length > 0) {
                        response = prepareEnrollCARequest(response);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("getEnrollCA Completed Successfully [%s] = " +
                                                "For External-MultiSAN-certificate name = [%s]",
                                        response.getResponse(), sslCertificateRequest.getCertificateName())).
                                build()));
                    }

                    //Step-8 PutEnrollCA
                    int updatedSelectedId = putEnrollCA(certManagerLogin, targetSystemServiceId, response);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("PutEnroll CA Successfully Completed[%s] = certificate " +
                                            "name = [%s]",
                                    updatedSelectedId, sslCertificateRequest.getCertificateName())).
                            build()));

                    //Step-9  GetEnrollTemplates
                    CertResponse templateResponse = getEnrollTemplates(certManagerLogin, targetSystemServiceId,
                            updatedSelectedId,sslCertificateRequest);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("Get Enrollment template  Completed Successfully [%s] = certificate name = [%s]",
                                    templateResponse.getResponse(), sslCertificateRequest.getCertificateName())).
                            build()));

                    //Only for External -MultiSAN - Update the selectedId
                    if ((!StringUtils.isEmpty(sslCertificateRequest.getCertType())) && sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)
                            && sslCertificateRequest.getDnsList().length > 0) {
                        templateResponse = prepareTemplateReqForExternalCert(templateResponse);
                    }
                    //Step-10  PutEnrollTemplates
                    int enrollTemplateId = putEnrollTemplates(certManagerLogin, targetSystemServiceId, templateResponse, updatedSelectedId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("PutEnroll template  Successfully Completed = " +
                                    "enrollTemplateId = [%s] = certificate name = [%s]", enrollTemplateId, sslCertificateRequest.getCertificateName())).
                            build()));

                    if ((!StringUtils.isEmpty(sslCertificateRequest.getCertType())) && sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)) {
                        //GetTemplateParameters
                        CertResponse getTemplateResponse = getTemplateParametersResponse(certManagerLogin,
                                targetSystemServiceId, enrollTemplateId);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, String.format("GetTemplateParameters  Successfully Completed = " +
                                                "getTemplateParamterRequest = [%s] = certificate name = [%s] ",
                                        getTemplateResponse.getResponse(), sslCertificateRequest.getCertificateName())).build()));
                        if (sslCertificateRequest.getDnsList().length > 0) {
                            getTemplateResponse = updateRequestWithRequestedDetails(getTemplateResponse);
                        }
                        //PutTemplateParameters
                        CertResponse putTemplateParameterResponse = putTemplateParameterResponse(certManagerLogin, targetSystemServiceId,
                                enrollTemplateId, getTemplateResponse);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, String.format("PUt TemplateParameters  Successfully Completed = " +
                                                "putTemplateParamterResponse = [%s] = certificate name = [%s] ",
                                        putTemplateParameterResponse.getResponse(), sslCertificateRequest.getCertificateName())).build()));
                    }

                    //Step-11  GetEnrollKeys
                    CertResponse getEnrollKeyResponse = getEnrollKeys(certManagerLogin, targetSystemServiceId, enrollTemplateId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("getEnrollKeys Completed Successfully [%s] = certificate name = [%s]",
                                    getEnrollKeyResponse.getResponse(), sslCertificateRequest.getCertificateName())).
                            build()));

                    //Step-12  PutEnrollKeys
                    int enrollKeyId = putEnrollKeys(certManagerLogin, targetSystemServiceId, getEnrollKeyResponse, enrollTemplateId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("putEnrollKeys  Successfully Completed[%s] = certificate name = [%s]",
                                    enrollKeyId, sslCertificateRequest.getCertificateName())).
                            build()));

                    //Step-13 GetEnrollCSRs
                    String updatedRequest = getEnrollCSR(certManagerLogin, targetSystemServiceId, enrollTemplateId, sslCertificateRequest);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("getEnrollCSRResponse Completed Successfully [%s] = certificate name = [%s]",
                                    updatedRequest, sslCertificateRequest.getCertificateName())).
                            build()));

                    //In case multiSAN(external/internal)-Need to build request for SubjectAlternateNames
                    if (sslCertificateRequest.getDnsList() != null && sslCertificateRequest.getDnsList().length > 0) {
                        //Build Object with DNS names
                        updatedRequest = buildSubjectAlternativeNameRequest(updatedRequest, sslCertificateRequest,
                                targetSystemServiceId);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, String.format("Build Object with DNS names [%s] = certificate name = [%s]",
                                        updatedRequest, sslCertificateRequest.getCertificateName())).
                                build()));

                    } else {
                        //If dnsList is empty ,remove the DNS names object from request
                        updatedRequest = removeSubjectAlternativeNameRequest(updatedRequest);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, String.format("Remove  DNS names [%s] = certificate name = [%s]",
                                        updatedRequest, sslCertificateRequest.getCertificateName())).
                                build()));
                    }

                    //Step-14  PutEnrollCSRs
                    CertResponse putEnrollCSRResponse = putEnrollCSR(certManagerLogin, targetSystemServiceId, updatedRequest);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("PutEnroll CSR  Successfully Completed  = [%s] = certificate name = [%s]",
                                    putEnrollCSRResponse, sslCertificateRequest.getCertificateName())).
                            build()));
                    String responseDetails = validateCSRResponse(putEnrollCSRResponse.getResponse());
                    if (!StringUtils.isEmpty(responseDetails)) {
                        enrollResponse.setSuccess(Boolean.FALSE);
                        enrollResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
                        enrollResponse.setResponse(responseDetails);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("Exception While creating certificate  " +
                                                "[%s] = certificate name = [%s]", responseDetails,
                                        sslCertificateRequest.getCertificateName())).
                                build()));
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORS + enrollResponse.getResponse() +
                                "\"]}");
                    }

                    //Step-15: Enroll Process
                    enrollResponse = enrollCertificate(certManagerLogin, targetSystemServiceId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("Enroll Certificate response Completed Successfully [%s]" +
                                    " = certificate name  = [%s]", enrollResponse.getResponse(), sslCertificateRequest.getCertificateName())).
                            build()));
                } else {
                    enrollResponse = nclmMockUtil.getEnrollMockResponse();
                }

                int actionId = 0;
                if (enrollResponse.getHttpstatus().equals(HttpStatus.ACCEPTED) && Objects.nonNull(enrollResponse.getResponse())) {
                    Map<String, Object> responseMap = ControllerUtil.parseJson(enrollResponse.getResponse());
                    if (!MapUtils.isEmpty(responseMap) && responseMap.get(SSLCertificateConstants.ACTION_ID) != null) {
                        actionId = (Integer) responseMap.get(SSLCertificateConstants.ACTION_ID);
                        if (actionId != 0) {
                            enrollResponse = approvalRequest(certManagerLogin, actionId);
                            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                    put(LogMessage.ACTION, String.format("approvalRequest Completed Successfully [%s]" +
                                            " = certificate name = [%s]", enrollResponse.getResponse(),sslCertificateRequest.getCertificateName())).
                                    build()));
                        }
                    }
                }
                //If Certificate creates successfully
				if ( (HttpStatus.OK.equals(enrollResponse.getHttpstatus()) ||
                        (HttpStatus.NO_CONTENT.equals(enrollResponse.getHttpstatus())))) {
					// Policy Creation
					boolean isPoliciesCreated;

					// To handle wildcard certificate
                    String actualCertName = sslCertificateRequest.getCertificateName();
                    boolean isWildCardCertificate = certificateUtils.isWildcardCertificate(actualCertName);
                    if (isWildCardCertificate) {
                        sslCertificateRequest.setCertificateName(certificateUtils.getVaultCompactibleCertifiacteName(actualCertName));
                    }

					if (userDetails.isAdmin()) {
						isPoliciesCreated = createPolicies(sslCertificateRequest, token);
					} else {
						isPoliciesCreated = createPolicies(sslCertificateRequest, userDetails.getSelfSupportToken());
					}
                    if(isPoliciesCreated) {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("Policies are created for SSL certificate name [%s]",
                                        certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).
                                build()));
                    }


                    String metadataJson = populateSSLCertificateMetadata(sslCertificateRequest, userDetails,
                            certManagerLogin,actionId);

					boolean sslMetaDataCreationStatus;

					if (userDetails.isAdmin()) {
						sslMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson, token);
					} else {
						sslMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson,
								userDetails.getSelfSupportToken());
					}


                    if (sslMetaDataCreationStatus) {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("Metadata  created for SSL certificate name [%s]",
                                        certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).
                                build()));
                    }

                    boolean sslApplicationMetaDataSaveStatus;
                    //save certificate name into application metadata path
                    if (userDetails.isAdmin()) {
                    	sslApplicationMetaDataSaveStatus = certificateMetadataForApplicationDetails(metadataJson, token, "create");
					} else {
						sslApplicationMetaDataSaveStatus = certificateMetadataForApplicationDetails(metadataJson,
								userDetails.getSelfSupportToken(), "create");
					}
                    
                    if (sslApplicationMetaDataSaveStatus) {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("Certificate details added to Application Metadata for SSL certificate name [%s]",
                                        certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).
                                build()));
                    }

                    //Send failed certificate response in case of any issues in Policy/Meta data creation
                    if ((!isPoliciesCreated) || (!sslMetaDataCreationStatus)) {
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                        enrollResponse.setSuccess(Boolean.FALSE);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION,
										String.format(
												"Metadata or Policies failed for SSL certificate name [%s] - metaDataStatus[%s] - policyStatus[%s]",
                                                certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()), sslMetaDataCreationStatus,
												isPoliciesCreated))
								.build()));
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS+enrollResponse.getResponse()+"\"]}");
                    } else {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE).
                                put(LogMessage.MESSAGE, "addSudoPermissionToCertificateOwner- STARTED ").
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                    	return addSudoPermissionToCertificateOwner(sslCertificateRequest, userDetails, enrollResponse
                                , isPoliciesCreated, sslMetaDataCreationStatus,token,"create");
                    }
                }
            } else {
                SSLCertificateMetadataDetails certMetaDataDetails = certificateUtils.getCertificateMetaData(token,
                        sslCertificateRequest.getCertificateName(), sslCertificateRequest.getCertType());
                String responseMessage;
                if(Objects.nonNull(certMetaDataDetails)) {
                 responseMessage = certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName())+" is already" +
                        " available  in system and owned  by "+ certMetaDataDetails.getCertOwnerEmailId() +" " +
                        ". Please try with different certificate name";
                } else {
                    responseMessage = "Certificate is already available in NCLM with Active status";
               }


                enrollResponse.setSuccess(Boolean.FALSE);
                enrollResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
                enrollResponse.setResponse(responseMessage);

                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("Certificate Already Available in  NCLM with Active Status " +
                                "[%s] = certificate name = [%s]", enrollResponse.toString(),
                                certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORS+enrollResponse.getResponse()+
                        "\"]}");
            }
        } catch (TVaultValidationException tex) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Inside  TVaultValidationException " +
                                    "Exception = [%s] =  Message [%s] = certificate name = [%s]",
                            Arrays.toString(tex.getStackTrace()), tex.getMessage(),certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).build()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + tex.getMessage() + "\"]}");

        } catch (Exception e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Inside  Exception " +
                                    "Exception = [%s] =  Message [%s] = = certificate name = [%s]", Arrays.toString(e.getStackTrace()),
                            e.getMessage(),certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).build()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body
                    (ERRORS + SSLCertificateConstants.SSL_CREATE_EXCEPTION + "\"]}");
        }

        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE).
                put(LogMessage.MESSAGE, String.format ("certificate [%s] before sending an email ",
                certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.OK).body(MESSAGES+SSLCertificateConstants.SSL_CERT_SUCCESS+"\"]}");
    }

    /**
     * THis method is responsible for sending email for cert creation for internal and external cert creation
     * @param sslCertificateRequest
     * @param userDetails
     * @param token
     */

    private void sendCreationEmail(SSLCertificateRequest sslCertificateRequest,
                                   UserDetails userDetails, String token) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
                .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                .put(LogMessage.ACTION, String.format("sendCreationEmail for SSL certificate [%s] - certType [%s] - "
                        , sslCertificateRequest.getCertificateName(), sslCertificateRequest.getCertType()))
                .build()));
        

         if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
            //Send email for certificate creation
            sendEmail(sslCertificateRequest.getCertType(), sslCertificateRequest.getCertificateName(),
                    sslCertificateRequest.getCertOwnerEmailId(),userDetails.getUsername(),
                    SSLCertificateConstants.CERT_CREATION_SUBJECT + " - " + sslCertificateRequest.getCertificateName(),
                    "created", token);
        } else {
             if(!isMockingEnabled(sslCertificateRequest.getCertType())) {
                 sendExternalEmail(sslCertificateRequest.getCertType(), sslCertificateRequest.getCertificateName(),
                         sslCertificateRequest.getCertOwnerEmailId(), userDetails.getUsername(),
                         SSLCertificateConstants.EX_CERT_CREATION_SUBJECT + " - " + sslCertificateRequest.getCertificateName(),
                         "creation");
             }
        }
    }


    private String validateCSRResponse(String csrResponse) {
        String errorDesc = null;
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject jsonValue = gson.fromJson(csrResponse, JsonObject.class);
        JsonElement containsError = jsonValue.get("containsErrors");
        if (Objects.nonNull(containsError) && (!jsonValue.get("containsErrors").isJsonNull())) {
            errorDesc = parseSubject(csrResponse);
            if (StringUtils.isEmpty(errorDesc)) {
                errorDesc = parseSubjectAlternativeName(csrResponse);
            }
        }
        return errorDesc;
    }

    //Parse SubjectAlternativeName
    private String parseSubjectAlternativeName(String json) {
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject jsonObj = gson.fromJson(json, JsonObject.class);
        JsonObject jsonAlternateNames = jsonObj.getAsJsonObject("subjectAlternativeName");
        if (Objects.nonNull(jsonAlternateNames)) {
            JsonArray jsonArray = jsonAlternateNames.getAsJsonArray("items");
            JsonObject jsonObject;
            for (int i = 0; i < jsonArray.size(); i++) {
                jsonObject = jsonArray.get(i).getAsJsonObject();
                JsonArray valueJsonArray = jsonObject.getAsJsonArray("value");
                for (int j = 0; j < valueJsonArray.size(); j++) {
                    JsonObject jsonObject1 = valueJsonArray.get(j).getAsJsonObject();
                    if (jsonObject1.getAsJsonObject(SSLCertificateConstants.VALIDATION_RESULT_LABEL) != null) {
                        JsonArray validationResult = jsonObject1.getAsJsonObject(SSLCertificateConstants.VALIDATION_RESULT_LABEL)
                                .getAsJsonArray("results");
                        for (int k = 0; k < validationResult.size(); k++) {
                            JsonObject result = validationResult.get(k).getAsJsonObject();
                            if (!StringUtils.isEmpty(result.get("description"))) {
                                return result.get("description").getAsString();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    //Parse Subject Array
    private String parseSubject(String csrResponse) {
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject jsonObj = gson.fromJson(csrResponse, JsonObject.class);
        JsonObject subjectJson = jsonObj.getAsJsonObject("subject");
        JsonArray jsonArray = subjectJson.getAsJsonArray("items");
        JsonObject jsonObject;
        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject = jsonArray.get(i).getAsJsonObject();
            JsonArray valueJson = jsonObject.getAsJsonArray("value");
            for (int j = 0; j < valueJson.size(); j++) {
                JsonObject valueObj = valueJson.get(j).getAsJsonObject();
                if (valueObj.getAsJsonObject(SSLCertificateConstants.VALIDATION_RESULT_LABEL) != null) {
                    JsonArray validationResult = valueObj.getAsJsonObject(SSLCertificateConstants.VALIDATION_RESULT_LABEL)
                            .getAsJsonArray("results");
                    for (int k = 0; k < validationResult.size(); k++) {
                        JsonObject result = validationResult.get(k).getAsJsonObject();
                        if (!StringUtils.isEmpty(result.get("description"))) {
                            return result.get("description").getAsString();
                        }
                    }
                }
            }
        }
        return null;
    }
    /**
     *
     * @param response
     * @return
     */
    private CertResponse  prepareTemplateReqForExternalCert(CertResponse response){
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(response.getResponse(),JsonObject.class);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json.toString());
        JsonObject jsonObject1 = jsonObject.getAsJsonObject("template");
        JsonArray jsonArray = jsonObject1.getAsJsonArray("items");
        JsonObject jsonObject2 = null;
        int selectedId = 0;
        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject2 = jsonArray.get(i).getAsJsonObject();
            if (jsonObject2.get("displayName").getAsString().toString().equals("Unified Communication Multi-Domain SSL Certificate")) {
                selectedId = Integer.parseInt(jsonObject2.get("policyLinkId").getAsString());
            }
        }

        jsonObject1.addProperty("selectedId",selectedId);
        response.setResponse(jsonObject.toString());
        return response;
    }


    /**
     * Update the selectedId value
     * @param response
     * @return
     */

    private CertResponse prepareEnrollCARequest(CertResponse response){
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(response.getResponse(),JsonObject.class);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json.toString());
        JsonObject jsonObject1 = jsonObject.getAsJsonObject("ca");
        JsonArray jsonArray = jsonObject1.getAsJsonArray("items");
        JsonObject jsonObject2 = null;
        int selectedId = 0;
        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject2 = jsonArray.get(i).getAsJsonObject();
            if (jsonObject2.get("displayName").getAsString().equals("Entrust CA")) {
                selectedId = Integer.parseInt(jsonObject2.get("policyLinkId").getAsString());
            }
        }
        jsonObject1.addProperty("selectedId",selectedId);
        response.setResponse(jsonObject.toString());
        return response;
    }


    /**
     *
     * @param csrRequest
     * @return
     */
    private String removeSubjectAlternativeNameRequest(String csrRequest) {
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject csrRequestDetails = gson.fromJson(csrRequest, JsonObject.class);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(csrRequestDetails.toString());

        //Removing the subjectAlternativeName object
        jsonObject.remove("subjectAlternativeName");
        return jsonObject.toString();
    }

    /**
     * Build SubjectAlternateNames with multiple DNS names
     * @param csrRequest
     * @param sslCertificateRequest
     * @param entityId
     * @return
     */
    private String buildSubjectAlternativeNameRequest(String csrRequest, SSLCertificateRequest sslCertificateRequest,
                                                      int entityId) {
        String[] dnsList = sslCertificateRequest.getDnsList();

        SubjectAlternativeName subjectAlternativeName = new SubjectAlternativeName();
        List<Items> itemList = new ArrayList<>();

        //Items
        Items items = new Items();
        items.setRemovable(false);
        items.setTypeName(SSLCertificateConstants.DNS_LABEL);
        items.setRemovable(true);

        //Owner
        Owner owner = new Owner();
        owner.setDisplayName(SSLCertificateConstants.POLICY_LABEL);
        owner.setEntityId(entityId);
        owner.setEntityRef(SSLCertificateConstants.SERVICE_LABEL);

        //DenyMore
        DenyMore denyMore = new DenyMore();
        denyMore.setDisabled(false);
        denyMore.setOwner(owner);
        denyMore.setValue(false);

        //Required
        Required required = new Required();
        required.setDisabled(false);
        required.setOwner(owner);
        required.setValue(false);

        //Setting to Items
        items.setDenyMore(denyMore);
        items.setRequired(required);

        //Preparing the Value array
        List<com.tmobile.cso.vault.api.model.Value> valueList = new ArrayList<>();

        for (String dnsName : dnsList) {
            com.tmobile.cso.vault.api.model.Value value = new com.tmobile.cso.vault.api.model.Value();
            value.setDisabled(false);
            value.setOwner(owner);
            value.setValue(dnsName);
            valueList.add(value);
        }

        items.setValue(valueList);
        itemList.add(items);
        subjectAlternativeName.setItems(itemList);


        String subjectAltObject = new Gson().toJson(subjectAlternativeName);
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject csrRequestDetails = gson.fromJson(csrRequest, JsonObject.class);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(csrRequestDetails.toString());

        //Removing the subjectAlternativeName object
        jsonObject.remove(SSLCertificateConstants.SUBJECT_ALTERNATIVE_NAMES);

        //Adding Object again with details
        JsonElement jsonElement = jsonParser.parse(subjectAltObject);
        jsonObject.add(SSLCertificateConstants.SUBJECT_ALTERNATIVE_NAMES, jsonElement);
        return jsonObject.toString();
    }


    /**
     * Update the Template Response with updated requested details
     *
     * @param certManagerLogin
    }
    /**
     * Update the request with requester details
     * @param certResponse
     * @return
     */
    private CertResponse updateRequestWithRequestedDetails(CertResponse certResponse) {
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(certResponse.getResponse(), JsonObject.class);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json.toString());
        JsonObject jsonObject1 = jsonObject.getAsJsonObject("templateParameters");
        JsonArray jsonArray = jsonObject1.getAsJsonArray("items");
        JsonObject jsonObject2 = null;
        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject2 = jsonArray.get(i).getAsJsonObject();
            if (jsonObject2.get("name").getAsString().equals("appname")) {
                jsonObject2.addProperty(SSLCertificateConstants.PROPERTY_VALUE, externalCertificateRequesterName);
            } else if (jsonObject2.get("name").getAsString().equals("appemail")) {
                jsonObject2.addProperty(SSLCertificateConstants.PROPERTY_VALUE, externalCertificateRequesterEmail);
            } else if (jsonObject2.get("name").getAsString().equals("apptelephone")) {
                jsonObject2.addProperty(SSLCertificateConstants.PROPERTY_VALUE, externalCertificateTelephoneNumber);
            } else if (jsonObject2.get("name").getAsString().equals("certyears")) {
                jsonObject2.addProperty(SSLCertificateConstants.PROPERTY_VALUE, externalCertificateLifeTime);
            }
        }
        certResponse.setResponse(jsonObject.toString());
        return certResponse;
    }

    /**
     * This Method used to get the container id
     * @param sslCertificateRequest
     * @return
     */
    private int getContainerId(SSLCertificateRequest sslCertificateRequest){
        int containerId=0;
        int dnsList = sslCertificateRequest.getDnsList().length;
        if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
            containerId =getTargetSystemGroupId(SSLCertType.valueOf(SSLCertificateConstants.PRIVATE_SINGLE_SAN));
        } else  if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL) && dnsList > 0) {
            containerId =getTargetSystemGroupId(SSLCertType.valueOf("PUBLIC_MULTI_SAN"));
        } else  if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL) && dnsList == 0) {
            containerId =getTargetSystemGroupId(SSLCertType.valueOf(SSLCertificateConstants.PUBLIC_SINGLE_SAN));
        }
        return containerId;
    }

    /**
     * Update the Template Response with updated requested details
     *
     * @param certManagerLogin
     * @param templateid
     * @param entityId
     * @param templateResponse
     * @return
     */
    private CertResponse putTemplateParameterResponse(CertManagerLogin certManagerLogin, int entityId,
                                                      int templateid, CertResponse templateResponse) throws Exception {
        String enrollEndPoint = "/certmanager/putTemplateParameter";
        String enrollTemplateCA = putTemplateParamUrl.replace(SSLCertificateConstants.TEMPLATE_ID, String.valueOf(templateid)).replace(
                SSLCertificateConstants.ENTITY_ID, String.valueOf(entityId));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "putTemplateParameterResponse")
        		.put(LogMessage.MESSAGE, "Trying to update the template parameter response")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        return reqProcessor.processCert(enrollEndPoint, templateResponse.getResponse(),
                certManagerLogin.getAccess_token(),
                getCertmanagerEndPoint(enrollTemplateCA));
    }

    /**
     * Get The templateParamter Response - Update the requester details
     *
     * @param certManagerLogin
     * @param entityId
     * @param templateid
     * @return
     * @throws Exception
     */
    private CertResponse getTemplateParametersResponse(CertManagerLogin certManagerLogin, int entityId,
                                                       int templateid) throws Exception {
        String enrollEndPoint = "/certmanager/getTemplateParameter";
        String enrollTemplateCA = getTemplateParamUrl.replace(SSLCertificateConstants.TEMPLATE_ID, String.valueOf(templateid)).replace(
                SSLCertificateConstants.ENTITY_ID, String.valueOf(entityId));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "getTemplateParametersResponse")
        		.put(LogMessage.MESSAGE, "Trying to get the template parameter response")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(),
                getCertmanagerEndPoint(enrollTemplateCA));
    }

    /**
     * This method will be responsible to send the approva request
     * @param certManagerLogin
     * @param entityId
     * @return
     * @throws Exception
     */
    private CertResponse approvalRequest(CertManagerLogin certManagerLogin, int actionId) throws Exception {
        String approvalEndPoint = "/certmanager/approvalrequest";
        String endPoint = approvalUrl.replace(SSLCertificateConstants.ACTION_ID, String.valueOf(actionId));
        ApproveRequest approveRequest = new ApproveRequest();
        approveRequest.setFinalize(true);
        approveRequest.setNote(SSLCertificateConstants.REQUEST_FOR_APPROVAL);
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "approvalRequest")
        		.put(LogMessage.MESSAGE, "Trying to send the approval request")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        return reqProcessor.processCert(approvalEndPoint, approveRequest, certManagerLogin.getAccess_token(), getCertmanagerEndPoint(endPoint));
    }

    /**
	 * Method to provide sudo permission to certificate owner
	 * 
	 * @param sslCertificateRequest
	 * @param userDetails
	 * @param token
	 * @param enrollResponse
	 * @param isPoliciesCreated
	 * @param sslMetaDataCreationStatus
	 * @return
	 */
	private ResponseEntity<String> addSudoPermissionToCertificateOwner(SSLCertificateRequest sslCertificateRequest,
			UserDetails userDetails, CertResponse enrollResponse, boolean isPoliciesCreated,
			boolean sslMetaDataCreationStatus,String token,String operation) {
		CertificateUser certificateUser = new CertificateUser();
		ResponseEntity<String> addUserresponse;
		ResponseEntity<String> addReadPolicyResponse;
		certificateUser.setUsername(sslCertificateRequest.getCertOwnerNtid());
		certificateUser.setAccess(TVaultConstants.SUDO_POLICY);
		certificateUser.setCertificateName(certificateUtils.getVaultCompactibleCertifiacteName(sslCertificateRequest.getCertificateName()));
		certificateUser.setCertType(sslCertificateRequest.getCertType());
		
		if(operation!=null && operation.equalsIgnoreCase(SSLCertificateConstants.ONBOARD)) {
			 addUserresponse = addUserToCertificateOnboard(certificateUser, userDetails, true);
		}
		
		else {
		 addUserresponse = addUserToCertificate(certificateUser, userDetails, true);
		}
		
		if(HttpStatus.OK.equals(addUserresponse.getStatusCode())){
			certificateUser.setAccess(TVaultConstants.WRITE_POLICY);
			if(operation!=null && operation.equalsIgnoreCase(SSLCertificateConstants.ONBOARD)) {
				addReadPolicyResponse = addUserToCertificateOnboard(certificateUser, userDetails, true);
			}else {
			 addReadPolicyResponse = addUserToCertificate(certificateUser, userDetails, true);
			}
			if(HttpStatus.OK.equals(addReadPolicyResponse.getStatusCode())){
			    if(sslCertificateRequest.getCertType().equals(SSLCertificateConstants.INTERNAL)) {
                    enrollResponse.setResponse(SSLCertificateConstants.SSL_CERT_SUCCESS);
                } else {
                    if(isMockingEnabled(sslCertificateRequest.getCertType())){
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_CERT_SUCCESS);
                    } else {
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_EXT_CERT_SUCCESS);
                    }
                }
				enrollResponse.setSuccess(Boolean.TRUE);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, String.format("Metadata or Policies created for SSL certificate [%s] - metaDataStatus [%s] - policyStatus [%s]", certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()), sslMetaDataCreationStatus, isPoliciesCreated))
						.build()));
                //Send email only in case of creation
                if (operation != null && operation.equalsIgnoreCase("create")) {
                    sendCreationEmail(sslCertificateRequest, userDetails, token);

                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                            .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                            .put(LogMessage.ACTION, String.format("CERTIFICATE [%s] - CREATED SUCCESSFULLY - BY [%s] - " +
                                            "ON- [%s] AND TYPE [%s]",
                                    certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()), sslCertificateRequest.getCertOwnerEmailId(),
                                    LocalDateTime.now(), sslCertificateRequest.getCertType())).build()));
                }


			    return ResponseEntity.status(HttpStatus.OK).body(MESSAGES+enrollResponse.getResponse()+"\"]}");
			}else {
				enrollResponse.setResponse(SSLCertificateConstants.SSL_OWNER_PERMISSION_EXCEPTION);
	            enrollResponse.setSuccess(Boolean.FALSE);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			            put(LogMessage.ACTION, "addUserToCertificate").
			            put(LogMessage.MESSAGE, "Adding sudo permission to certificate owner failed").
			            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			            build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS+enrollResponse.getResponse()+"\"]}");
			}
		}else {
			enrollResponse.setResponse(SSLCertificateConstants.SSL_OWNER_PERMISSION_EXCEPTION);
            enrollResponse.setSuccess(Boolean.FALSE);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		            put(LogMessage.ACTION, "addUserToCertificate").
		            put(LogMessage.MESSAGE, "Adding sudo permission to certificate owner failed").
		            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		            build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS+enrollResponse.getResponse()+"\"]}");
		}
	}

    private DirectoryUser getUserDetails(String userName) {
        ResponseEntity<DirectoryObjects> data = directoryService.searchByCorpId(userName);
        DirectoryObjects Obj = data.getBody();
        DirectoryObjectsList usersList = Obj.getData();
        DirectoryUser directoryUser = null;
        for (int i = 0; i < usersList.getValues().length; i++) {
            directoryUser = (DirectoryUser) usersList.getValues()[i];
            if (directoryUser.getUserName().equalsIgnoreCase(userName)) {
                break;
            }
        }
        if(directoryUser != null) {
	        String[] displayName =   directoryUser.getDisplayName().split(",");
	        if(displayName.length > 1) {
	            directoryUser.setDisplayName(displayName[1] + "  " + displayName[0]);
	        }
        }
        return directoryUser;
    }

    /**
     * @param certType
     * @param certName
     * @param userDetails
     */
    private void sendDeleteEmail(String token,String certType, String certName, String certOwnerEmailId, String certOwnerNtId,
                                 String subject,
                                 String operation,CertificateData certData,Map<String, String> metadataParams) {
        DirectoryUser directoryUser = getUserDetails(certOwnerNtId);
        if (directoryUser != null) {
            String enrollService = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_CERT_ENROLL_STRING :
                    SSLCertificateConstants.EXTERNAL_CERT_ENROLL_STRING);

            String keyUsage = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ? getActualKeyUsageValue(metadataParams.get("keyUsageValue")) :
                    SSLCertificateConstants.EXTERNAL_KEY_USAGE);

                 // set template variables
            Map<String, String> mailTemplateVariables = new HashMap<>();
            mailTemplateVariables.put("name", directoryUser.getDisplayName());
            mailTemplateVariables.put(SSLCertificateConstants.CERT_TYPE, StringUtils.capitalize(certType));
            mailTemplateVariables.put(SSLCertificateConstants.CERT_NAME, certName);
            mailTemplateVariables.put(SSLCertificateConstants.CONTACT_LINK, fromEmail);
            mailTemplateVariables.put("operation", operation);
            mailTemplateVariables.put("enrollService", enrollService);
            mailTemplateVariables.put("supportEmail", supportEmail);
            mailTemplateVariables.put("keyUsage", keyUsage);
            mailTemplateVariables.put("certStartDate", certData != null ? Objects.requireNonNull(certData).getCreateDate() : null);
            mailTemplateVariables.put("certEndDate", certData != null ? Objects.requireNonNull(certData).getExpiryDate() : null);
            emailUtils.sendHtmlEmalFromTemplateForDelete(fromEmail, certOwnerEmailId,
                    subject, mailTemplateVariables);
            
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "sendEmail").
                    put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   " +
                                    "for an user name =  [%s] , do Emails might not send to customer ",
                            certOwnerEmailId)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }
    /**
     * @param certType
     * @param certName
     * @param userDetails
     */
    private void sendEmail(String certType, String certName, String certOwnerEmailId, String certOwnerNtId  ,
                           String subject, String operation, String token) {
        DirectoryUser directoryUser = getUserDetails(certOwnerNtId);
        if (directoryUser != null) {
            String enrollService = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_CERT_ENROLL_STRING :
                    SSLCertificateConstants.EXTERNAL_CERT_ENROLL_STRING);
            SSLCertificateMetadataDetails certMetaData = null;

            if (!StringUtils.isEmpty(token)) {
                //Get the DNS names
                certMetaData = certificateUtils.getCertificateMetaData(token, certName, certType);
            }
            String keyUsage = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    getActualKeyUsageValue(certMetaData.getKeyUsageValue()) :
                    SSLCertificateConstants.EXTERNAL_KEY_USAGE);
            // set template variables
            Map<String, String> mailTemplateVariables = new HashMap<>();
            mailTemplateVariables.put("name", directoryUser.getDisplayName());
            mailTemplateVariables.put(SSLCertificateConstants.CERT_TYPE, StringUtils.capitalize(certType));
            mailTemplateVariables.put(SSLCertificateConstants.CERT_NAME, certName);
            mailTemplateVariables.put(SSLCertificateConstants.CONTACT_LINK, fromEmail);
            mailTemplateVariables.put("operation", operation);
            mailTemplateVariables.put("enrollService", enrollService);
            mailTemplateVariables.put("supportEmail", supportEmail);
            mailTemplateVariables.put("keyUsage", keyUsage);
            mailTemplateVariables.put("certStartDate", certMetaData != null ? Objects.requireNonNull(certMetaData).getCreateDate() : null);
            mailTemplateVariables.put("certEndDate", certMetaData != null ? Objects.requireNonNull(certMetaData).getExpiryDate() : null);

            if(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
            if (Objects.nonNull(Objects.requireNonNull(certMetaData).getDnsNames())) {
                //Removing first and last char from String
                mailTemplateVariables.put(SSLCertificateConstants.DNS_NAMES, certMetaData.getDnsNames().toString().
                        substring(1, certMetaData.getDnsNames().toString().length() - 1));
                }
            }else {
                if (Objects.nonNull(Objects.requireNonNull(certMetaData).getDnsNames())) {
                    if (isMockingEnabled(certType)) {
                        //Removing first and last char from String
                        mailTemplateVariables.put(SSLCertificateConstants.DNS_NAMES, certMetaData.getDnsNames().toString().
                                substring(1, certMetaData.getDnsNames().toString().length() - 1));

                    } else {
                        mailTemplateVariables.put(SSLCertificateConstants.DNS_NAMES, certMetaData.getDnsNames().toString().
                                substring(3, certMetaData.getDnsNames().toString().length() - 3));
                    }
                }
            }

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, String.format("sendEmail for SSL certificate [%s] - certType [%s] - User " +
                                    "email=[%s] - subject = [%s]"
                            ,certName , certType,directoryUser.getUserEmail(),subject)).
                    put(LogMessage.MESSAGE, String.format("Certificate [%s] successfully [%s] [%s]  by [%s] on " +
                            "[%s]",operation,certName,operation,certOwnerNtId,LocalDateTime.now())).
                    build()));

            emailUtils.sendHtmlEmalFromTemplateForInternalCert(fromEmail, certOwnerEmailId, subject, mailTemplateVariables);
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "sendEmail").
                    put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   " +
                                    "for an user name =  [%s] ,  Emails might not send to customer for an certificate = [%s]",
                            certOwnerEmailId,certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }

    /**
     * To get key usage actual name from key usage label.
     * @param keyUsageLabel
     * @return
     */
    private String getActualKeyUsageValue(String keyUsageLabel) {

        String keyUsageValue = "";
        if (!StringUtils.isEmpty(keyUsageLabel)) {
            if (keyUsageLabel.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_SERVER_LABEL)) {
                keyUsageValue = SSLCertificateConstants.INTERNAL_KEY_USAGE;
            } else if (keyUsageLabel.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_CLIENT_LABEL)) {
                keyUsageValue = SSLCertificateConstants.INTERNAL_KEY_USAGE_CLIENT;
            } else if (keyUsageLabel.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_BOTH_LABEL)) {
                keyUsageValue = SSLCertificateConstants.EXTERNAL_KEY_USAGE;
            }
        }
        return keyUsageValue;
    }


    /**
     *
     * @param certType
     * @param certName
     * @param userDetails
     */
    private void sendExternalEmail(String certType, String certName, String certOwnerEmailId,String certOwnerNtId,
                                   String subject,
                                   String operation) {
        DirectoryUser directoryUser = getUserDetails(certOwnerNtId);
        if (directoryUser != null) {
            String enrollService = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_CERT_ENROLL_STRING :
                    SSLCertificateConstants.EXTERNAL_CERT_ENROLL_STRING);

            String keyUsage = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_KEY_USAGE :
                    SSLCertificateConstants.EXTERNAL_KEY_USAGE);
            // set template variables
            Map<String, String> mailTemplateVariables = new HashMap<>();
            mailTemplateVariables.put("name", directoryUser.getDisplayName());
            mailTemplateVariables.put(SSLCertificateConstants.CERT_TYPE, StringUtils.capitalize(certType));
            mailTemplateVariables.put(SSLCertificateConstants.CERT_NAME, certName);
            mailTemplateVariables.put(SSLCertificateConstants.CONTACT_LINK, fromEmail);
            mailTemplateVariables.put("supportEmail", supportEmail);
            mailTemplateVariables.put("operation", operation);
            mailTemplateVariables.put("enrollService", enrollService);
            mailTemplateVariables.put("keyUsage", keyUsage);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, String.format("sendEmail for SSL certificate [%s] - certType [%s] - User " +
                                    "email=[%s] - subject = [%s]"
                            ,certName , certType,certOwnerEmailId,subject))
                    .build()));
            emailUtils.sendEmailForExternalCert(fromEmail, directoryUser.getUserEmail(),
                    subject, mailTemplateVariables);
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "sendEmail").
                    put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   " +
                                    "for an user name =  [%s] ,  Emails might not send to customer for an certificate = [%s]",
                            certOwnerNtId,certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }

	/**
	 * Method to populate certificate metadata details
	 * @param sslCertificateRequest
	 * @param userDetails
	 * @param certManagerLogin
	 * @return
	 * @throws Exception
	 */
	private String populateSSLCertificateMetadata(SSLCertificateRequest sslCertificateRequest, UserDetails userDetails,
            CertManagerLogin certManagerLogin,int actionId) throws Exception {
		
		String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	            SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
	    int containerId = getContainerId(sslCertificateRequest);
	    
	    String certMetadataPath = metaDataPath + '/' + sslCertificateRequest.getCertificateName();		

        SSLCertificateMetadataDetails sslCertificateMetadataDetails = new SSLCertificateMetadataDetails();

        //Get Application details
        String applicationName = sslCertificateRequest.getAppName();
        ResponseEntity<String> appResponse = workloadDetailsService.getWorkloadDetailsByAppName(applicationName);
        if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
            JsonParser jsonParser = new JsonParser();
            JsonObject response = (JsonObject) jsonParser.parse(appResponse.getBody());
        JsonObject jsonElement = null;
        if (Objects.nonNull(response)) {
            jsonElement = response.get("spec").getAsJsonObject();
            if (Objects.nonNull(jsonElement)) {
                String applicationTag = validateString(jsonElement.get("tag"));
                String projectLeadEmail = validateString(jsonElement.get("projectLeadEmail"));
                String appOwnerEmail = validateString(jsonElement.get("brtContactEmail"));
                String akmid = validateString(jsonElement.get("akmid"));
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION,"Populate Application details in SSL Certificate Metadata").
                        put(LogMessage.MESSAGE, String.format("Application Details  for an " +
                                        "applicationName = [%s] , applicationTag = [%s], " +
                                        "projectLeadEmail =  [%s],appOwnerEmail =  [%s], akmid = [%s]", applicationName,
                                applicationTag, projectLeadEmail, appOwnerEmail, akmid)).build()));

                sslCertificateMetadataDetails.setAkmid(akmid);
                sslCertificateMetadataDetails.setProjectLeadEmailId(projectLeadEmail);
                sslCertificateMetadataDetails.setApplicationOwnerEmailId(appOwnerEmail);
                sslCertificateMetadataDetails.setApplicationTag(applicationTag);
                sslCertificateMetadataDetails.setApplicationName(applicationName);
            }
            }
        } else {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "Getting Application Details by app name during Meta data creation ").
                    put(LogMessage.MESSAGE, String.format("Application details will not insert/update in metadata  " +
                                    "for an application =  [%s] ",  applicationName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }

        if(sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
        CertificateData certDetails = null;
        //Get Certificate Details
        for (int i = 1; i <= retrycount; i++) {
            Thread.sleep(delayTime);
            //Adding Mocking parameter
            certDetails = (!isMockingEnabled(sslCertificateRequest.getCertType()))?
                    getCertificate(sslCertificateRequest, certManagerLogin) :
                    nclmMockUtil.getMockCertificateData(sslCertificateRequest);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "Populate Certificate Details in SSL Certificate MetaData").
                    put(LogMessage.MESSAGE, String.format("Fetching certificate details count = [%s] and status = [%s]"
                            , i, Objects.nonNull(certDetails))).build()));
            if (Objects.nonNull(certDetails)) {
                break;
            }
        }
        if (Objects.nonNull(certDetails)) {
            sslCertificateMetadataDetails.setCertificateId(certDetails.getCertificateId());
            sslCertificateMetadataDetails.setCertificateName(certificateUtils.getVaultCompactibleCertifiacteName(certDetails.getCertificateName()));
            sslCertificateMetadataDetails.setCreateDate(certDetails.getCreateDate());
            sslCertificateMetadataDetails.setExpiryDate(certDetails.getExpiryDate());
            sslCertificateMetadataDetails.setAuthority(certDetails.getAuthority());
            sslCertificateMetadataDetails.setCertificateStatus(certDetails.getCertificateStatus());
            sslCertificateMetadataDetails.setContainerName(certDetails.getContainerName());
            sslCertificateMetadataDetails.setDnsNames(certDetails.getDnsNames());

        } else {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, String.format("Certificate Details to  not available for given " +
                    "certificate= [%s]", certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName()))).
                    build()));
        }
        sslCertificateMetadataDetails.setCertCreatedBy(userDetails.getUsername());
        sslCertificateMetadataDetails.setCertOwnerEmailId(sslCertificateRequest.getCertOwnerEmailId());
        sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
        sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());
        sslCertificateMetadataDetails.setContainerId(containerId);
		String[] notifEmailLst = sslCertificateRequest.getNotificationEmail().split(",");
		notifEmailLst = Arrays.stream(notifEmailLst).map(String::toLowerCase).distinct().toArray(String[]::new);
		sslCertificateMetadataDetails.setNotificationEmails(String.join(",", notifEmailLst));
		sslCertificateMetadataDetails.setKeyUsageValue(getKeyUsageValue(sslCertificateRequest.getKeyUsageValue()));

        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, String.format("MetaData info details= [%s]", sslCertificateMetadataDetails.toString())).
                build()));
        } else {
            //populate mock data for eternal certificate
            if(isMockingEnabled(sslCertificateRequest.getCertType())) {
                sslCertificateMetadataDetails = prepareMockdataForExternalCertificate(userDetails,actionId,
                        sslCertificateRequest,sslCertificateMetadataDetails);
              }
            else{
                sslCertificateMetadataDetails.setCertCreatedBy(userDetails.getUsername());
                sslCertificateMetadataDetails.setCertOwnerEmailId(sslCertificateRequest.getCertOwnerEmailId());
                sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
                sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());
                sslCertificateMetadataDetails.setContainerId(containerId);
                sslCertificateMetadataDetails.setCertificateName(sslCertificateRequest.getCertificateName());
                sslCertificateMetadataDetails.setRequestStatus(SSLCertificateConstants.REQUEST_PENDING_APPROVAL);
                sslCertificateMetadataDetails.setActionId(actionId);
				String[] notifEmailLst = sslCertificateRequest.getNotificationEmail().split(",");
				notifEmailLst = Arrays.stream(notifEmailLst).map(String::toLowerCase).distinct().toArray(String[]::new);
				sslCertificateMetadataDetails.setNotificationEmails(String.join(",", notifEmailLst));
            }
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, String.format("  MetaData info details = [%s] = for an external " +
                            "certificate= [%s]", sslCertificateMetadataDetails.toString(),
                            sslCertificateMetadataDetails.getCertificateName())).
                    build()));
        }

        SSLCertMetadata sslCertMetadata = new SSLCertMetadata(certMetadataPath, sslCertificateMetadataDetails);
        String jsonStr = JSONUtil.getJSON(sslCertMetadata);
        Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
        rqstParams.put("path", certMetadataPath);
        return ControllerUtil.convetToJson(rqstParams);
	}


    /**
     * Gte the value of selected extended key usage key
     * @param keyUsage
     * @return
     */
    private String getKeyUsageValue(String keyUsage) {
        String keyUsageValue = null;
        if (!StringUtils.isEmpty(keyUsage)) {
            if (keyUsage.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_SERVER)) {
                keyUsageValue = SSLCertificateConstants.KEYUSAGE_VALUE_SERVER_LABEL;
            } else if (keyUsage.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_CLIENT)) {
                keyUsageValue = SSLCertificateConstants.KEYUSAGE_VALUE_CLIENT_LABEL;
            } else if (keyUsage.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_BOTH)) {
                keyUsageValue = SSLCertificateConstants.KEYUSAGE_VALUE_BOTH_LABEL;
            }
        } else {
            keyUsageValue = SSLCertificateConstants.KEYUSAGE_VALUE_SERVER_LABEL;
        }
        return keyUsageValue;
    }


    /**
     * prepareMockdata for external certificate
     * @param userDetails
     * @param containerId
     * @param sslCertificateRequest
     * @return
     */
    private SSLCertificateMetadataDetails prepareMockdataForExternalCertificate(UserDetails userDetails,int containerId,
                                                                                SSLCertificateRequest sslCertificateRequest,
                                                                                SSLCertificateMetadataDetails sslCertificateMetadataDetails){
        CertificateData certDetails = nclmMockUtil.getMockCertificateData(sslCertificateRequest);
        sslCertificateMetadataDetails.setCertificateId(certDetails.getCertificateId());
        sslCertificateMetadataDetails.setCertificateName(certDetails.getCertificateName());
        sslCertificateMetadataDetails.setCreateDate(certDetails.getCreateDate());
        sslCertificateMetadataDetails.setExpiryDate(certDetails.getExpiryDate());
        sslCertificateMetadataDetails.setAuthority(certDetails.getAuthority());
        sslCertificateMetadataDetails.setCertificateStatus(certDetails.getCertificateStatus());
        sslCertificateMetadataDetails.setContainerName(certDetails.getContainerName());
        sslCertificateMetadataDetails.setDnsNames(certDetails.getDnsNames());
        sslCertificateMetadataDetails.setCertCreatedBy(userDetails.getUsername());
        sslCertificateMetadataDetails.setCertOwnerEmailId(sslCertificateRequest.getCertOwnerEmailId());
        sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
        sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());
        sslCertificateMetadataDetails.setContainerId(containerId);
        sslCertificateMetadataDetails.setRequestStatus(SSLCertificateConstants.APPROVED);
        String[] notifEmailLst = sslCertificateRequest.getNotificationEmail().split(",");
		notifEmailLst = Arrays.stream(notifEmailLst).map(String::toLowerCase).distinct().toArray(String[]::new);
		sslCertificateMetadataDetails.setNotificationEmails(String.join(",", notifEmailLst));
        return sslCertificateMetadataDetails;
    }


    /**
     * Validate the DNSNames
     * @param sslCertificateRequest
     * @param isAdmin
     * @return
     */
	private boolean validateDNSNames(SSLCertificateRequest sslCertificateRequest, boolean isAdmin) {
        String[] dnsNames = sslCertificateRequest.getDnsList();
        Set<String> set = new HashSet<>();

        if(!ArrayUtils.isEmpty(dnsNames)) {
	        for (String dnsName : dnsNames) {
	            if (dnsName.contains(" ") || (!dnsName.matches("^[a-zA-Z0-9*.-]+$")) || (dnsName.endsWith(certificateNameTailText)) ||
	                    (dnsName.contains(".-")) || (dnsName.contains("-.")) || (dnsName.contains("..")) || (dnsName.endsWith(".")) ||
                        ((dnsName.contains("*") && ((!isAdmin || !dnsName.startsWith("*."))))) || dnsName.startsWith(".") || (!set.add(dnsName))) {
	                return false;
	            }
	        }

        }
        return true;
    }
   
	/**
     * Validate input data
     * @param sslCertificateRequest
     * @return
     */
    private boolean validateInputData(SSLCertificateRequest sslCertificateRequest, UserDetails userDetails) {
        boolean isValid = true;
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                .put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_INPUT_DATA)
                .put(LogMessage.MESSAGE, "Trying to validate input data")
                .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
                .build()));
        if ((!validateCertficateName(sslCertificateRequest.getCertificateName(), userDetails.isAdmin())) || sslCertificateRequest.getAppName().contains(" ") ||
                (!populateCertOwnerEmaild(sslCertificateRequest, userDetails)) ||
                sslCertificateRequest.getCertOwnerEmailId().contains(" ") || sslCertificateRequest.getCertType().contains(" ") ||
                (!sslCertificateRequest.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))
                || (!validateDNSNames(sslCertificateRequest, userDetails.isAdmin())) ||
                (!validateNotificationEmailsForOnboard(sslCertificateRequest.getNotificationEmail())) ||
                (!validatekeyUsageValue(sslCertificateRequest))) {
            isValid = false;
        }
        return isValid;
    }

    /**
     * This method will be used to validae the extended key usage value
     * @param sslCertificateRequest
     * @return
     */
    private boolean validatekeyUsageValue(SSLCertificateRequest sslCertificateRequest) {
        boolean isValidKeyValue = false;
        if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
            String keyValue = sslCertificateRequest.getKeyUsageValue();
            if (StringUtils.isEmpty(keyValue) || keyValue.equalsIgnoreCase("null")
                    || (keyValue.matches(SSLCertificateConstants.KEYUSAGE_VALUE_VALID_STRING))) {
                isValidKeyValue = true;
            }
        } else if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)) {
            isValidKeyValue = true;
        }
        return isValidKeyValue;
    }

	/**
	 * Method to validate the certificate name
	 *
	 * @param certName
	 * @param isAdmin
     * @return
	 */
	private boolean validateCertficateName(String certName, boolean isAdmin) {
		boolean isValid = true;
		if (certName.contains(" ") || (certName.endsWith(certificateNameTailText)) || (certName.contains(".-"))
				|| (certName.contains("-.")) || (certName.contains("..")) || (certName.endsWith("."))
                || ((certName.contains("*") && ((!isAdmin) || !certName.startsWith("*.")))) || certName.startsWith(".")) {
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Method to set the target system, target system services and append t-mobile.com text 
	 * to certificate name and dns
	 *
	 * @param sslCertificateRequest
	 */
	private void populateSSLCertificateRequest(SSLCertificateRequest sslCertificateRequest, String appName) {
		String certName = sslCertificateRequest.getCertificateName() + certificateNameTailText;
		sslCertificateRequest.setCertificateName(certName);

		String[] dnsNames = sslCertificateRequest.getDnsList();

		if (!ArrayUtils.isEmpty(dnsNames)) {
			String[] dnsArray = Arrays.stream(dnsNames).map(value -> value + certificateNameTailText)
					.toArray(String[]::new);
			sslCertificateRequest.setDnsList(dnsArray);
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_SSL_CERTIFICATE_REQUEST)
				.put(LogMessage.MESSAGE, "Trying to populate SSL certificate request")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));
		TargetSystem targetSystem = new TargetSystem();
		targetSystem.setName(appName);
		targetSystem.setAddress(appName.replaceAll("[^a-zA-Z0-9]",""));
		sslCertificateRequest.setTargetSystem(targetSystem);

		TargetSystemServiceRequest targetSystemService = new TargetSystemServiceRequest();
		targetSystemService.setName(sslCertificateRequest.getCertificateName());
		targetSystemService.setPort(Integer.parseInt(SSLCertificateConstants.NCLM_TARGET_PORT_NUMBER));
		sslCertificateRequest.setTargetSystemServiceRequest(targetSystemService);
	}

	/**
	 * Method to get the application name
	 *
	 * @param sslCertificateRequest
	 * @return
	 */
	private String getValidApplicationName(SSLCertificateRequest sslCertificateRequest) {
		String appName = sslCertificateRequest.getAppName();
		if((sslCertificateRequest.getAppName().equalsIgnoreCase(SSLCertificateConstants.APP_NAME_OTHER))) {
			appName=null;
			return appName;
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.GET_VALID_APPLICATION_NAME)
				.put(LogMessage.MESSAGE, "Trying to get the application name")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));	
		ResponseEntity<String> appResponse = workloadDetailsService
				.getWorkloadDetailsByAppName(appName);
		if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
			JsonParser jsonParser = new JsonParser();
			JsonObject response = (JsonObject) jsonParser.parse(appResponse.getBody());
			JsonObject jsonElement = null;
			if (Objects.nonNull(response)) {
				jsonElement = response.get("spec").getAsJsonObject();
				if (Objects.nonNull(jsonElement) && !StringUtils.isEmpty(jsonElement.get("tag"))) {
					appName = jsonElement.get("tag").getAsString();					
				}
			}
		}else {
			appName = null;
		}
		return appName;
	}


	/**
     * To create r/w/o/d policies
     * @param sslCertificateRequest
     * @param token
     * @return
     */
    private boolean createPolicies(SSLCertificateRequest sslCertificateRequest, String token) {
        boolean policiesCreated = false;
        Map<String, Object> policyMap = new HashMap<>();
        Map<String, String> accessMap = new HashMap<>();
        String certificateName = certificateUtils.getVaultCompactibleCertifiacteName(sslCertificateRequest.getCertificateName());
        
        String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;

		String certPathVal = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
				SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

        String policyValue=(sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
        
        String certMetadataPath = metaDataPath +'/' +certificateName;
        String certPath = certPathVal + certificateName;

    	boolean isCertDataUpdated = certificateMetadataForPoliciesCreation(sslCertificateRequest, token, certPath);

		if(isCertDataUpdated) {

	        //Read Policy
	        accessMap.put(certPath , TVaultConstants.READ_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.READ_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.READ_CERT_POLICY_PREFIX +policyValue+"_" + certificateName);
	        policyMap.put(SSLCertificateConstants.ACCESS_STRING, accessMap);

	        String policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response readResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);
	        
	        if(readResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || readResponse.getHttpstatus().equals(HttpStatus.OK)){
				 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
		                    put(LogMessage.MESSAGE, String.format("[%s] -Read Policy Created - Completed", certificateName )).
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
						put(LogMessage.MESSAGE, String.format("[%s] -Read Policy Creation - FAILED", certificateName )).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}

	        //Write Policy
	        accessMap.put(certPath , TVaultConstants.WRITE_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX +policyValue+"_" + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response writeResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);
	        if(writeResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || writeResponse.getHttpstatus().equals(HttpStatus.OK)){
				 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
		                    put(LogMessage.MESSAGE, String.format("[%s] -Write Policy Created - Completed", certificateName )).
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
						put(LogMessage.MESSAGE, String.format("[%s] -Write Policy Creation - Failed", certificateName )).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}

	        //Deny Policy
	        accessMap.put(certPath , TVaultConstants.DENY_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.DENY_CERT_POLICY_PREFIX +policyValue+"_" + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response denyResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);
	        if(denyResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || denyResponse.getHttpstatus().equals(HttpStatus.OK)){
				 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
		                    put(LogMessage.MESSAGE, String.format("[%s] -Deny Policy Created - Completed", certificateName )).
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
						put(LogMessage.MESSAGE, String.format("[%s] -Deny Policy Creation - Failed", certificateName )).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}

	        //Owner Policy
	        accessMap.put(certPath , TVaultConstants.SUDO_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX +policyValue+"_" + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response sudoResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);
	        if(sudoResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || sudoResponse.getHttpstatus().equals(HttpStatus.OK)){
				 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
		                    put(LogMessage.MESSAGE, String.format("[%s] -Sudo Policy Created - Completed", certificateName )).
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
						put(LogMessage.MESSAGE, String.format("[%s] -Sudo Policy Creation - Failed", certificateName )).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}

	        if ((readResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
	        		writeResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
	        		denyResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
	                &&  sudoResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
	        ) ||
	                (readResponse.getHttpstatus().equals(HttpStatus.OK) &&
	                		writeResponse.getHttpstatus().equals(HttpStatus.OK) &&
	                		denyResponse.getHttpstatus().equals(HttpStatus.OK))
	              && sudoResponse.getHttpstatus().equals(HttpStatus.OK)
	        ) {
	            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, String.format("[%s] -Policies Creation - Completed", certificateName )).
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
	            policiesCreated = true;
	        } else {
	            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, String.format("[%s] -Policies Creation - Failed", certificateName )).
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
	        }
		}
        return policiesCreated;
    }

	/**
	 * Method to create certificate metadata in sslcerts/externalcerts mount
	 * @param sslCertificateRequest
	 * @param token
	 * @param certPath
	 * @return
	 */
    private boolean certificateMetadataForPoliciesCreation(SSLCertificateRequest sslCertificateRequest, String token,
			String certPath) {
		SSLCertificateMetadataDetails sslCertificateMetadataDetails = new SSLCertificateMetadataDetails();

    	sslCertificateMetadataDetails.setApplicationName(sslCertificateRequest.getAppName());
		sslCertificateMetadataDetails.setCertificateName(
				certificateUtils.getVaultCompactibleCertifiacteName(sslCertificateRequest.getCertificateName()));
    	sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
    	sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());

    	SSLCertMetadata sslCertMetadata = new SSLCertMetadata(certPath, sslCertificateMetadataDetails);
        String jsonStr = JSONUtil.getJSON(sslCertMetadata);
        Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
        rqstParams.put("path", certPath);
        String certDataJson = ControllerUtil.convetToJson(rqstParams);

		Response response = reqProcessor.process("/write", certDataJson, token);

		boolean isCertDataUpdated = false;

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, "SSL certificate metadata creation success for policy creation").
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
			 isCertDataUpdated = true;
		}else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
					put(LogMessage.MESSAGE, "SSL certificate metadata creation failed for policy creation").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return isCertDataUpdated;
	}

    /**
     * THis method will be responsible to check the whether given certificate exists or not
     * @param sslCertificateRequest
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private CertificateData getCertificate(SSLCertificateRequest sslCertificateRequest, CertManagerLogin certManagerLogin) throws Exception {
        CertificateData certificateData=null;
        String certName = certificateUtils.getActualCertifiacteName(sslCertificateRequest.getCertificateName());
        int containerId = getContainerId(sslCertificateRequest);
        String findCertificateEndpoint = "/certmanager/findCertificate";
        String targetEndpoint = findCertificate.replace("certname", String.valueOf(certName)).replace("cid", String.valueOf(containerId));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE)
                .put(LogMessage.MESSAGE, String.format("Trying to get Info for the SSL Certifcate [%s]", certificateUtils.getActualCertifiacteName(certName)))
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
        		.build()));
        CertResponse response = reqProcessor.processCert(findCertificateEndpoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetEndpoint));
        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.CERTIFICATES) != null)) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
                certificateData = setLatestCertificate(certificateData, certName, jsonArray);
            }
        }
         return certificateData;
    }

    private String validateString(JsonElement jsonElement){
        return (!StringUtils.isEmpty(jsonElement) ? (jsonElement.getAsString()):null);
    }

    /**
     * Get Certificate name
     * @param certData
     * @return
     */
    private String getCertficateName(String certData){
        String[] list = certData.split(",");
        for (String str : list) {
            String[] values = str.split("=");
            if (values[0].equalsIgnoreCase("CN"))
                return values[1];
        }
        return null;
    }
    
    /**
     * Get targetSystemServiceIds 
     * @param certData
     * @return
     */
    private List<Integer> getTargetSystemServiceIds(JsonArray jArray){
    List<Integer> listdata = new ArrayList<Integer>(); 
    
    if (jArray != null) { 
       for (int i=0;i<jArray.size();i++){ 
        listdata.add(jArray.get(i).getAsInt());
       } 
    } 
    return listdata;
    }
    /**
     * To check whether the given certificate already exists
     * @param sslCertificateRequest
     * @param targetSystemId
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private int getTargetSystemServiceId(SSLCertificateRequest sslCertificateRequest, int targetSystemId, CertManagerLogin certManagerLogin) throws Exception {
        int targetSystemServiceID = 0;
        String targetSystemName = sslCertificateRequest.getTargetSystemServiceRequest().getName();
        String getTargetSystemServiceEndpoint = "/certmanager/findTargetSystemService";
        String findTargetSystemServiceEndpoint = findTargetSystemService.replace("tsgid",
                String.valueOf(targetSystemId));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "getTargetSystemServiceId")
        		.put(LogMessage.MESSAGE, String.format("Trying to validate target system service ID for [%s]", targetSystemName))
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        CertResponse response = reqProcessor.processCert(getTargetSystemServiceEndpoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(findTargetSystemServiceEndpoint));

        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.TARGETSYSTEM_SERVICES) != null)) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEM_SERVICES);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    if (jsonElement.get(SSLCertificateConstants.NAME).getAsString().equalsIgnoreCase(targetSystemName)) {
                        targetSystemServiceID = jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_SERVICE_ID).getAsInt();
                        break;
                    }

                }
            }
        }
        return targetSystemServiceID;
    }


    /**
     * This method will be responsible for get the id of given Target System  if exists
     * @param sslCertificateRequest
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private int getTargetSystem(SSLCertificateRequest sslCertificateRequest, CertManagerLogin certManagerLogin) throws Exception {
        int targetSystemID = 0;
        String targetSystemName = sslCertificateRequest.getTargetSystem().getName();
        String getTargetSystemEndpoint = "/certmanager/findTargetSystem";
        int containerId = getContainerId(sslCertificateRequest);
        String findTargetSystemEndpoint = findTargetSystem.replace("tsgid", String.valueOf(containerId));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "getTargetSystem")
        		.put(LogMessage.MESSAGE, String.format("Trying to create target System for [%s]", targetSystemName))
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(findTargetSystemEndpoint));
        if(HttpStatus.OK.equals(response.getHttpstatus())) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
        JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEMS);
        if (Objects.nonNull(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                if (jsonElement.get(SSLCertificateConstants.NAME).getAsString().equalsIgnoreCase(targetSystemName)) {
                    targetSystemID = jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_ID).getAsInt();
                }
            }
        }
        }
        else if(HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
        	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getTargetSystem ").
                    put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
        }
        return targetSystemID;
    }


    //Get Enroll CSR

    /**
     * Get the enroll csr details
     * @param certManagerLogin
     * @param entityid
     * @param templateid
     * @param sslCertificateRequest
     * @return
     * @throws Exception
     */
    private String getEnrollCSR(CertManagerLogin certManagerLogin, int entityid, int templateid, SSLCertificateRequest sslCertificateRequest) throws Exception {
        String enrollEndPoint = "/certmanager/getEnrollCSR";
        String enrollTemplateCA = enrollCSRUrl.replace(SSLCertificateConstants.TEMPLATE_ID, String.valueOf(templateid)).replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "getEnrollCSR")
        		.put(LogMessage.MESSAGE, "Trying to get the enroll CSR details")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        CertResponse response = reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
        String updatedRequest = updatedRequestWithCN(response.getResponse(), sslCertificateRequest);
        return updatedRequest;
    }

    //Update request with certificate name

    /**
     * This method will be responsible for updating request with certificate name
     * @param jsonString
     * @param sslCertificateRequest
     * @return
     */
    private String updatedRequestWithCN(String jsonString, SSLCertificateRequest sslCertificateRequest) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonString);
        JsonObject jsonObject1 = jsonObject.getAsJsonObject(SSLCertificateConstants.SUBJECT);
        JsonArray jsonArray = jsonObject1.getAsJsonArray(SSLCertificateConstants.ITEMS);
        JsonObject jsonObject2 = null;
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement jsonElement = jsonArray.get(i);
            jsonObject2 = jsonElement.getAsJsonObject();
            if (jsonObject2.get(SSLCertificateConstants.TYPENAME).getAsString().equals(SSLCertificateConstants.CN)) {
                JsonArray jsonArray2 = jsonElement.getAsJsonObject().getAsJsonArray(SSLCertificateConstants.VALUE);
				for (int j = 0; j < jsonArray2.size(); j++) {
					JsonElement jsonElement1 = jsonArray2.get(j);
					jsonObject2 = jsonElement1.getAsJsonObject();
					jsonObject2.addProperty(SSLCertificateConstants.VALUE, sslCertificateRequest.getCertificateName());
					if (j == 1) {
						break;
					}
				}
            }
			if (i == 1) {
				break;
			}
        }
        return jsonObject.toString();
    }


    //petEnrollCSR

    /**
     * Update the CSR details
     * @param certManagerLogin
     * @param entityid
     * @param updatedRequest
     * @return
     * @throws Exception
     */
    private CertResponse putEnrollCSR(CertManagerLogin certManagerLogin, int entityid, String updatedRequest) throws Exception {
        int enrollKeyId = 0;
        String enrollEndPoint = "/certmanager/putEnrollCSR";
        String enrollTemplateCA = enrollUpdateCSRUrl.replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "putEnrollCSR")
        		.put(LogMessage.MESSAGE, "Trying to update the enroll CSR details")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        return reqProcessor.processCert(enrollEndPoint, updatedRequest, certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
    }

    /**
     * Update the enroll keys
     * @param certManagerLogin
     * @param entityid
     * @param response
     * @param templateid
     * @return
     * @throws Exception
     */
    private int putEnrollKeys(CertManagerLogin certManagerLogin, int entityid, CertResponse response, int templateid) throws Exception {
        int enrollKeyId = 0;
        String enrollEndPoint = "/certmanager/putEnrollKeys";
        String enrollTemplateCA = enrollKeysUrl.replace(SSLCertificateConstants.TEMPLATE_ID, String.valueOf(templateid)).replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "putEnrollKeys")
        		.put(LogMessage.MESSAGE, "Trying to update the enroll keys")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        CertResponse certResponse = reqProcessor.processCert(enrollEndPoint, response.getResponse(), certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
        Map<String, Object> responseMap = ControllerUtil.parseJson(certResponse.getResponse());
        if (!MapUtils.isEmpty(responseMap)) {
            enrollKeyId = (Integer) responseMap.get(SSLCertificateConstants.SELECTED_ID);
        }
        return enrollKeyId;
    }

    /**
     * Get the enroll keys
     * @param certManagerLogin
     * @param entityid
     * @param templateid
     * @return
     * @throws Exception
     */
    private CertResponse getEnrollKeys(CertManagerLogin certManagerLogin, int entityid, int templateid) throws Exception {
        String enrollEndPoint = "/certmanager/getEnrollkeys";
        String enrollTemplateCA = enrollKeysUrl.replace(SSLCertificateConstants.TEMPLATE_ID, String.valueOf(templateid)).replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "getEnrollKeys")
        		.put(LogMessage.MESSAGE, "Trying to get the enroll keys")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
    }


    //putEnrollTemplates

    /**
     * update the enroll templates
     * @param certManagerLogin
     * @param entityid
     * @param response
     * @param caId
     * @return
     * @throws Exception
     */
    private int putEnrollTemplates(CertManagerLogin certManagerLogin, int entityid, CertResponse response, int caId) throws Exception {
        int enrollTemlateId = 0;
        String enrollEndPoint = "/certmanager/putEnrollTemplates";
        String enrollTempletEndpoint = enrollTemplateUrl.replace("caid", String.valueOf(caId)).replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "putEnrollTemplates")
        		.put(LogMessage.MESSAGE, "Trying to update the enroll templates")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        CertResponse certResponse = reqProcessor.processCert(enrollEndPoint, response.getResponse(), certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTempletEndpoint));
        Map<String, Object> responseMap = ControllerUtil.parseJson(certResponse.getResponse());
        if (!MapUtils.isEmpty(responseMap)) {
            enrollTemlateId = (Integer) responseMap.get(SSLCertificateConstants.SELECTED_ID);
        }
        return enrollTemlateId;
    }

    //getEnrollTemplate

    /**
     * Get the enroll templates
     * @param certManagerLogin
     * @param entityid
     * @param caId
     * @return
     * @throws Exception
     */
    private CertResponse getEnrollTemplates(CertManagerLogin certManagerLogin, int entityid, int caId,
                                            SSLCertificateRequest sslCertificateRequest) throws Exception {
        String enrollEndPoint = "/certmanager/getEnrollTemplates";
        String enrollTemplateCA = enrollTemplateUrl.replace("caid", String.valueOf(caId)).replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "getEnrollTemplates")
        		.put(LogMessage.MESSAGE, "Trying to get the enroll templates")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        CertResponse certResponse =  reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(),
                getCertmanagerEndPoint(enrollTemplateCA));
        if( (!StringUtils.isEmpty(sslCertificateRequest.getKeyUsageValue()))
                && (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))) {
            String keyUsageValue = sslCertificateRequest.getKeyUsageValue();
            if (keyUsageValue.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_CLIENT) || keyUsageValue.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_BOTH)) {
                //Update the response with correct template
                prepareResponseWithTemplateSelectionId(certResponse, keyUsageValue);
            }
        }
        return certResponse;
    }

    /**
     * Thi method will be used to update the template id based on selected extended key usage (client/server/both)
     * @param response
     * @param keyUsageValue
     * @return
     */
    private CertResponse prepareResponseWithTemplateSelectionId(CertResponse response,String keyUsageValue){
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(response.getResponse(),JsonObject.class);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json.toString());
        JsonObject jsonObject1 = jsonObject.getAsJsonObject("template");
        JsonArray jsonArray = jsonObject1.getAsJsonArray("items");
        JsonObject jsonObject2 = null;
        int selectedId = 0;

        for (int i = 0; i < jsonArray.size(); i++) {
            jsonObject2 = jsonArray.get(i).getAsJsonObject();
            if (keyUsageValue.equalsIgnoreCase(SSLCertificateConstants.KEYUSAGE_VALUE_BOTH)) {
                if (jsonObject2.get("displayName").getAsString().equals(SSLCertificateConstants.CLIENT_SERVER_TEMPLATE_NAME)) {
                    selectedId = Integer.parseInt(jsonObject2.get("policyLinkId").getAsString());
                    break;
                }
            } else  {
                if (jsonObject2.get("displayName").getAsString().equals(SSLCertificateConstants.CLIENT_TEMPLATE_NAME)) {
                    selectedId = Integer.parseInt(jsonObject2.get("policyLinkId").getAsString());
                    break;
                }
            }
        }

        jsonObject1.addProperty("selectedId",selectedId);
        response.setResponse(jsonObject.toString());
        return response;
    }


    //Update the CA

    /**
     * Update the enroll CA
     * @param certManagerLogin
     * @param entityid
     * @param response
     * @return
     * @throws Exception
     */
    private int putEnrollCA(CertManagerLogin certManagerLogin, int entityid, CertResponse response) throws Exception {
        int selectedId = 0;
        String enrollEndPoint = "/certmanager/putEnrollCA";
        String enrollCA = enrollCAUrl.replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "putEnrollCA").put(LogMessage.MESSAGE, "Trying to update the enroll CA")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        CertResponse certResponse = reqProcessor.processCert(enrollEndPoint, response.getResponse(), certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollCA));
        Map<String, Object> responseMap = ControllerUtil.parseJson(certResponse.getResponse());
        if (!MapUtils.isEmpty(responseMap)) {
            selectedId = (Integer) responseMap.get(SSLCertificateConstants.SELECTED_ID);
        }
        return selectedId;
    }


    //Get the Enroll CA

    /**
     * Update the enroll CA
     * @param certManagerLogin
     * @param entityid
     * @return
     * @throws Exception
     */
    private CertResponse getEnrollCA(CertManagerLogin certManagerLogin, int entityid) throws Exception {
        int selectedId = 0;
        String enrollEndPoint = "/certmanager/getEnrollCA";
        String enrollCA = enrollCAUrl.replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityid));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "getEnrollCA")
        		.put(LogMessage.MESSAGE, "Trying to get the enroll CA")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollCA));
    }


    //Enroll Certificate

    /**
     * This method will be responsible to create certificate
     * @param certManagerLogin
     * @param entityId
     * @return
     * @throws Exception
     */
    private CertResponse enrollCertificate(CertManagerLogin certManagerLogin, int entityId) throws Exception {
        String enrollEndPoint = "/certmanager/enroll";
        String targetSystemEndPoint = enrollUrl.replace(SSLCertificateConstants.ENTITY_ID, String.valueOf(entityId));
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, "enrollCertificate")
        		.put(LogMessage.MESSAGE, "Trying to enroll certificate")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
        		.build()));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetSystemEndPoint));
    }

    //Create Target System Service

    /**
     * This method will be responsible to create target system service
     * @param targetSystemServiceRequest
     * @param targetSystemId
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private TargetSystemService createTargetSystemService(TargetSystemServiceRequest targetSystemServiceRequest, int targetSystemId,
                                                          CertManagerLogin certManagerLogin) throws Exception {
        TargetSystemService targetSystemService = null;
        String createTargetSystemEndPoint = "/certmanager/targetsystemservice/create";
        String targetSystemAPIEndpoint = new StringBuffer().append(targetSystems).append("/").
                append(targetSystemId).
                append("/").
                append(targetSystemServies).toString();
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "createTargetSystemService").
                put(LogMessage.MESSAGE, String.format("Trying to create target System Service [%s]", targetSystemServiceRequest)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        CertResponse response = reqProcessor.processCert(createTargetSystemEndPoint, targetSystemServiceRequest, certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetSystemAPIEndpoint));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystemService ").
                    put(LogMessage.MESSAGE, "Creation of TargetSystem Service Successful.").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
            if (!MapUtils.isEmpty(responseMap)) {
                String tss_hostname = (String) responseMap.get("hostname");
                String tss_name = (String) responseMap.get("name");
                int tss_port = (Integer) responseMap.get("port");
                int tss_groupId = (Integer) responseMap.get("targetSystemGroupId");
                int tss_systemId = (Integer) responseMap.get("targetSystemId");
                int tss_systemServiceId = (Integer) responseMap.get("targetSystemServiceId");
                targetSystemService = new TargetSystemService(tss_hostname, tss_name, tss_port, tss_groupId, tss_systemId, tss_systemServiceId);
            }
            return targetSystemService;
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystemService").
                    put(LogMessage.MESSAGE, "Creation of TargetSystemService failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return targetSystemService;
        }

    }


    //Build TargetSystem Service

    /**
     * Prepare the request for target system service
     * @param sslCertificateRequest
     * @return
     */
    private TargetSystemServiceRequest prepareTargetSystemServiceRequest(SSLCertificateRequest sslCertificateRequest) {
        TargetSystemServiceRequest targetSysServiceRequest = sslCertificateRequest.getTargetSystemServiceRequest();
        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setPort(targetSysServiceRequest.getPort());
        targetSystemServiceRequest.setDescription(targetSysServiceRequest.getDescription());
        targetSystemServiceRequest.setName(targetSysServiceRequest.getName());
        targetSystemServiceRequest.setHostname(targetSysServiceRequest.getHostname());
        
        targetSystemServiceRequest.setMonitoringEnabled(Boolean.TRUE); 
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(Boolean.TRUE);

        return targetSystemServiceRequest;
    }


    /**
     * Creates a targetSystem
     *
     * @param targetSystemRequest
     * @param certManagerLogin
     * @return
     */
    private int createTargetSystem(TargetSystem targetSystemRequest, CertManagerLogin certManagerLogin,
                                 int  containerId) throws Exception {
        int  targetSystemId = 0;
        String createTargetSystemEndPoint = "/certmanager/targetsystem/create";
        String targetSystemAPIEndpoint = new StringBuffer().append(targetSystemGroups).
                append(containerId).
                append("/").
                append(targetSystems).toString();
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "createTargetSystems").
                put(LogMessage.MESSAGE, String.format("Trying to create target System [%s], [%s]", targetSystemRequest.getName(), targetSystemRequest.getAddress())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        CertResponse response = reqProcessor.processCert(createTargetSystemEndPoint, targetSystemRequest, certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetSystemAPIEndpoint));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystem").
                    put(LogMessage.MESSAGE, "Creation of TargetSystem Successful.").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
            if (!MapUtils.isEmpty(responseMap)) {
                 targetSystemId = (Integer) responseMap.get("targetSystemID");
            }
            return targetSystemId;
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystem").
                    put(LogMessage.MESSAGE, "Creation of TargetSystem failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return targetSystemId;
        }
    }

    /**
     * @param certManagerAPIEndpoint
     * @return
     */
    private String getCertmanagerEndPoint(String certManagerAPIEndpoint) {
        if (!StringUtils.isEmpty(certManagerAPIEndpoint) && !StringUtils.isEmpty(certManagerDomain)) {
            StringBuilder endPoint = new StringBuilder(certManagerDomain);
            endPoint.append(certManagerAPIEndpoint);
            return endPoint.toString();
        }
        return "";
    }

    /**
     * Get the target System Group ID
     * @param sslCertType
     * @return
     */
    private int getTargetSystemGroupId(SSLCertType sslCertType) {
        int ts_gp_id = private_single_san_ts_gp_id;
        switch (sslCertType) {
            case PRIVATE_SINGLE_SAN:
                ts_gp_id = private_single_san_ts_gp_id;
                break;
            case PUBLIC_SINGLE_SAN:
                ts_gp_id = public_single_san_ts_gp_id; //75
                break;
            case PUBLIC_MULTI_SAN:
                ts_gp_id = public_multi_san_ts_gp_id; //99
                break;
        }
        return ts_gp_id;
    }

    /**
     * Get ssl certificate metadata list
     * @param token
     * @param userDetails
     * @param certName
     * @return
     * @throws Exception
     */
    public ResponseEntity<String> getServiceCertificates(String token, UserDetails userDetails, String certName, Integer limit, Integer offset, String certType)  {
    	if(!certType.matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING)){
    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getServiceCertificate")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
    	}
    	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                  put(LogMessage.ACTION, "getServiceCertificate").
   			      put(LogMessage.MESSAGE, "Trying to get list of Ssl certificatests").
   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
   			      build()));
        String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
       	Response response;
       	String certListStr = "";

       	String tokenValue= (userDetails.isAdmin())? token :userDetails.getSelfSupportToken();
        response = getMetadata(tokenValue, metaDataPath);
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            certListStr = getsslmetadatalist(response.getResponse(),tokenValue,userDetails,certName,limit,offset,metaDataPath);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "GetServiceCertificates").
                    put(LogMessage.MESSAGE, "Certificates fetched from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
        }
        else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getServiceCertificates").
                    put(LogMessage.MESSAGE, "Reterived empty certificate list from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body(certListStr);
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                  put(LogMessage.ACTION, "getServiceCertificates").
                  put(LogMessage.MESSAGE, "Failed to get certificates list from metadata").
   			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
   			      build()));

   		return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
   	}

    /**
     * Get ssl certificate metadata list to manage
     * @param token
     * @param userDetails
     * @param certName
     * @return
     * @throws Exception
     */
    public ResponseEntity<String> getAllSSLCertificatesToManage(String token, UserDetails userDetails, String certName, Integer limit, Integer offset, String certType)  {
		if (!certType.matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "GetAllSSLCertificatesToManage")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "Get All SSLCertificates To Manage")
				.put(LogMessage.MESSAGE, "Trying to get list of Ssl certificatests to manage")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
        String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
		Response response;
		String certListStr = "";
		String tokenValue = (userDetails.isAdmin()) ? token : userDetails.getSelfSupportToken();
		if (userDetails.isAdmin()) {
			response = getMetadata(tokenValue, metaDataPath);
			if (HttpStatus.OK.equals(response.getHttpstatus())) {
				certListStr = getsslmetadatalist(response.getResponse(), tokenValue, userDetails, certName, limit,
						offset, metaDataPath);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "getAllSSLCertificatesToManage").
                        put(LogMessage.MESSAGE, "Certificates fetched from metadata").
                        put(LogMessage.STATUS, response.getHttpstatus().toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
            }else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "getAllSSLCertificatesToManage").
                        put(LogMessage.MESSAGE, "Reterived empty certificate list from metadata").
                        put(LogMessage.STATUS, response.getHttpstatus().toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.OK).body(certListStr);
			}
		} else {
			List<String> certificateNames = getAllOwnedCertificateFromPermissionsByCertType(userDetails, certType);
			List<String> certNames = getMatchedCertificates(certificateNames, certName);
			if (!CollectionUtils.isEmpty(certNames)) {
				certListStr = getAllCertificateListFromPermissions(userDetails, metaDataPath, certNames, limit, offset);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "get All SSLCertificates To Manage").
                        put(LogMessage.MESSAGE, "Certificates fetched from metadata").
                        put(LogMessage.STATUS, HttpStatus.OK.toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.OK).body(certListStr);
			}else {
				JsonObject metadataJsonObj = new JsonObject();
				JsonArray responseArray = new JsonArray();
				metadataJsonObj.add("keys", responseArray);
				metadataJsonObj.addProperty("total", "0");
				metadataJsonObj.addProperty("next", "-1");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "Get all SSL Certificates to manage").
                        put(LogMessage.MESSAGE, "No certificates available for this user").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
				certListStr = metadataJsonObj.toString();
			}

		}

		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "get All SSLCertificates To Manage")
				.put(LogMessage.MESSAGE, "Failed to get certificates list from metadata")
				.put(LogMessage.STATUS, HttpStatus.OK.toString())
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		return ResponseEntity.status(HttpStatus.OK).body(certListStr);
	}

    /**
   	 * Get  for ssl certificate names
   	 * @param token
   	 * @param userDetails
   	 * @param path
   	 * @return
   	 */
   	private Response getMetadata(String token, String path) {

   		String pathStr = path+"?list=true";
   		return reqProcessor.process("/sslcert","{\"path\":\""+pathStr+"\"}",token);
   	}

    /**
   	 * Get metadata for each certificate
   	 * @param token
   	 * @param userDetails
   	 * @param path
   	 * @return
   	 */
	private String getsslmetadatalist(String certificateResponse, String token, UserDetails userDetails,
			String certName, Integer limit, Integer offset, String path) {

   		String pathStr= "";
   		String endPoint = "";
   		Response response = new Response();
   		JsonParser jsonParser = new JsonParser();
   		JsonArray responseArray = new JsonArray();
   		JsonObject metadataJsonObj=new JsonObject();
   		if(certificateResponse!=null) {
        JsonObject jsonObject = (JsonObject) jsonParser.parse(certificateResponse);
   		JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
		List<String> certNames = geMatchCertificates(jsonArray, certName);
		Integer totalCertCount = certNames.size();
   		if(limit == null || offset ==null) {
   			limit = certNames.size();
   			offset = 0;
   		}
   		
		if (!userDetails.isAdmin()) {
			responseArray = getMetadataForUser(certNames, userDetails,path,limit,offset);
		} else {
			int maxVal = certNames.size()> (limit+offset)?limit+offset : certNames.size();
			for (int i = offset; i < maxVal; i++) {
				endPoint = certNames.get(i).replaceAll(CERTNAMEREGEX, "");
				pathStr = path + '/' + endPoint;
				response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);
				if (HttpStatus.OK.equals(response.getHttpstatus())) {
					JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "getsslmetadatalist")
							.put(LogMessage.MESSAGE, String.format("CertificatesName [%s] and Status [%s] is ",
									(object.get("certificateName") != null) ? object.get("certificateName").getAsString()
											: "",
									response.getHttpstatus()))
							.build()));
					object.addProperty("certificateName", certificateUtils.getActualCertifiacteName(object.get("certificateName").getAsString()));
					responseArray.add(object);
				}
			}
		}

   		if(ObjectUtils.isEmpty(responseArray)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
 	   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
 	   				  put(LogMessage.ACTION, "get ssl metadata").
 	   			      put(LogMessage.MESSAGE, "Certificates metadata is not available").
 	   			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
 	   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
 	   			      build()));
   		}
		metadataJsonObj.add("keys", responseArray);
		metadataJsonObj.addProperty("total", String.valueOf(totalCertCount));
		metadataJsonObj.addProperty("next",
				(totalCertCount - (responseArray.size() + offset)>0?String.valueOf((responseArray.size() + offset)):"-1"));
   		}
   		return metadataJsonObj.toString();
   	}

  	/**
   	 * Get the certificate names matches the search keyword
   	 * @param jsonArray
   	 * @param searchText
   	 * @return
   	 */
	private List<String> geMatchCertificates(JsonArray jsonArray, String searchText) {
		List<String> list = new ArrayList<>();
		if (!ObjectUtils.isEmpty(jsonArray)) {
		    if (!StringUtils.isEmpty(searchText)) {
				for (int i = 0; i < jsonArray.size(); i++) {
					if (jsonArray.get(i).toString().contains(searchText)) {
						list.add(jsonArray.get(i).getAsString());
					}
				}
			} else {
				for (int i = 0; i < jsonArray.size(); i++) {
					list.add(jsonArray.get(i).getAsString());
				}
			}
		}
		return list;
	}
   	
   	/**
   	 * To Get the metadata details for user
   	 * @param certNames
   	 * @param userDetails
   	 * @param limit
   	 * @param offset
   	 * @return
   	 */
	private JsonArray getMetadataForUser(List<String> certNames, UserDetails userDetails, String path, Integer limit,
			Integer offset) {
		Response response;
		String pathStr = "";
		String endPoint = "";
		int count = 0;
		JsonParser jsonParser = new JsonParser();
		JsonArray responseArray = new JsonArray();
		int maxVal = certNames.size() > (limit + offset) ? limit + offset : certNames.size();
		for (int i = 0; i < certNames.size(); i++) {
			endPoint = certNames.get(i).replaceAll(CERTNAMEREGEX, "");
			pathStr = path + '/' + endPoint;
			response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}",
					userDetails.getSelfSupportToken());
			if (HttpStatus.OK.equals(response.getHttpstatus()) && !ObjectUtils.isEmpty(response.getResponse())) {
				JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
		  				put(LogMessage.ACTION, "get certificateName and Status").
		  			    put(LogMessage.MESSAGE, String.format("CertificatesName [%s] and Status [%s] is ",object.get("certificateName")==null?"":object.get("certificateName").getAsString(),response.getHttpstatus())).
		  			    build()));
				if (userDetails.getUsername().equalsIgnoreCase(
						(object.get(SSLCertificateConstants.CERT_OWNER_NTID) != null ? object.get(SSLCertificateConstants.CERT_OWNER_NTID).getAsString() : ""))) {
					if (count >= offset && count < maxVal) {
						responseArray.add(object);
					}
					count++;
				}
			}
		}
		return responseArray;
	}

	/**	
     * To get nclm token	
     * @return	
     */	
    public String getNclmToken() {	
        String username = (Objects.nonNull(ControllerUtil.getNclmUsername())) ?	
                (new String(Base64.getDecoder().decode(ControllerUtil.getNclmUsername()))) :	
                (new String(Base64.getDecoder().decode(certManagerUsername)));	
        String password = (Objects.nonNull(ControllerUtil.getNclmPassword())) ?	
                (new String(Base64.getDecoder().decode(ControllerUtil.getNclmPassword()))) :	
                (new String(Base64.getDecoder().decode(certManagerPassword)));	
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest(username, password);	
        try {	
            CertManagerLogin certManagerLogin = login(certManagerLoginRequest);	
            if(!ObjectUtils.isEmpty(certManagerLogin)) {	
            return certManagerLogin.getAccess_token();	
            }	
            else {	
            	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).	
                        put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).	
                        put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).                        	
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).	
                        build()));	
                return null;	
            }	
        } catch (Exception e) {	
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                    put(LogMessage.ACTION, "getNclmToken").	
                    put(LogMessage.MESSAGE, "Failed to get nclm token").	
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                    build()));	
        }	
        return null;	
    }

    /**	
     * To get the list of target systems in a target system group.	
     * @param token	
     * @param userDetails	
     * @return	
     * @throws Exception	
     */	
    public ResponseEntity<String> getTargetSystemList(String token, UserDetails userDetails,String certType) throws Exception {	
    	if(!certType.matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING)){	
    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()	
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))	
					.put(LogMessage.ACTION, "GetTargetSystemList")	
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)	
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));	
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);	
    	}	
        String getTargetSystemEndpoint = "/certmanager/findTargetSystem";	
        SSLCertType sslCertType = certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)?	
                SSLCertType.valueOf(SSLCertificateConstants.PRIVATE_SINGLE_SAN): SSLCertType.valueOf(SSLCertificateConstants.PUBLIC_SINGLE_SAN);	
        String findTargetSystemEndpoint = findTargetSystem.replace("tsgid",	
                String.valueOf(getTargetSystemGroupId(sslCertType)));	
        List<TargetSystemDetails> targetSystemDetails = new ArrayList<>();	
        String nclmToken = getNclmToken();	
        if(!StringUtils.isEmpty(nclmToken)) {	
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", nclmToken,	
                getCertmanagerEndPoint(findTargetSystemEndpoint));	
        if (HttpStatus.OK.equals(response.getHttpstatus())) {	
            JsonParser jsonParser = new JsonParser();	
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());	
            if (jsonObject != null && jsonObject.get(SSLCertificateConstants.TARGETSYSTEMS) != null && !jsonObject.get(SSLCertificateConstants.TARGETSYSTEMS).toString().equalsIgnoreCase("null"))  {	
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEMS);	
                for (int i = 0; i < jsonArray.size(); i++) {	
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();	
                    targetSystemDetails.add(new TargetSystemDetails(jsonElement.get(SSLCertificateConstants.NAME).getAsString(),	
                            jsonElement.get(SSLCertificateConstants.DESCRIPTION).getAsString(),	
                            jsonElement.get(SSLCertificateConstants.ADDRESS).getAsString(),	
                            jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_ID).getAsString()));	
                }	
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                        put(LogMessage.ACTION, "getTargetSystemsList").	
                        put(LogMessage.MESSAGE, "Successfully retrieved target system list from NCLM").	
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                        build()));	
                return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemDetails)+"}");	
            }	
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                    put(LogMessage.ACTION, "getTargetSystemList").	
                    put(LogMessage.MESSAGE, "Retrieved empty target system list from NCLM").	
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                    build()));	
            return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemDetails)+"}");	
        }	
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {	
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                    put(LogMessage.ACTION, "GetTargetSystemList").	
                    put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).	
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                    build()));	
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + nclmErrorMessage + "\"]}");
        }	
        }else {	
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).	
                    put(LogMessage.ACTION, "geTargetSystemList").	
                    put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).	
                    build()));	
        	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
					.body(ERRORS + nclmErrorMessage + "\"]}");
        }	
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                put(LogMessage.ACTION, "getTargetSystemList").	
                put(LogMessage.MESSAGE, "Failed to get Target system list from NCLM").	
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                build()));	
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system list\"]}");
    }

    /**
     * Get service list from a target system.
     * @param token
     * @param userDetails
     * @param targetSystemId
     * @return
     */
    public ResponseEntity<String> getTargetSystemServiceList(String token, UserDetails userDetails, String targetSystemId) throws Exception {
        String getTargetSystemEndpoint = "/certmanager/targetsystemservicelist";
        String findTargetSystemEndpoint = findTargetSystemService.replace("tsgid", targetSystemId);

        List<TargetSystemServiceDetails> targetSystemServiceDetails = new ArrayList<>();
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", getNclmToken(),
                getCertmanagerEndPoint(findTargetSystemEndpoint));

        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null && jsonObject.get(SSLCertificateConstants.TARGETSYSTEM_SERVICES) != null && !jsonObject.get(SSLCertificateConstants.TARGETSYSTEM_SERVICES).toString().equalsIgnoreCase("null")) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEM_SERVICES);

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    targetSystemServiceDetails.add(new TargetSystemServiceDetails(jsonElement.get(SSLCertificateConstants.NAME).getAsString(),
                            jsonElement.get(SSLCertificateConstants.DESCRIPTION).getAsString(),
                            jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_SERVICE_ID).getAsString(),
                            jsonElement.get(SSLCertificateConstants.HOSTNAME).getAsString(),
                            jsonElement.get(SSLCertificateConstants.MONITORINGENABLED).getAsBoolean(),
                            jsonElement.get(SSLCertificateConstants.MULTIIPMONITORINGENABLED).getAsBoolean(),
                            jsonElement.get(SSLCertificateConstants.PORT).getAsInt()));
                }
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "GetTargetSystemServiceList").
                        put(LogMessage.MESSAGE, "Successfully retrieved target system service list from NCLM").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemServiceDetails)+"}");
            }
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "GetTargetSystemServiceList").
                    put(LogMessage.MESSAGE, "Retrieved empty target system service list from NCLM").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemServiceDetails)+"}");
        }
        else if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getHttpstatus())) {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getTargetSystemServiceList.").
                    put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + nclmErrorMessage + "\"]}");
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getTargetSystemServiceList").
                put(LogMessage.MESSAGE, String.format("Failed to get Target system service list from NCLM for the target system [%s]", targetSystemId)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system service list\"]}");

    }
  	
	/**	
 * Get Revocation Reasons.	
 * 	
 * @param certificateId	
 * @param token	
 * @return	
 */	
public ResponseEntity<String> getRevocationReasons(Integer certificateId, String token) {	
	CertResponse revocationReasons = new CertResponse();	
	try {	
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()	
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())	
				.put(LogMessage.ACTION, "getRevocationReasons")	
				.put(LogMessage.MESSAGE,	
						String.format("Trying to fetch Revocation Reasons for [%s]", certificateId))	
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())	
				.build()));


		String nclmAccessToken = nclmMockEnabled.equalsIgnoreCase(TVaultConstants.TRUE) ? TVaultConstants.NCLM_MOCK_VALUE :  getNclmToken();
		if(!StringUtils.isEmpty(nclmAccessToken)) {
            if(!nclmMockEnabled.equalsIgnoreCase(TVaultConstants.TRUE) ) {
                String nclmGetCertificateReasonsEndpoint = getCertifcateReasons.replace("certID", certificateId.toString());
                revocationReasons = reqProcessor.processCert("/certificates/revocationreasons", certificateId,
                        nclmAccessToken, getCertmanagerEndPoint(nclmGetCertificateReasonsEndpoint));
            } else {
                revocationReasons = nclmMockUtil.getMockRevocationReasons();
            }

		//check if NCLM is down	
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(revocationReasons.getHttpstatus())) {	
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
                    put(LogMessage.ACTION, "getRevocationReasons").	
                    put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).	
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
                    build()));	
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + nclmErrorMessage + "\"]}");
        }	
			
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()	
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())	
				.put(LogMessage.ACTION, "Fetch Revocation Reasons")	
				.put(LogMessage.MESSAGE, "Fetch Revocation Reasons")	
				.put(LogMessage.STATUS, revocationReasons.getHttpstatus().toString())	
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())	
				.build()));	
		return ResponseEntity.status(revocationReasons.getHttpstatus()).body(revocationReasons.getResponse());	
		}else {	
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).	
                    put(LogMessage.ACTION, "getRevocationeasons").	
                    put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).                        	
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).	
                    build()));	
        	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
					.body(ERRORS + nclmErrorMessage + "\"]}");
        }	
	} catch (TVaultValidationException error) {	
		log.error(	
				JSONUtil.getJSON(ImmutableMap.<String, String> builder()	
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())	
						.put(LogMessage.ACTION,	
								String.format(	
										"Inside  TVaultValidationException " + "Exception= [%s] =  Message [%s]",	
										Arrays.toString(error.getStackTrace()), error.getMessage()))	
						.build()));	
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ERRORS + "Certificate unavailable in " +
                "system." + "\"]}");
	} catch (Exception e) {	
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()	
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())	
				.put(LogMessage.ACTION, String.format("Exception = [%s] =  Message [%s]", 	
						Arrays.toString(e.getStackTrace()), e.getMessage()))	
				.build()));	
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
				.body(ERRORS + SSLCertificateConstants.SSL_CERTFICATE_REASONS_FAILED + "\"]}");	
	}	
}

    /**
    * Issue a revocation request for certificate
    *
    * @param certificateId
    * @param token
    * @param revocationRequest
    * @return
    * @throws IOException
    * @throws JsonMappingException
    * @throws JsonParseException
    */
	public ResponseEntity<String> issueRevocationRequest(String certType, String certificateName, UserDetails userDetails, String token,
			RevocationRequest revocationRequest) {

		revocationRequest.setTime(getCurrentLocalDateTimeStamp());
		if (!isValidInputs(certificateName, certType)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "revokeCertificate")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}

		Map<String, String> metaDataParams = new HashMap<String, String>();

		String endPoint = certificateName;
		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + endPoint;
		Response response = null;
		try {
			if (userDetails.isAdmin()) {
				response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", token);
			} else {
				response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}",
						userDetails.getSelfSupportToken());
			}
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Exception = [%s] =  Message [%s]", 
							Arrays.toString(e.getStackTrace()), response.getResponse()))
					.build()));
			return ResponseEntity.status(response.getHttpstatus())
					.body(ERRORS + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body(ERRORS + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);

		if (!userDetails.isAdmin()) {
			Boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certificateName,certType);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ERRORS + "Access denied: no permission to revoke certificate" + "\"]}");
			}
		}
		String certID = object.get(SSLCertificateConstants.CERTIFICATE_ID).getAsString();
		int containerId = object.get(SSLCertificateConstants.CONTAINER_ID).getAsInt();
		float value = Float.valueOf(certID);
		int certificateId = (int) value;
		CertResponse revocationResponse = new CertResponse();
		try {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Issue Revocation Request")
					.put(LogMessage.MESSAGE,
							String.format("Trying to issue Revocation Request for [%s]", certificateId))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			String nclmAccessToken = isMockingEnabled(certType) ?  TVaultConstants.NCLM_MOCK_VALUE :  getNclmToken();

			if(!StringUtils.isEmpty(nclmAccessToken)) {
			    if(!isMockingEnabled(certType)) {
                    CertificateData cerificatetData = getLatestCertificate(certificateName, nclmAccessToken, containerId);
                    if ((!ObjectUtils.isEmpty(cerificatetData) && cerificatetData.getCertificateId() != 0)) {
                        if (cerificatetData.getCertificateStatus().equalsIgnoreCase("Revoked")) {
                            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                    .body(ERRORS + "Certificate in Revoked status cannot be revoked." + "\"]}");
                        }
                    } else {
                        log.error(
                                JSONUtil.getJSON(
                                        ImmutableMap.<String, String>builder()
                                                .put(LogMessage.USER,
                                                        ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
                                                .put(LogMessage.ACTION, "Issue Revocation Request")
                                                .put(LogMessage.MESSAGE, "Revoke Request failed for CertificateID")
                                                .build()));
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ERRORS + "Certificate " +
                                "unavailable in system." + "\"]}");
                    }
                }
                //Depends on mock response property
                if(!isMockingEnabled(certType)) {
                    String nclmApiIssueRevocationEndpoint = issueRevocationRequest.replace("certID",
                            String.valueOf(certificateId));
                    revocationResponse = reqProcessor.processCert("/certificates/revocationrequest", revocationRequest,
                            nclmAccessToken, getCertmanagerEndPoint(nclmApiIssueRevocationEndpoint));
                } else {
                    revocationResponse =nclmMockUtil.getRevocationMockResponse();
                }
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Issue Revocation Request")
					.put(LogMessage.MESSAGE, String.format("Issue Revocation Request for [%s] requested by [%s] on [%s]", certificateName,userDetails.getUsername(),LocalDateTime.now()))
					.put(LogMessage.STATUS, revocationResponse.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(revocationResponse.getHttpstatus())) {
	        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, "Issue Revocation Request").
	                    put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + nclmErrorMessage + "\"]}");
	        }
			boolean sslMetaDataUpdationStatus;
			metaDataParams.put("certificateStatus", "Revoked");
			if (userDetails.isAdmin()) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams, token);
			} else {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams,
						userDetails.getSelfSupportToken());
			}

            String certOwnerEmailId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_EMAILID);
            String certOwnerNtId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_NTID);
			if (sslMetaDataUpdationStatus) {
			    //Send an email for revoke for internal and external
                sendEmail(certType, certificateName, certOwnerEmailId,certOwnerNtId,
                        SSLCertificateConstants.CERT_REVOKED_SUBJECT + " - " + certificateName,
                        "revoked", token);

                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                        .put(LogMessage.ACTION, String.format("CERTIFICATE [%s] - REVOKED SUCCESSFULLY - BY [%s] - " +
                                        "ON- [%s] AND TYPE [%s]",
                                certificateName, certOwnerEmailId, LocalDateTime.now(), certType)).build()));


				return ResponseEntity.status(revocationResponse.getHttpstatus())
						.body(MESSAGES + "Revocation done successfully" + "\"]}");
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "Revocation Request Failed")
						.put(LogMessage.MESSAGE, "Revocation Request failed for CertificateID")
						.put(LogMessage.STATUS, revocationResponse.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(ERRORS + "Revocation failed" + "\"]}");
			}			
			}else {	
            	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).	
                        put(LogMessage.ACTION, "Issue Revocation Request").	
                        put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).                        	
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).	
                        build()));	
            	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
    					.body(ERRORS + nclmErrorMessage + "\"]}");
            }

		} catch (TVaultValidationException error) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  TVaultValidationException  = [%s] =  Message [%s]", 
							Arrays.toString(error.getStackTrace()), error.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORS + error.getMessage() + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ERRORS + e.getMessage() + "\"]}");
		}
	}
	
	/**		
	 * Get Current Date and Time.
	 * 
	 * @return
	 */
	public String getCurrentLocalDateTimeStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
	}
	
	/**
	 * Validate Permission for Non-admin User.
	 * 
	 * @param userDetails
	 * @param certificateName
	 * @return
	 */
	public Boolean validateOwnerPermissionForNonAdmin(UserDetails userDetails, String certificateName) {
		String ownerPermissionCertName = SSLCertificateConstants.OWNER_PERMISSION_CERTIFICATE + certificateName;
		Boolean isPermission = false;
		if (ArrayUtils.isNotEmpty(userDetails.getPolicies())) {
			isPermission = Arrays.stream(userDetails.getPolicies()).anyMatch(ownerPermissionCertName::equals);
			if (isPermission) {
				log.debug(
						JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Certificate permission for user " + userDetails.getUsername())
								.put(LogMessage.MESSAGE,
										"User has permission to access the certificate " + certificateName)
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
				return isPermission;
			}
		}

		return isPermission;
	}
	
    /**
   	 * Adds permission to user for a certificate
   	 * @param token
   	 * @param safeUser
   	 * @return
   	 */
   	public ResponseEntity<String> addUserToCertificate(CertificateUser certificateUser, UserDetails userDetails, boolean addSudoPermission) {
   		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   				put(LogMessage.MESSAGE, "Trying to add user to Certificate folder ").
   				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   				build()));
   		
   		if(!areCertificateUserInputsValid(certificateUser)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG).
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
   		}
   		
   		String userName = certificateUser.getUsername().toLowerCase();
   		String certificateName = certificateUser.getCertificateName().toLowerCase();
   		String access = certificateUser.getAccess().toLowerCase();   
   		String certificateType = certificateUser.getCertType();
   		String authToken = null;
   		
   		boolean isAuthorized = true;
   		if (!ObjectUtils.isEmpty(userDetails)) {

   	        	authToken = userDetails.getSelfSupportToken();
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certificateType);
   			
   			if(!addSudoPermission){
   				isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
   			}
   			
   			if((!addSudoPermission) && (isAuthorized) && (userName.equalsIgnoreCase(certificateMetaData.getCertOwnerNtid()))) {
   				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   	   					put(LogMessage.MESSAGE, "Certificate owner cannot be added as a user to the certificate owned by him").
   	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   	   					build()));
   				
   				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate owner cannot be added as a user to the certificate owned by him\"]}");
   			}
   		}else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to add users to this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
   		}
   		
   		if(isAuthorized){   			
   			return checkUserDetailsAndAddCertificateToUser(authToken, userName, certificateName, access, certificateType, userDetails);
   		}else{
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to add users to this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
   		}
   	}

	/**
	 * Method to check the user details and add access policy to certificate
	 * @param token
	 * @param userName
	 * @param certificateName
	 * @param access
	 * @return
	 */
	private ResponseEntity<String> checkUserDetailsAndAddCertificateToUser(String token, String userName,
			String certificateName, String access, String certificateType, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		String policyPrefix = getCertificatePolicyPrefix(access, certificateType);
		
		String metaDataPath = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		
		String certificatePath = metaDataPath + certificateName;
		
		if(TVaultConstants.EMPTY.equals(policyPrefix)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Incorrect access requested. Valid values are read, write, deny").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,deny \"]}");
		}

		String policy = policyPrefix + certificateName;
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		String certPrefix=(certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s]", readPolicy, writePolicy, denyPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read", "{\"username\":\"" + userName + "\"}", token);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/ldap/users", "{\"username\":\"" + userName + "\"}", token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
            //OIDC Changes

			// OIDC implementation changes
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(token, userName, userDetails, true);
			if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, "Add User to SDB")
							.put(LogMessage.MESSAGE,
									"Trying to fetch OIDC user policies, failed")
							.put(LogMessage.APIURL,
									ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
							.build()));
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
							.body("{\"messages\":[\"User configuration failed. Please try again.\"]}");
				}
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
			}
			oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
			oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
			userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
			userResponse.setHttpstatus(responseEntity.getStatusCode());

		}
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse.getHttpstatus())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				ObjectMapper objMapper = new ObjectMapper();
				// OIDC Changes
				if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					currentpolicies.addAll(oidcEntityResponse.getPolicies());
				} else {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while getting the currentpolicies or groups").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
			
			policies.add(policy);
		}else{
			// New user to be configured
			policies.add(policy);
		}
		
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
 				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureUserpassUser/configureLDAPUser", policies)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		return configureUserpassOrLDAPUserToUpdateMetadata(token, userName, certificatePath, access, groups,
				policiesString, currentpoliciesString, userDetails, policies, currentpolicies,
				oidcEntityResponse.getEntityName());
		
	}

	private String getCertificatePolicyPrefix(String access, String certificateType) {
		String policyPrefix ="";
		String certPrefix=(certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;		
		switch (access){
			case TVaultConstants.READ_POLICY: policyPrefix = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"; break ;
			case TVaultConstants.WRITE_POLICY: policyPrefix = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"; break;
			case TVaultConstants.DENY_POLICY: policyPrefix = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"; break;
			case TVaultConstants.SUDO_POLICY: policyPrefix = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certPrefix+"_"; break;
			default: log.error(SSLCertificateConstants.ERROR_INVALID_ACCESS_POLICY_MSG); break;
		}
		return policyPrefix;
	}


	/**
	 * Method to configure the Userpass or ldap users and update metadata for add user to certificate
	 * @param token
	 * @param userName
	 * @param certificateName
	 * @param access
	 * @param groups
	 * @param policiesString
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> configureUserpassOrLDAPUserToUpdateMetadata(String token, String userName,
			String certificatePath, String access, String groups, String policiesString, String currentpoliciesString,
			UserDetails userDetails, List<String> policies, List<String> currentpolicies, String entityName) {
		Response ldapConfigresponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,policiesString,token);
		}
		else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			//OIDC Implementation : Entity Update
			try {

				ldapConfigresponse = oidcUtil.updateOIDCEntity(policies, entityName);
				oidcUtil.renewUserToken(userDetails.getClientToken());
			}catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "Add User to SDB").
						put(LogMessage.MESSAGE, "Exception while adding or updating the identity ").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}

		}

		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
			return updateMetadataForAddUserToCertificate(token, userName, certificatePath, access, groups,
					currentpoliciesString, userDetails, currentpolicies, entityName);
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Trying to configureUserpassUser/configureLDAPUser failed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");
		}
	}

	/**
	 * Method to update the metadata for user to add the certificate access policies
	 * @param token
	 * @param userName
	 * @param certificateName
	 * @param access
	 * @param groups
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForAddUserToCertificate(String token, String userName,
			String certificatePath, String access, String groups, String currentpoliciesString, UserDetails userDetails,
			List<String> currentpolicies, String entityName) {
		Response ldapConfigresponse = new Response();
		Map<String,String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name",userName);
		params.put("path",certificatePath);
		params.put(SSLCertificateConstants.ACCESS_STRING,access);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Trying to update metadata [%s]", params.toString())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format ("User is successfully associated with Certificate [%s] - User [%s] -Access [%s]", certificatePath, userName, access)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");		
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,currentpoliciesString,token);
			}
			else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,currentpoliciesString,groups,token);
			}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				//OIDC Implementation : Entity Update
				try {

					ldapConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies, entityName);
					oidcUtil.renewUserToken(userDetails.getClientToken());
				}catch (Exception e) {
					log.error(e);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "Add User to SDB").
							put(LogMessage.MESSAGE, "Exception while adding or updating the identity ").
							put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				log.debug("Reverting user policy update");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...Passed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");
			}else{
				log.debug("Reverting user policy update failed");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Contact Admin \"]}");
			}
		}
	} 
	
	/**
	 * Validates Certificate User inputs
	 * @param certificateUser
	 * @return boolean
	 */
	private boolean areCertificateUserInputsValid(CertificateUser certificateUser) {
		
		if (ObjectUtils.isEmpty(certificateUser)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateUser.getUsername())
				|| ObjectUtils.isEmpty(certificateUser.getAccess())
				|| ObjectUtils.isEmpty(certificateUser.getCertificateName())
				|| certificateUser.getCertificateName().contains(" ")
	            || (!certificateUser.getCertificateName().endsWith(certificateNameTailText))
	            || (certificateUser.getCertificateName().contains(".-"))
	            || (certificateUser.getCertificateName().contains("-."))
	            || (!certificateUser.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))
				) {
			return false;
		}
		boolean isValid = true;
		String access = certificateUser.getAccess();
		if (!ArrayUtils.contains(PERMISSIONS, access)) {
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Adds a group to a certificate
	 * @param userDetails
	 * @param userToken
	 * @param certificateGroup
	 * @return
	 */
	public ResponseEntity<String> addGroupToCertificate(UserDetails userDetails, String userToken, CertificateGroup certificateGroup) {
   		String authToken = null;
   		boolean isAuthorized = true;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG)
				.put(LogMessage.MESSAGE, "Trying to add group to Certificate folder")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));
   		if(!ControllerUtil.arecertificateGroupInputsValid(certificateGroup)) {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
	   					put(LogMessage.MESSAGE, "Invalid input values").
	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	   					build()));
   			
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			isAuthorized=isAuthorized(userDetails, certificateGroup.getCertificateName(), certificateGroup.getCertType());
   			if(isAuthorized){
   	   			return addingGroupToCertificate(authToken, certificateGroup, userDetails);
   	   		}else{
   	   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
   	   					put(LogMessage.MESSAGE, "Access denied: No permission to add groups to this certificate").
   	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   	   					build()));

   	   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this certificate\"]}");
   	   		}

   		}
   		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\\\":[\"Group is successfully associated with Certificate\"]}");
	}

	/**
	 * isAuthorizedh
	 * @param token
	 * @param certName
	 * @return
	 */
	public boolean isAuthorized(UserDetails userDetails, String certificatename, String certType) {
		String certName = certificatename;

		String powerToken = null;
		if (userDetails.isAdmin()) {
			powerToken = userDetails.getClientToken();
		} else {
			powerToken = userDetails.getSelfSupportToken();
		}

		SSLCertificateMetadataDetails sslMetaData = certificateUtils.getCertificateMetaData(powerToken, certName, certType);

		return certificateUtils.hasAddOrRemovePermission(userDetails, sslMetaData);

	}

	/**
	 * Adds group to a certificate
	 * @param token
	 * @param certificateGroup
	 * @return
	 */
	public ResponseEntity<String> addingGroupToCertificate(String token, CertificateGroup certificateGroup, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
				put(LogMessage.MESSAGE, "Trying to add Group to certificate").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));		

		//checking whether auth method is userpass or ldap//
		//we should set vaultAuthMethod=ldap//
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		} 		
		ObjectMapper objMapper = new ObjectMapper();
		String groupName = certificateGroup.getGroupname();
		String certificateName = certificateGroup.getCertificateName().toLowerCase();
		String access = certificateGroup.getAccess().toLowerCase();		
		String certType = certificateGroup.getCertType().toLowerCase();
		
		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;

		String certPathVal = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
				SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

        String policyValue=(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
        
        String certPath = certPathVal + certificateName;
		
		boolean canAddGroup = ControllerUtil.canAddCertPermission(metaDataPath, certificateName, token);
		String policyPrefix = getCertificatePolicyPrefix(access, certType);
		if(canAddGroup){
			String policy = policyPrefix + certificateName;
			
			String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+policyValue+"_"+certificateName;
			String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+policyValue+"_"+certificateName;
			String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+policyValue+"_"+certificateName;
			
			Response getGrpResp = new Response();
			if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				getGrpResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				//call read api with groupname
				oidcGroup= oidcUtil.getIdentityGroupDetails(groupName, token);
				if (oidcGroup != null) {
					getGrpResp.setHttpstatus(HttpStatus.OK);
					getGrpResp.setResponse(oidcGroup.getPolicies().toString());
				} else {
					getGrpResp.setHttpstatus(HttpStatus.BAD_REQUEST);
				}
			}
			String responseJson="";

			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(getGrpResp.getHttpstatus())){
				responseJson = getGrpResp.getResponse();
				try {
					//OIDC Changes
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && oidcGroup != null) {
						currentpolicies.addAll(oidcGroup.getPolicies());
					}
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);

				policies.add(policy);
			}else{
				// New group to be configured
				policies.add(policy);
			}

			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			Response ldapConfigresponse = new Response();
			// OIDC Changes
			if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, token);
			} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, policies, currentpolicies,
						oidcGroup != null ? oidcGroup.getId() : null);
				oidcUtil.renewUserToken(userDetails.getClientToken());
			}
			if (ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
					|| ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
				return updateMetadataForAddGroupToCertificate(token, groupName, certificateName, access, certPath,
						currentpoliciesString, userDetails, currentpolicies, oidcGroup != null ? oidcGroup.getId() : null);
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "Group configuration failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Try Again\"]}");
			}
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
	   					put(LogMessage.MESSAGE, "Access denied: No permission to add groups to this certificate").
	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	   					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this certificate\"]}");
		}
	}

	/**
	 * @param token
	 * @param groupName
	 * @param certificateName
	 * @param access
	 * @param certPath
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForAddGroupToCertificate(String token, String groupName,
			String certificateName, String access, String certPath, String currentpoliciesString,
			UserDetails userDetails, List<String> currentpolicies, String groupId) {
		Map<String,String> params = new HashMap<>();
		params.put("type", "groups");
		params.put("name",groupName);
		params.put("certificateName",certificateName);
		params.put("access",access);
		params.put("path", certPath);
		Response metadataResponse = ControllerUtil.updateSslCertificateMetadata(params,token);
		if(metadataResponse !=null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format ("Group configuration Success [%s] - Group [%s] -Access [%s]",certificateName, groupName, access)).
					put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
		} else {
			return revertPoliciesIfMetadataUpdateFailed(token, groupName, currentpoliciesString, metadataResponse,
					userDetails, currentpolicies, groupId);
		}
	}

	/**
	 * @param token
	 * @param groupName
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> revertPoliciesIfMetadataUpdateFailed(String token, String groupName,
			String currentpoliciesString, Response metadataResponse, UserDetails userDetails,
			List<String> currentpolicies, String groupId) {
		Response ldapConfigresponse = new Response();
		//OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString,
					token);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ldapConfigresponse = oidcUtil.updateGroupPolicies(token, groupName, currentpolicies,
					currentpolicies, groupId);
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Group configuration success").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Group configuration failed").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Contact Admin \"]}");
		}
	}

	/**
     * Associate Approle to Certificate
     * @param userDetails
     * @param certificateApprole
     * @return
     */
    public ResponseEntity<String> associateApproletoCertificate(CertificateApprole certificateApprole, UserDetails userDetails) {        
        String authToken = null;
        boolean isAuthorized = true;
        if(!areCertificateApproleInputsValid(certificateApprole)) {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Invalid input values").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
        }
        
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
                put(LogMessage.MESSAGE, String.format("Trying to add Approle to Certificate - Request [%s]", certificateApprole.toString())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String approleName = certificateApprole.getApproleName().toLowerCase();
        String certificateName = certificateApprole.getCertificateName().toLowerCase();
        String access = certificateApprole.getAccess().toLowerCase();
        String certType = certificateApprole.getCertType().toLowerCase();

		if (Arrays.asList(TVaultConstants.MASTER_APPROLES).contains(approleName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					"{\"errors\":[\"Access denied: no permission to associate this AppRole to any Certificate\"]}");
		}
        

        if (!ObjectUtils.isEmpty(userDetails)) {

	        if (userDetails.isAdmin()) {
	        	authToken = userDetails.getClientToken();
	        }else {
	        	authToken = userDetails.getSelfSupportToken();
	        }

	        SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certType);

			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);

        }else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to add approle to this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));

   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add approle to this certificate\"]}");
   		}

        if(isAuthorized){
        	return createPoliciesAndConfigureApproleToCertificate(authToken, approleName, certificateName, access, certType);
        } else{
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Access denied: No permission to add Approle [%s] to the Certificate [%s]", approleName, certificateName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this Certificate\"]}");
        }
    }

	/**
	 * @param authToken
	 * @param approleName
	 * @param certificateName
	 * @param access
	 * @return
	 */
	private ResponseEntity<String> createPoliciesAndConfigureApproleToCertificate(String authToken, String approleName,
			String certificateName, String access, String certType) {
		String policyPrefix = getCertificatePolicyPrefix(access, certType);
		
		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		
		String certificatePath = metaDataPath + certificateName;

		if(TVaultConstants.EMPTY.equals(policyPrefix)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Incorrect access requested. Valid values are read, write, deny").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,deny \"]}");
		}

		String policy = policyPrefix + certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		String certPrefix=(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;

		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Approle Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approleName+"\"}",authToken);

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
		        put(LogMessage.MESSAGE, String.format("roleResponse status is [%s]", roleResponse.getHttpstatus())).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
			responseJson = roleResponse.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
				if (null != policiesArry) {
					for (JsonNode policyNode : policiesArry) {
						currentpolicies.add(policyNode.asText());
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while creating currentpolicies").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
			policies.add(policy);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Non existing role name. Please configure approle as first step - Approle = [%s]", approleName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

		    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either Approle doesn't exists or you don't have enough permission to add this approle to Certificate\"]}");
		}

		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureApprole", policies)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		return configureApproleAndMetadataToCertificate(authToken, approleName, certificatePath, access,
				policiesString, currentpoliciesString);
	}

	/**
	 * @param authToken
	 * @param approleName
	 * @param certificatePath
	 * @param access
	 * @param policiesString
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> configureApproleAndMetadataToCertificate(String authToken, String approleName,
			String certificatePath, String access, String policiesString, String currentpoliciesString) {
		Response approleControllerResp = appRoleService.configureApprole(approleName, policiesString, authToken);

		if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT) || approleControllerResp.getHttpstatus().equals(HttpStatus.OK)){
			return updateApproleMetadataForCertificate(authToken, approleName, certificatePath, access,
					currentpoliciesString);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Failed to add Approle [%s] to the Certificate [%s]", approleName, certificatePath)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add Approle to the Certificate\"]}");
		}
	}

	/**
	 * @param authToken
	 * @param approleName
	 * @param certificatePath
	 * @param access
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateApproleMetadataForCertificate(String authToken, String approleName,
			String certificatePath, String access, String currentpoliciesString) {		
		Map<String,String> params = new HashMap<>();
		params.put("type", "app-roles");
		params.put("name",approleName);
		params.put("path",certificatePath);
		params.put("access",access);
		Response metadataResponse = ControllerUtil.updateMetadata(params, authToken);
		if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Approle [%s] successfully associated to Certificate [%s] with policy [%s]", approleName, certificatePath,access)).
					put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
		} else {
			return rollBackApprolePolicyForCertificate(authToken, approleName, currentpoliciesString, metadataResponse);
		}
	}

	/**
	 * @param authToken
	 * @param approleName
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> rollBackApprolePolicyForCertificate(String authToken, String approleName,
			String currentpoliciesString, Response metadataResponse) {
		Response approleControllerResp;
		approleControllerResp = appRoleService.configureApprole(approleName, currentpoliciesString, authToken);
		if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting, Approle policy update success").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting Approle policy update failed").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin\"]}");
		}
	}

	/**
	 * Validates Certificate approle inputs
	 * @param certificateApprole
	 * @return boolean
	 */
	private boolean areCertificateApproleInputsValid(CertificateApprole certificateApprole) {

		if (ObjectUtils.isEmpty(certificateApprole)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateApprole.getApproleName())
				|| ObjectUtils.isEmpty(certificateApprole.getAccess())
				|| ObjectUtils.isEmpty(certificateApprole.getCertificateName())
				|| certificateApprole.getCertificateName().contains(" ")
	            || (!certificateApprole.getCertificateName().endsWith(certificateNameTailText))
	            || (certificateApprole.getCertificateName().contains(".-"))
	            || (certificateApprole.getCertificateName().contains("-."))
	            || (!certificateApprole.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))
				) {
			return false;
		}
		boolean isValid = true;
		String access = certificateApprole.getAccess();
		if (!ArrayUtils.contains(PERMISSIONS, access)) {
			isValid = false;
		}
		return isValid;
	}

    /**
     * Check if user has download permission.
     * @param certificateName
     * @param userDetails
     * @return
     */
	public boolean hasDownloadPermission(String certificateName, UserDetails userDetails, String certType) {
		String certPrefix=(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;	
        String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX +certPrefix+"_" + certificateName;
        String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX +certPrefix+"_" + certificateName;
        String renewRevokePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX +certPrefix+"_" + certificateName;
        if (userDetails.isAdmin()) {
            return true;
        }
        VaultTokenLookupDetails  vaultTokenLookupDetails = null;
        try {
            vaultTokenLookupDetails = tokenValidator.getVaultTokenLookupDetails(userDetails.getClientToken());
        } catch (TVaultValidationException e) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "hasDownloadPermission").
                    put(LogMessage.MESSAGE, String.format ("Failed to get lookup details for user  [%s]", userDetails.getUsername())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return false;
        }
        String[] policies = vaultTokenLookupDetails.getPolicies();
        if (ArrayUtils.isNotEmpty(policies) && (Arrays.asList(policies).contains(readPolicy) || Arrays.asList(policies).contains(sudoPolicy) || Arrays.asList(policies).contains(renewRevokePolicy))) {
            return true;
        }
        return false;
    }

    public SSLCertificateMetadataDetails getCertificateMetadata(String token, String certificateName) {
        return certificateUtils.getCertificateMetaData(token, certificateName, SSLCertificateConstants.INTERNAL);
    }

    /**
     * Download certificate.
     * @param token
     * @param certificateDownloadRequest
     * @param userDetails
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadCertificateWithPrivateKey(String token, CertificateDownloadRequest certificateDownloadRequest, UserDetails userDetails) {

        String certName = certificateDownloadRequest.getCertificateName();
        String certType = certificateDownloadRequest.getCertType();
        if(!ControllerUtil.arecertificateDownloadInputsValid(certificateDownloadRequest)) {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	   					put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
	   					put(LogMessage.MESSAGE, "Invalid input values").
	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
        
        SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token, certName, certType);
        if (hasDownloadPermission(certificateDownloadRequest.getCertificateName(), userDetails, certType) && sslCertificateMetadataDetails!= null) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                    put(LogMessage.MESSAGE, String.format ("Trying to download certificate [%s] on [%s] by  [%s]", certName,LocalDateTime.now(),userDetails.getUsername())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return downloadCertificateWithPrivateKey(certificateDownloadRequest, sslCertificateMetadataDetails);
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                put(LogMessage.MESSAGE, String.format ("Access denied: [%s] has no permission to download certificate [%s] or certificate is not onboarded in T-Vault", userDetails.getUsername(), certName)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }

    /**
     * Download certificate.
     * @param certificateDownloadRequest
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadCertificateWithPrivateKey(CertificateDownloadRequest certificateDownloadRequest, SSLCertificateMetadataDetails sslCertificateMetadataDetails) {
        InputStreamResource resource = null;
        int certId = sslCertificateMetadataDetails.getCertificateId();
        String certName = certificateDownloadRequest.getCertificateName();

        String nclmToken = nclmMockEnabled.equalsIgnoreCase(TVaultConstants.TRUE) ? TVaultConstants.NCLM_MOCK_VALUE :  getNclmToken();
           
        if (StringUtils.isEmpty(nclmToken)) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Invalid nclm token", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }

        String fileType;
        switch (certificateDownloadRequest.getFormat()) {
            case SSLCertificateConstants.CERT_DOWNLOAD_TYPE_PKCS12DERR: fileType=".p12"; break;
            case SSLCertificateConstants.CERT_DOWNLOAD_TYPE_PEMBUNDLE: fileType=".pem"; break;
            case SSLCertificateConstants.CERT_DOWNLOAD_TYPE_PKCS12PEM:
            default: fileType=".pfx"; break;
        }
        String downloadFileName = certificateDownloadRequest.getCertificateName()+fileType;
        HttpClient httpClient;
        String api = certManagerDomain + "certificates/"+certId+"/privatekeyexport";
        try {
            httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
                    NoopHostnameVerifier.INSTANCE).
                    setSSLContext(
                            new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
                                @Override
                                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                                    return true;
                                }
                            }).build()
                    ).setRedirectStrategy(new LaxRedirectStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getApiResponse").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to create hhtpClient", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }

        if(!nclmMockEnabled.equalsIgnoreCase(TVaultConstants.TRUE) ) {
        	
        
        HttpPost postRequest = new HttpPost(api);
        postRequest.addHeader("Authorization", "Bearer "+ nclmToken);
        postRequest.addHeader("Content-type", "application/json");
        postRequest.addHeader("Accept","application/octet-stream");
        StringEntity stringEntity;
        try {
            stringEntity = new StringEntity("{\"format\":\""+certificateDownloadRequest.getFormat()+"\",\"password\":\""+certificateDownloadRequest.getCertificateCred()+"\", \"issuerChain\": "+certificateDownloadRequest.isIssuerChain()+"}");
        } catch (UnsupportedEncodingException e) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificate").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to encode request", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }
        postRequest.setEntity(stringEntity);

        try {
            HttpResponse apiResponse = httpClient.execute(postRequest);

            if (apiResponse.getStatusLine().getStatusCode() != 200) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
            }
            HttpEntity entity = apiResponse.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity, "UTF-8");
                // nclm api will give certificate in base64 encoded format
                byte[] decodedBytes = Base64.getDecoder().decode(responseString);
                resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
                return ResponseEntity.status(HttpStatus.OK).contentLength(decodedBytes.length)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+downloadFileName+"\"")
                        .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8"))
                        .body(resource);
            }
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificate").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to get api response from NCLM", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        } catch (IOException e) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificate").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to get api response from NCLM", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
        }else {
        	try {
        	Path path = Paths.get(downloadLocation+"/"+SSLCertificateConstants.DOWNLOAD_CERT+fileType);
        	if(path!=null) {
            byte[] decodedBytes;			
			decodedBytes = Files.readAllBytes(path);        	
            resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
            return ResponseEntity.status(HttpStatus.OK).contentLength(decodedBytes.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+downloadFileName+"\"")
                    .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8"))
                    .body(resource);
        	}
        	} catch (IOException e) {
        		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s].", certName)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
			}
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        
    }

    /**
     * Download certificate.
     * @param token
     * @param userDetails
     * @param certificateName
     * @param certificateType
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadCertificate(String token, UserDetails userDetails,
                                                                   String certificateName, String certificateType,
                                                                   String sslCertType) {

        InputStreamResource resource = null;
        if(!ControllerUtil.areDownloadInputsValid(certificateName,sslCertType)) {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	   					put(LogMessage.ACTION, "downloadCertificate").
	   					put(LogMessage.MESSAGE, "Invalid input values").
	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
        
        SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token, certificateName, sslCertType);
        if (hasDownloadPermission(certificateName, userDetails, sslCertType) && sslCertificateMetadataDetails != null) {
        	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificate").
                    put(LogMessage.MESSAGE, String.format ("Trying to download certificate [%s] on [%s] by  [%s]", certificateName,LocalDateTime.now(),userDetails.getUsername())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));

        	String nclmToken = nclmMockEnabled.equalsIgnoreCase(TVaultConstants.TRUE) ? TVaultConstants.NCLM_MOCK_VALUE :  getNclmToken();
            if (StringUtils.isEmpty(nclmToken)) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Invalid nclm token", certificateName)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
            }

            String contentType;
            switch (certificateType) {
                case "der": contentType = "application/pkix-cert"; break;
                case "pem":
                default: contentType = "application/x-pem-file"; break;
            }

            if(!nclmMockEnabled.equalsIgnoreCase(TVaultConstants.TRUE) ) {
            HttpClient httpClient;

            String api = certManagerDomain + "certificates/"+sslCertificateMetadataDetails.getCertificateId()+"/"+certificateType;

            try {
                httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
                        NoopHostnameVerifier.INSTANCE).
                        setSSLContext(
                                new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
                                    @Override
                                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                                        return true;
                                    }
                                }).build()
                        ).setRedirectStrategy(new LaxRedirectStrategy()).build();


            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to create hhtpClient", certificateName)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
            }

            HttpGet getRequest = new HttpGet(api);
            getRequest.addHeader("accept", "application/json");
            getRequest.addHeader("Authorization", "Bearer "+ nclmToken);

            try {
                HttpResponse apiResponse = httpClient.execute(getRequest);
                if (apiResponse.getStatusLine().getStatusCode() != 200) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
                }

                HttpEntity entity = apiResponse.getEntity();
                if (entity != null) {
                    String responseString = EntityUtils.toString(entity, "UTF-8");
                    // nclm api will give certificate in base64 encoded format
                    byte[] decodedBytes = Base64.getDecoder().decode(responseString);
                    resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
                    return ResponseEntity.status(HttpStatus.OK).contentLength(decodedBytes.length)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+certificateName+"\"")
                            .contentType(MediaType.parseMediaType(contentType+";charset=utf-8"))
                            .body(resource);
                }
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, "Failed to download certificate").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

            } catch (IOException e) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, "Failed to download certificate").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
            
            }else {
            	try {
            	Path path = Paths.get(downloadLocation+"/"+SSLCertificateConstants.DOWNLOAD_CERT+"."+certificateType);
            	if(path!=null) {
                byte[] decodedBytes;			
    			decodedBytes = Files.readAllBytes(path);        	
    			resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
                return ResponseEntity.status(HttpStatus.OK).contentLength(decodedBytes.length)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+certificateName+"\"")
                        .contentType(MediaType.parseMediaType(contentType+";charset=utf-8"))
                        .body(resource);
            	}
            	} catch (IOException e) {
            		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "downloadCertificate").
                            put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s].", certificateName)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
    			}
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }

        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                put(LogMessage.MESSAGE, String.format ("Access denied: [%s] has no permission to download certificate [%s] or certificate is not onboarded in T-Vault", userDetails.getUsername(), certificateName)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resource);
    }


	/**
	 * Get certificate details.
	 * 
	 * @param token
	 * @param certificateName
	 * @return
	 */
	public ResponseEntity<String> getCertificateDetails(String token, String certificateName, String certificateType) {

		certificateName = certificateUtils.getVaultCompactibleCertifiacteName(certificateName);

		SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token,
				certificateName, certificateType);
		if (sslCertificateMetadataDetails != null) {
			sslCertificateMetadataDetails.setCertificateName(certificateUtils.getActualCertifiacteName(sslCertificateMetadataDetails.getCertificateName()));
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(sslCertificateMetadataDetails));
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body("{\"errors\":[\"Access denied: Unable to read certificate details.\"]}");
	}


    /**
     * This method will be used to validate the no.of days for external certificates
     * @param metadataParams
     * @return
     * @throws ParseException
     */
    private long validateNoOfDays(Map<String, String> metadataParams) throws ParseException {
        String createDate = metadataParams.get("createDate");
        Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(createDate.substring(0, 10));
        return ((new Date().getTime() - date1.getTime()) / (1000 * 60 * 60 * 24)) % 365;
    }

    /**
	 * Renew SSL Certificate and update metadata
	 * 
	 * @param certificateId
	 * @param token
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public ResponseEntity<String> renewCertificate(String certType, String certificateName, UserDetails userDetails, String token) throws ParseException {

		Map<String, String> metaDataParams = new HashMap<>();
		Boolean isPermission = true;
		if (!isValidInputs(certificateName, certType)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "renewCertificate")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
		String endPoint = certificateName;
		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + endPoint;
		Response response = new Response();
		if (!userDetails.isAdmin()) {
			isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certificateName,certType);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ERRORS
								+ "Access denied: No permission to renew certificate"
								+ "\"]}");
			}
		}
		try {
			if (userDetails.isAdmin()) {
				response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", token);
			} else {
				response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}",
						userDetails.getSelfSupportToken());
			}
		} catch (Exception e) {
			log.error(
					JSONUtil.getJSON(
							ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
									.put(LogMessage.ACTION,
											String.format("Exception = [%s] =  Message [%s]",
													Arrays.toString(e.getStackTrace()), response.getResponse()))
									.build()));
			return ResponseEntity.status(response.getHttpstatus())
					.body(MESSAGES + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body(ERRORS + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);
        if(Objects.nonNull(metaDataParams.get("requestStatus")) && (!metaDataParams.get("requestStatus").equalsIgnoreCase(SSLCertificateConstants.REQUEST_PENDING_APPROVAL))
                && metaDataParams.get(SSLCertificateConstants.CERT_TYPE).equalsIgnoreCase("external")){
            long noOfDays = validateNoOfDays(metaDataParams);
            if(noOfDays<=30){
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                        .put(LogMessage.ACTION, "validateNoOfDays")
                        .put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
                        .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"External certificate can be renewed only after a month of  certificate  creation\"]}");
            }
        }

		String certID = object.get(SSLCertificateConstants.CERTIFICATE_ID).getAsString();
		int containerId = object.get(SSLCertificateConstants.CONTAINER_ID).getAsInt();
        float value = Float.valueOf(certID);
		int certificateId = (int) value;
		
		CertResponse renewResponse = new CertResponse();
		try {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Renew certificate")
					.put(LogMessage.MESSAGE,
							String.format("Trying to renew certificate for [%s] renewed by [%s] on [%s]", certificateName,userDetails.getUsername(),LocalDateTime.now()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			String nclmAccessToken = isMockingEnabled(certType) ? TVaultConstants.NCLM_MOCK_VALUE :  getNclmToken();
			if(!StringUtils.isEmpty(nclmAccessToken)) {
				CertificateData cerificatetData = (!isMockingEnabled(certType)) ?
                        getLatestCertificate(certificateName,nclmAccessToken, containerId):
                        nclmMockUtil.getRenewCertificateMockData();
				if((!ObjectUtils.isEmpty(cerificatetData)&& cerificatetData.getCertificateId()!=0)) {
					if(cerificatetData.getCertificateStatus().equalsIgnoreCase("Revoked")) {
						return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
		    					.body(ERRORS + "Certificate in Revoked status cannot be renewed." + "\"]}");
					}
				}else {
					log.error(
							JSONUtil.getJSON(
									ImmutableMap.<String, String> builder()
											.put(LogMessage.USER,
													ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
											.put(LogMessage.ACTION, "Renew certificate Failed")
											.put(LogMessage.MESSAGE, "Renew Request failed for CertificateID")										
											.build()));
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ERRORS + "Certificate " +
                            "unavailable in system." + "\"]}");
				}

				//Adding mocking flag
		    if(!isMockingEnabled(certType) ) {
                String nclmApiRenewEndpoint = renewCertificateEndpoint.replace("certID", String.valueOf(certificateId));
                renewResponse = reqProcessor.processCert("/certificates/renew", "",
                        nclmAccessToken, getCertmanagerEndPoint(nclmApiRenewEndpoint));
            } else {
                renewResponse = nclmMockUtil.getRenewMockResponse();
            }
			Thread.sleep(renewDelayTime);
			log.debug(
					JSONUtil.getJSON(
							ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
									.put(LogMessage.ACTION, "Renew certificate")
									.put(LogMessage.MESSAGE, "Renew certificate for CertificateID")
									.put(LogMessage.STATUS, renewResponse.getHttpstatus().toString())
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
									.build()));
			
			//if renewed get new certificate details and update metadata
			if (renewResponse!=null && (HttpStatus.OK.equals(renewResponse.getHttpstatus()) || HttpStatus.ACCEPTED.equals(renewResponse.getHttpstatus())) ) {
            CertificateData certData = (!isMockingEnabled(certType)) ?
                    getLatestCertificate(certificateName,nclmAccessToken, containerId):nclmMockUtil.getRenewCertificateMockData();
			boolean sslMetaDataUpdationStatus=true;
			boolean isApprovalReq=false;
			if(certData != null && !ObjectUtils.isEmpty(certData)) {
            int actionId=0;
			if(certType.equalsIgnoreCase("external")) {
                CertManagerLogin certManagerLogin = new CertManagerLogin();
                certManagerLogin.setAccess_token(nclmAccessToken);
                Map<String, Object> responseMap = ControllerUtil.parseJson(renewResponse.getResponse());

                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                        .put(LogMessage.ACTION, "renewCertificate")
                        .put(LogMessage.MESSAGE, String.format("Renew  certificate name = " +
                                        "[%s]=certificateType = [%s] = oldCertId = [%s] = newCertId = [%s]",  certificateName,
                                certType, certificateId, certData.getCertificateId()))
                        .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

                if(String.valueOf(certificateId).equalsIgnoreCase(String.valueOf(certData.getCertificateId()))) {
                    //Make sure certificate renewed
                    certData = getRenewedCertificate(certType, certificateName, nclmAccessToken, containerId, certificateId);
                }
                if (responseMap.size() > 0 && Objects.nonNull(responseMap.get(SSLCertificateConstants.ACTION_ID)) && !MapUtils.isEmpty(responseMap) && responseMap.get(
                        SSLCertificateConstants.ACTION_ID) != null) {
                    actionId = (Integer) responseMap.get(SSLCertificateConstants.ACTION_ID);
                    if (actionId != 0) {
                        renewResponse = approvalRequest(certManagerLogin, actionId);
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, String.format("approvalRequest Completed Successfully [%s]" +
                                        " = certificate name = [%s]", renewResponse.getResponse(), certificateName)).
                                build()));
                        metaDataParams.put("requestStatus", SSLCertificateConstants.RENEW_PENDING);
                        metaDataParams.put(SSLCertificateConstants.ACTION_ID, String.valueOf(actionId));
                        isApprovalReq = true;
                    } else {
                        metaDataParams.put(SSLCertificateConstants.CERTIFICATE_ID, certData != null && ((Integer) certData.getCertificateId()).toString() != null ?
                                ((Integer) certData.getCertificateId()).toString() : String.valueOf(certificateId));
                        metaDataParams.put("createDate", certData != null && certData.getCreateDate() != null ? certData.getCreateDate() : object.get("createDate").getAsString());
                        metaDataParams.put("expiryDate", certData != null && certData.getExpiryDate() != null ? certData.getExpiryDate() : object.get("expiryDate").getAsString());
                        metaDataParams.put("certificateStatus", certData != null && certData.getCertificateStatus() != null ? certData.getCertificateStatus() :
                                object.get("certificateStatus").getAsString());
                    }
                } else {
                    metaDataParams.put(SSLCertificateConstants.CERTIFICATE_ID, certData != null && ((Integer) certData.getCertificateId()).toString() != null ?
                            ((Integer) certData.getCertificateId()).toString() : String.valueOf(certificateId));
                    metaDataParams.put("createDate", certData != null && certData.getCreateDate() != null ? certData.getCreateDate() : object.get("createDate").getAsString());
                    metaDataParams.put("expiryDate", certData != null && certData.getExpiryDate() != null ? certData.getExpiryDate() : object.get("expiryDate").getAsString());
                    metaDataParams.put("certificateStatus", certData != null && certData.getCertificateStatus() != null ? certData.getCertificateStatus() :
                            object.get("certificateStatus").getAsString());
                }

            }else {
				metaDataParams.put(SSLCertificateConstants.CERTIFICATE_ID,((Integer)certData.getCertificateId()).toString()!=null?
						((Integer)certData.getCertificateId()).toString():String.valueOf(certificateId));
				metaDataParams.put("createDate", certData.getCreateDate()!=null?certData.getCreateDate():object.get("createDate").getAsString());
				metaDataParams.put("expiryDate", certData.getExpiryDate()!=null?certData.getExpiryDate():object.get("expiryDate").getAsString());			
				metaDataParams.put("certificateStatus", certData.getCertificateStatus()!=null?certData.getCertificateStatus():
					object.get("certificateStatus").getAsString());
			}
			if (userDetails.isAdmin()) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams, token);
			} else {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams,
						userDetails.getSelfSupportToken());
			}
			}
			if (sslMetaDataUpdationStatus) {
                String certOwnerEmailId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_EMAILID);
                String certOwnerNtId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_NTID);
                //Sending renew email
                sendRenewEmail(certType, certificateName, certOwnerEmailId,certOwnerNtId, token,isApprovalReq);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
    					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
    					.put(LogMessage.ACTION, "Renew certificate")
    					.put(LogMessage.MESSAGE,
    							String.format("Certificate renewed successfully [%s] renewed by [%s] on [%s]", certificateName,userDetails.getUsername(),LocalDateTime.now()))
    					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
    					.build()));
				return ResponseEntity.status(renewResponse.getHttpstatus())
						.body(MESSAGES + "Certificate renewed successfully" + "\"]}");
			} else {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION, "Renew certificate Failed")
										.put(LogMessage.MESSAGE, "Metadata updation failed for CertificateID")
										.put(LogMessage.STATUS, renewResponse.getHttpstatus().toString())
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
										.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(ERRORS + "Metadata updation Failed." + "\"]}");
			}
			}else {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION, "Renew certificate Failed")
										.put(LogMessage.MESSAGE, "Renew Request failed for CertificateID")
										.put(LogMessage.STATUS, renewResponse.getHttpstatus().toString())
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
										.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(ERRORS + "Certificate Renewal Failed" + "\"]}");
			}
			
			}else {	
            	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).	
                        put(LogMessage.ACTION, "renew certificate").	
                        put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).                        	
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).	
                        build()));	
            	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
    					.body(ERRORS + nclmErrorMessage + "\"]}");
            }

		} catch (TVaultValidationException error) {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, String.format("Inside  TVaultValidationException  = [%s] =  Message [%s]",
									Arrays.toString(error.getStackTrace()), error.getMessage()))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORS + error.getMessage() + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ERRORS + e.getMessage() + "\"]}");
		}
	}

    /**
     * This method to get the renewed certificate
     * @param certType
     * @param certificateName
     * @param nclmAccessToken
     * @param containerId
     * @param certID
     * @return
     * @throws Exception
     */
    private CertificateData getRenewedCertificate(String certType, String certificateName, String nclmAccessToken,
                                                  int containerId, int certID) throws Exception {
        CertificateData certData = null;
        for (int i = 0; i < retrycount; i++) {
            Thread.sleep(renewDelayTime);
            certData = (!isMockingEnabled(certType)) ?
                    getLatestCertificate(certificateName, nclmAccessToken, containerId) :
                    nclmMockUtil.getRenewCertificateMockData();

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "getRenewedCertificate")
                    .put(LogMessage.MESSAGE, String.format("Renew RETRY COUNT = [%s] = certificate name = " +
                                    "[%s]=certificateType = [%s] = oldCertId = [%s] = newCertId = [%s]", i, certificateName,
                            certType, certID, certData.getCertificateId()))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

            if (!String.valueOf(certID).equalsIgnoreCase(String.valueOf(certData.getCertificateId()))) {
                break;
            }
        }
        return certData;
    }

    /**
     * This method will be responsible for sending an email for renew certificate for both internal and external
     * @param certType
     * @param certificateName
     * @param userDetails
     * @param token
     */
    private void sendRenewEmail(String certType, String certificateName, String certOwnerEmailId, String certOwnerNtId,
                                String token, boolean isApprovalReq) {

        //Send an approval process in case renew required approval process
        if (isApprovalReq) {
            sendExternalEmail(certType, certificateName, certOwnerEmailId, certOwnerNtId,
                    SSLCertificateConstants.EX_CERT_RENEW_SUBJECT + " - " +
                            certificateName, "renew");
        } else {
            sendEmail(certType, certificateName, certOwnerEmailId, certOwnerNtId,
                    SSLCertificateConstants.CERT_RENEW_SUBJECT + " - " + certificateName,
                    "renewed", token);
        }
    }


	/**
     * To Get the latest certificate details in nclm for a given renewed certificate name
     * @param sslCertificateRequest
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private CertificateData getLatestCertificate(String certName, String accessToken, int containerId) throws Exception {
        CertificateData certificateData=new CertificateData(); 
        String findCertificateEndpoint = "/certmanager/findCertificate";

        String targetEndpoint = findCertificate.replace("certname", " \"" +String.valueOf(certName)+"\" ").replace(
                "cid", String.valueOf(containerId));
        CertResponse response = reqProcessor.processCert(findCertificateEndpoint, "", accessToken, getCertmanagerEndPoint(targetEndpoint));        
        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.CERTIFICATES) != null)) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
                JsonArray jsonArrayvalid = new JsonArray();
                LocalDateTime  createdDate = null ;
                LocalDateTime  certCreatedDate;
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElements = jsonArray.get(i).getAsJsonObject();
                    if ((Objects.equals(getCertficateName(jsonElements.get("sortedSubjectName").getAsString()), certName))) {
                    	 jsonArrayvalid.add(jsonElements);
				    }
				}
                String notBeforeDate = null;
				for (int j = 0; j < jsonArrayvalid.size(); j++) {
					JsonObject jsonElement = jsonArrayvalid.get(j).getAsJsonObject();

					if (j == 0) {
						notBeforeDate = validateString(jsonElement.get("NotBefore"));
						createdDate = notBeforeDate != null ? LocalDateTime.parse(notBeforeDate.substring(0, 19))
								: null;
					} else if (j > 0) {
						notBeforeDate = validateString(jsonArray.get(j - 1).getAsJsonObject().get("NotBefore"));
						createdDate = notBeforeDate != null ? LocalDateTime.parse(notBeforeDate.substring(0, 19))
								: null;
					}

					notBeforeDate = validateString(jsonElement.get("NotBefore"));
					certCreatedDate = notBeforeDate != null ? LocalDateTime.parse(notBeforeDate.substring(0, 19))
							: null;
					if (certCreatedDate != null && createdDate != null && !ObjectUtils.isEmpty(createdDate)
							&& (createdDate.isBefore(certCreatedDate) || createdDate.isEqual(certCreatedDate))) {
						certificateData = new CertificateData();
						certificateData
								.setCertificateId(Integer.parseInt(jsonElement.get("certificateId").getAsString()));
						certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
						certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));
						certificateData.setCertificateStatus(
								validateString(jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS)));
						certificateData.setCertificateName(certName);
						certificateData.setDeployStatus(
								getTargetSystemServiceIds(jsonElement.getAsJsonArray("targetSystemServiceIds")));
					}
                }

            }
        }
        return certificateData;
    }    

	/**
	 * Removes user from certificate
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromCertificate(CertificateUser certificateUser, UserDetails userDetails) {
		
		if(!areCertificateUserInputsValid(certificateUser)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG).
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
   		}
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG)
				.put(LogMessage.MESSAGE, "Trying to delete user from the certificate")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));

		String userName = certificateUser.getUsername().toLowerCase();
   		String certificateName = certificateUser.getCertificateName().toLowerCase(); 
   		String certificateType = certificateUser.getCertType();
   		String authToken = null;   		
   		boolean isAuthorized = true;
   		
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();   	            
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certificateType);
   			
   			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData); 
   		}else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to remove user from this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}");
   		}
		
		if(isAuthorized){
			return checkUserPolicyAndRemoveFromCertificate(userName, certificateName, authToken, certificateType, userDetails);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Access denied: No permission to remove user from this certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}");
		}
	}

	/**
	 *
	 * @param userName
	 * @param certificateName
	 * @param authToken
	 * @param certificateType
	 * @param userDetails
	 * @return
	 */
	private ResponseEntity<String> checkUserPolicyAndRemoveFromCertificate(String userName, String certificateName,
			String authToken, String certificateType, UserDetails userDetails) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		String certPrefix=(certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		
		String metaDataPath = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		
		String certificatePath = metaDataPath + certificateName;
		
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
				
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}", authToken);	
		}
		else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
			userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}", authToken);
		}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC implementation changes
			ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(authToken, userName, userDetails, true);
			if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, "checkUserPolicyAndRemoveFromCertificate")
							.put(LogMessage.MESSAGE, "Trying to fetch OIDC user policies, failed")
							.put(LogMessage.APIURL,
									ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
							.build()));
				}
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
			}
			oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
			oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
			userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
			userResponse.setHttpstatus(responseEntity.getStatusCode());

		}

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				ObjectMapper objMapper = new ObjectMapper();
				if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					currentpolicies.addAll(oidcEntityResponse.getPolicies());
				} else {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			
			policies.addAll(currentpolicies);				
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		Response ldapConfigresponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, authToken);
		} else if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, authToken);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC Implementation : Entity Update
			try {
				ldapConfigresponse = oidcUtil.updateOIDCEntity(policies,
						oidcEntityResponse.getEntityName());
                oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "Remove User from Certificates")
						.put(LogMessage.MESSAGE, String.format("Exception while updating the identity"))
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
			}
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
			return updateMetadataForRemoveUserFromCertificate(userName, certificatePath, authToken, groups,
					currentpoliciesString, userDetails, currentpolicies, oidcEntityResponse.getEntityName());
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Failed to remvoe the user from the certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remvoe the user from the certificate\"]}");
		}
	}

	/**
	 *
	 * @param userName
	 * @param certificatePath
	 * @param authToken
	 * @param groups
	 * @param currentpoliciesString
	 * @param userDetails
	 * @param currentpolicies
	 * @param entityName
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForRemoveUserFromCertificate(String userName, String certificatePath,
			String authToken, String groups, String currentpoliciesString, UserDetails userDetails,
			List<String> currentpolicies, String entityName) {
		Response ldapConfigresponse = new Response();
		// User has been associated with certificate. Now metadata has to be deleted
		Map<String,String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name",userName);
		params.put("path",certificatePath);
		params.put("access","delete");
		
		Response metadataResponse = ControllerUtil.updateMetadata(params, authToken);
		if(metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE,String.format("User [%s] is successfully Removed from Certificate [%s]",userName,certificatePath)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully removed user from the certificate\"]}");
		} else {
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, currentpoliciesString, authToken);
			}
			else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString, groups, authToken);
			}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
				// OIDC changes
				try {
					ldapConfigresponse = oidcUtil.updateOIDCEntity(currentpolicies,
							entityName);
                    oidcUtil.renewUserToken(userDetails.getClientToken());
				} catch (Exception e2) {
					log.error(e2);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER,
									ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "Remove User from Certificates")
							.put(LogMessage.MESSAGE,
									String.format("Exception while updating the identity"))
							.put(LogMessage.STACKTRACE, Arrays.toString(e2.getStackTrace()))
							.put(LogMessage.APIURL,
									ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
				}
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, "Failed to remove the user from the certificate. Metadata update failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the user from the certificate. Metadata update failed\"]}");
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, "Failed to revert user association on certificate").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to revert user association on certificate\"]}");
			}
		}
	}

	/**
     * Remove Group from certificate
     * @param certificateGroup
     * @param userDetails
     * @return
     */
    public ResponseEntity<String> removeGroupFromCertificate(CertificateGroup certificateGroup, UserDetails userDetails) {

    	if(!areCertificateGroupInputsValid(certificateGroup)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG).
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
   		}

        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
                put(LogMessage.MESSAGE, String.format("Trying to remove Group from certificate - [%s]", certificateGroup.toString())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String groupName = certificateGroup.getGroupname();
   		String certificateName = certificateGroup.getCertificateName().toLowerCase();
   		String certificateType = certificateGroup.getCertType();
   		String authToken = null;

   		boolean isAuthorized = true;

   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();   	            
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certificateType);

   			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);

   		}else {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG)
							.put(LogMessage.MESSAGE, String.format(
									"Access denied: No permission to remove group [%s] from this certificate [%s]",
									groupName, certificateName))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove group from this certificate\"]}");
   		} 

        if(isAuthorized){        	
			return checkPolicyDetailsAndRemoveGroupFromCertificate(groupName, certificateName, authToken,
					certificateType, userDetails);
        } else {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
                    put(LogMessage.MESSAGE, "Access denied: No permission to remove groups from this certificate").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove groups from this certificate\"]}");
        }

    }

	/**
	 * Method to check the group policy and remove the group from certificate
	 * @param groupName
	 * @param certificateName
	 * @param authToken
	 * @return
	 */
	private ResponseEntity<String> checkPolicyDetailsAndRemoveGroupFromCertificate(String groupName,
			String certificateName, String authToken, String certificateType, UserDetails userDetails) {
		OIDCGroup oidcGroup = new OIDCGroup();

		// check for this group is associated to this certificate
		String sslCertMetaDataPath = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                new StringBuilder(SSLCertificateConstants.SSL_CERT_PATH).append("/").append(certificateName).toString() : new StringBuilder(SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH).append("/").append(certificateName).toString();
		Response metadataReadResponse = reqProcessor.process("/read", "{\"path\":\"" + sslCertMetaDataPath + "\"}", authToken);
		
		Map<String, Object> responseMap = null;
		boolean metaDataResponseStatus = true;
		if(metadataReadResponse != null && HttpStatus.OK.equals(metadataReadResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(metadataReadResponse.getResponse());
			if(responseMap.isEmpty()) {
				metaDataResponseStatus = false;
			}
		}
		else {
			metaDataResponseStatus = false;
		}

		if(metaDataResponseStatus) {
			@SuppressWarnings("unchecked")
			Map<String,Object> metadataMap = (Map<String,Object>)responseMap.get("data");
			Map<String,Object> groupsData = (Map<String,Object>)metadataMap.get(TVaultConstants.GROUPS);

			if (groupsData == null || !groupsData.containsKey(groupName)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, String.format ("Group [%s] is not associated to certificate [%s]", groupName, certificateName)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove group from certificate. Group association to certificate not found\"]}");
			}
		}else {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, String.format ("Error Fetching existing certificate info [%s]", certificateName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Error Fetching existing certificate info. please check the path specified\"]}");
		}

		return getGroupDetailsAndCallRemovalProcessForCert(groupName, certificateName, authToken, certificateType,
				userDetails, oidcGroup);
	}

	/**
	 * Method to call the group removal based on the auth method
	 * @param groupName
	 * @param certificateName
	 * @param authToken
	 * @param certificateType
	 * @param userDetails
	 * @param oidcGroup
	 * @return
	 */
	private ResponseEntity<String> getGroupDetailsAndCallRemovalProcessForCert(String groupName, String certificateName,
			String authToken, String certificateType, UserDetails userDetails, OIDCGroup oidcGroup) {
		Response groupResp = new Response();
		if(TVaultConstants.LDAP.equals(vaultAuthMethod)){
			groupResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}", authToken);
		}else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			//call read api with groupname
			oidcGroup= oidcUtil.getIdentityGroupDetails(groupName, authToken);
			if (oidcGroup != null) {
				groupResp.setHttpstatus(HttpStatus.OK);
				groupResp.setResponse(oidcGroup.getPolicies().toString());
			} else {
				groupResp.setHttpstatus(HttpStatus.BAD_REQUEST);
			}
		}
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		        put(LogMessage.MESSAGE, String.format ("Group Response status is [%s]", groupResp.getHttpstatus())).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		return removePoliciesAndCallMetadataUpdate(groupName, certificateName, authToken, certificateType, userDetails,
				oidcGroup, groupResp);
	}

	/**
	 * Method to update policies for remove group from certificate.
	 * @param groupName
	 * @param certificateName
	 * @param authToken
	 * @param certificateType
	 * @param userDetails
	 * @param oidcGroup
	 * @param groupResp
	 * @return
	 */
	private ResponseEntity<String> removePoliciesAndCallMetadataUpdate(String groupName, String certificateName,
			String authToken, String certificateType, UserDetails userDetails, OIDCGroup oidcGroup,
			Response groupResp) {
		String metaDataPath = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

		String certificatePath = metaDataPath + certificateName;
		String certPrefix=(certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(groupResp != null && HttpStatus.OK.equals(groupResp.getHttpstatus())){
		    responseJson = groupResp.getResponse();
		    try {
				ObjectMapper objMapper = new ObjectMapper();
				// OIDC Changes
				if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				} else if (TVaultConstants.OIDC.equals(vaultAuthMethod) && oidcGroup != null) {
					currentpolicies.addAll(oidcGroup.getPolicies());
				}
		    } catch (IOException e) {
		        log.error(e);
		        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		                put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups").
		                put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
		                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                build()));
		    }

		    policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		}else {
			return deleteOrphanGroupEntriesForCertificate(authToken, certificatePath, groupName);
		}

		return configureGroupPoliciesAndCallMetadataUpdate(groupName, authToken, userDetails, oidcGroup,
				certificatePath, policies, currentpolicies);
	}

	/**
	 * Method to configure group policies based on the auth method and call the metadata update
	 * @param groupName
	 * @param authToken
	 * @param userDetails
	 * @param oidcGroup
	 * @param certificatePath
	 * @param policies
	 * @param currentpolicies
	 * @return
	 */
	private ResponseEntity<String> configureGroupPoliciesAndCallMetadataUpdate(String groupName, String authToken,
			UserDetails userDetails, OIDCGroup oidcGroup, String certificatePath, List<String> policies,
			List<String> currentpolicies) {
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		Response ldapConfigresponse = new Response();
		// OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, authToken);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ldapConfigresponse = oidcUtil.updateGroupPolicies(authToken, groupName, policies, currentpolicies,
					oidcGroup!=null?oidcGroup.getId(): null);
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
			return updateMetadataForRemoveGroupFromCertificate(groupName, certificatePath, authToken,
					currentpoliciesString, userDetails, currentpolicies, oidcGroup!=null?oidcGroup.getId(): null);
		} else {
			String ssoToken = oidcUtil.getSSOToken();
			if (!StringUtils.isEmpty(ssoToken)) {
				String objectId = oidcUtil.getGroupObjectResponse(ssoToken, groupName);
				if (objectId == null || StringUtils.isEmpty(objectId)) {
					return deleteOrphanGroupEntriesForCertificate(authToken, certificatePath, groupName);
				}
			}
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		            put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		            put(LogMessage.MESSAGE, "Failed to remove the group from the certificate").
		            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		            build()));
		    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Try Again\"]}");
		}
	}

	/**
	 * Method to delete orphan group entries if exists for certificate
	 * @param token
	 * @param certificatePath
	 * @param groupName
	 * @return
	 */
	private ResponseEntity<String> deleteOrphanGroupEntriesForCertificate(String token, String certificatePath, String groupName) {
		// Trying to remove the orphan entries if exists
		Map<String,String> params = new HashMap<>();
		params.put("type", "groups");
		params.put("name",groupName);
		params.put("path",certificatePath);
		params.put("access","delete");
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, "Remove Group from certificate").
					put(LogMessage.MESSAGE, String.format ("Group [%s] is successfully removed from certificate [%s]", groupName, certificatePath)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"Message\":\"Group not available or deleted from AD, removed the group assignment and permissions \"}");
		}else{
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Try again \"]}");
		}
	}

	/**
	 * Method to update the metadata after removed the group policy for a certificate
	 * @param groupName
	 * @param certificateName
	 * @param authToken
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForRemoveGroupFromCertificate(String groupName, String certificatePath,
			String authToken, String currentpoliciesString, UserDetails userDetails, List<String> currentPolicies,
			String groupId) {
		Map<String,String> params = new HashMap<>();
		params.put("type", "groups");
		params.put("name", groupName);
		params.put("path",certificatePath);
		params.put("access","delete");
		
		Response metadataResponse = ControllerUtil.updateMetadata(params, authToken);
		
		if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, String.format ("Group - [%s] is successfully removed from the certificate - [%s]", groupName, certificatePath)).
					put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from certificate\"]}");
		}else {				
			return revertGroupPolicyIfMetadataUpdateFailed(groupName, authToken, currentpoliciesString,
					metadataResponse, userDetails, currentPolicies, groupId);
		}
	}

	/**
	 * Method to revert group policy if metadata update failed for certificate
	 * @param groupName
	 * @param authToken
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> revertGroupPolicyIfMetadataUpdateFailed(String groupName, String authToken,
			String currentpoliciesString, Response metadataResponse, UserDetails userDetails,
			List<String> currentpolicies, String groupId) {
		Response ldapConfigresponse = new Response();
		// OIDC Changes
		if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString, authToken);
		} else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			ldapConfigresponse = oidcUtil.updateGroupPolicies(authToken, groupName, currentpolicies, currentpolicies,
					groupId);
			oidcUtil.renewUserToken(userDetails.getClientToken());
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting, group policy update success").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
		} else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting group policy update failed").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
		}
	}

    
	/**
	 * Validates Certificate group inputs
	 * @param certificateUser
	 * @return boolean
	 */
	private boolean areCertificateGroupInputsValid(CertificateGroup certificateGroup) {
		
		if (ObjectUtils.isEmpty(certificateGroup)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateGroup.getGroupname())
				|| ObjectUtils.isEmpty(certificateGroup.getAccess())
				|| ObjectUtils.isEmpty(certificateGroup.getCertificateName())
				|| certificateGroup.getCertificateName().contains(" ")
	            || (!certificateGroup.getCertificateName().endsWith(certificateNameTailText))
	            || (certificateGroup.getCertificateName().contains(".-"))
	            || (certificateGroup.getCertificateName().contains("-."))
	            || (!certificateGroup.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))
				) {
			return false;
		}
		boolean isValid = true;
		String access = certificateGroup.getAccess();
		if (!ArrayUtils.contains(PERMISSIONS, access)) {
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Get List Of internal or external certificates
	 * @param token
	 * @param certificateType
	 * @param limit
	 * @param offset
	 * @return
	 */
	public ResponseEntity<String> getListOfCertificates(String token, String certificateType, Integer limit, Integer offset) {
		Response response;
		String path = "";
		if(!certificateType.matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING)){
    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "Get list Of Certificates")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
    	}
		if (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
			path = SSLCertificateConstants.SSL_CERT_PATH_VALUE;
		} else {
			path = SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		}
		response = getMetadata(token, path);

		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "Get list Of Certificates")
					.put(LogMessage.MESSAGE, "Certificates fetched from metadata")
					.put(LogMessage.STATUS, response.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			Map<String, Object> certificateMap = getCertificateSublistFromResponse(limit, offset, response);
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(certificateMap));
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "Get list Of certificates")
					.put(LogMessage.MESSAGE, "Failed to get certificate list from metadata")
					.put(LogMessage.STATUS, response.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}

	/**
	 * Method to get the sublist of certificate from response.
	 * @param limit
	 * @param offset
	 * @param response
	 * @return
	 */
	private Map<String, Object> getCertificateSublistFromResponse(Integer limit, Integer offset, Response response) {
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
		JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
		List<String> certNames = getMatchedCertificatesBasedOnPermissions(jsonArray);
		Integer totalCertCount = certNames.size();

		limit = (limit == null)?totalCertCount:limit;
		offset = (offset == null)?0:offset;

		List<String> certificateSubList =  certNames.stream().skip(offset).limit(limit).collect(Collectors.toList());

		String[] certArray = certificateSubList.toArray(new String[certificateSubList.size()]);

		Map<String, Object> certificateMap = new HashMap<>();
		certificateMap.put("keys", certArray);
		certificateMap.put("total", totalCertCount);
		certificateMap.put("next", (totalCertCount - (certArray.length+ offset)>0?(certArray.length + offset):-1));
		return certificateMap;
	}

	/**
	 * Get List Of internal or external certificates for validation
	 *
	 * @param token
	 * @param certificateType
	 * @return
	 * @throws Exception
	 */
	private List<String> getListOfCertificatesForValidation(String token, String certificateType) {
		Response response;
		String path = "";
		List<String> certNames = new ArrayList<>();
		if (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
			path = SSLCertificateConstants.SSL_CERT_PATH_VALUE;
		} else {
			path = SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		}
		response = getMetadata(token, path);

		if(response != null && response.getResponse() != null) {
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
			JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
			certNames = getMatchedCertificatesBasedOnPermissions(jsonArray);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getListOfCertificates for validation")
					.put(LogMessage.MESSAGE, "Certificates fetched from metadata")
					.put(LogMessage.STATUS, response.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}else {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getListOfCertificates for validation")
					.put(LogMessage.MESSAGE, "No certificates available")
					.put(LogMessage.STATUS, "No certificates available")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return certNames;
	}

	/**
     * To update the owner of an existing certificate.
     * @param token
     * @param userDetails
     * @return
     * @throws Exception
     */
    public ResponseEntity<String> updateCertOwner( String certType,String certName,String certOwnerEmailId,  UserDetails userDetails) throws Exception {
		Map<String, String> metaDataParams ;
		Map<String, String> dataMetaDataParams = new HashMap<>();
		SSLCertificateRequest certificateRequest = new SSLCertificateRequest();
		String authToken = userDetails.getSelfSupportToken();
		boolean isValidEmail = true;
		String certOwnerNtId = "";
		Object[] users = null;
		DirectoryUser dirUser = new DirectoryUser();
    	if (!isValidInputs(certName, certType) || !validateCertficateEmail(certOwnerEmailId)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "transferCertificate")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
		ResponseEntity<DirectoryObjects> userResponse = directoryService.searchByUPNInGsmAndCorp(certOwnerEmailId);
    	if(userResponse.getStatusCode().equals(HttpStatus.OK)) {
    		 users = userResponse.getBody().getData().getValues();
    		 if(!ObjectUtils.isEmpty(users)) {
    		 dirUser = (DirectoryUser) users[0];
    		 certOwnerNtId = dirUser.getUserName();
    		 }
    	}   

		if (certOwnerNtId.equals("")) {
    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "updateCertOwner")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"User unavailable\"]}");
    	}
       
		String endPoint = certName;	
		CertResponse enrollResponse = new CertResponse();
		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH + '/' + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + '/' + endPoint;		
		String permissionMetaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.SSL_CERT_PATH_VALUE  + endPoint :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT  + endPoint;
		Response response = new Response();
		Response dataResponse = new Response();
		if (!userDetails.isAdmin()) {
			Boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certName, certType);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ERRORS
								+ "Access denied: No permission to transfer the ownership of this certificate"
								+ "\"]}");
			}
		}
		try {
			if (userDetails.isAdmin()) {
				response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", authToken);
				dataResponse = reqProcessor.process("/read", "{\"path\":\"" + permissionMetaDataPath + "\"}", authToken);
			} else {
				response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}",
						userDetails.getSelfSupportToken());
				dataResponse = reqProcessor.process("/read", "{\"path\":\"" + permissionMetaDataPath + "\"}",
						userDetails.getSelfSupportToken());
			}
		} catch (Exception e) {
			log.error(
					JSONUtil.getJSON(
							ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
									.put(LogMessage.ACTION,
											String.format("Exception = [%s] =  Message [%s]",
													Arrays.toString(e.getStackTrace()), response.getResponse()))
									.build()));
			return ResponseEntity.status(response.getHttpstatus())
					.body(MESSAGES + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body(ERRORS + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		ObjectMapper objMapper = new ObjectMapper();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		JsonObject dataObject = ((JsonObject) jsonParser.parse(dataResponse.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);	
		
		if(certOwnerEmailId.equalsIgnoreCase(metaDataParams.get(SSLCertificateConstants.CERT_OWNER_EMAILID)))	{
			isValidEmail=false;
		}
				
		if(dataObject!=null) {
		dataMetaDataParams = new Gson().fromJson(dataObject.toString(), Map.class);	
		
		dataMetaDataParams.put(SSLCertificateConstants.CERT_OWNER_NTID, certOwnerNtId);
		dataMetaDataParams.put(SSLCertificateConstants.CERT_OWNER_EMAILID, certOwnerEmailId);
		}	
		
		if((Objects.nonNull(metaDataParams)) && (Objects.nonNull(metaDataParams.get("requestStatus")))	
                && metaDataParams.get("requestStatus").equalsIgnoreCase(SSLCertificateConstants.REQUEST_PENDING_APPROVAL)) {	
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Certificate may not be approved or rejected from NCLM\"]}");
		}
		
		if(!isValidEmail) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ERRORS
							+ "New owner email id should not be same as owner email id"
							+ "\"]}");
		}		
		
		String certificateUser = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_NTID);
        String oldEmailId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_EMAILID);
		boolean sslMetaDataUpdationStatus;			
		metaDataParams.put(SSLCertificateConstants.CERT_OWNER_EMAILID, certOwnerEmailId);
		metaDataParams.put(SSLCertificateConstants.CERT_OWNER_NTID, certOwnerNtId);
        updateNotificationEmails(metaDataParams,oldEmailId,certOwnerEmailId);
		certificateRequest.setCertificateName(certName);
		certificateRequest.setCertType(certType);
		certificateRequest.setCertOwnerEmailId(certOwnerEmailId);
		certificateRequest.setCertOwnerNtid(certOwnerNtId);
		
		try {
		if (userDetails.isAdmin()) {

			sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams, authToken);
			if(dataObject!=null) {
			sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(permissionMetaDataPath, dataMetaDataParams, authToken);
			}
		} else {
			sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams,
					userDetails.getSelfSupportToken());	
			if(dataObject!=null) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(permissionMetaDataPath, dataMetaDataParams, userDetails.getSelfSupportToken());
				}
		}
		if (sslMetaDataUpdationStatus) {
			boolean isPoliciesCreated=true;	
			removeSudoPermissionForPreviousOwner( certificateUser.toLowerCase(), certName,userDetails,certType);
			addSudoPermissionToCertificateOwner(certificateRequest, userDetails, enrollResponse, isPoliciesCreated, true,authToken,"transfer");
            sendTransferEmail(metaDataParams,certificateRequest.getCertOwnerNtid(),certificateUser);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "sendTransferEmail").
                    put(LogMessage.MESSAGE, String.format("Successfully sent Transfer email notification oldOwner=  " +
                                    "[%s] newOwner =[%s] for certificate [%s] on date = [%s]",
                            getUserEmail(getUserDetails(certificateUser)),  getUserEmail(getUserDetails(certificateRequest.getCertOwnerNtid()))
                            , metaDataParams.get("certificateName"),java.time.LocalDateTime.now())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));

			return ResponseEntity.status(HttpStatus.OK)
					.body(MESSAGES + "Certificate Owner Transferred Successfully" + "\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "updateCertOwner")
					.put(LogMessage.MESSAGE, "Certificate owner Transfer failed for CertificateID")
					.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ERRORS + "Certificate owner Transfer failed" + "\"]}");
		}
	
	} catch (Exception e) {
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
				.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
						Arrays.toString(e.getStackTrace()), e.getMessage()))
				.build()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ERRORS + e.getMessage() + "\"]}");
	}

    }
    /**
     * This method is to update the notification emails list with new owner email id
     * @param metaDataParams
     * @param oldEmailId
     * @param certOwnerEmailId
     */
    private void updateNotificationEmails(Map<String, String> metaDataParams,String oldEmailId,String certOwnerEmailId) {
        String notificationEmails = metaDataParams.get("notificationEmails");
        if (!StringUtils.isEmpty(notificationEmails) && (notificationEmails.toLowerCase().contains(oldEmailId.toLowerCase()))) {
            notificationEmails = notificationEmails.replaceAll("(?i)" + oldEmailId, certOwnerEmailId).toLowerCase();
            notificationEmails=
                    new LinkedHashSet<String>(Arrays.asList(notificationEmails.split(","))).toString().replaceAll("(^\\[|\\]$)",
                    "").replace(", ", ",");

            metaDataParams.put("notificationEmails", notificationEmails);
        }
    }


    //get Displayable name
    private String getDisplayName(DirectoryUser directoryUser) {
        String displayName = "";
        if (Objects.nonNull(directoryUser)) {
            displayName = directoryUser.getDisplayName();
        }
        return displayName;
    }

    //get User Email
    private String getUserEmail(DirectoryUser directoryUser) {
        String emailId = "";
        if (Objects.nonNull(directoryUser)) {
            emailId = directoryUser.getUserEmail();
        }
        return emailId;
    }

    /**
     * Sending transfer email
     *
     * @param metaDataParams
     * @param newOwner
     * @param oldOwner
     */
    private void sendTransferEmail(Map<String, String> metaDataParams, String newOwner, String oldOwner) {
        Map<String, String> mailTemplateVariables = new HashMap<>();
        mailTemplateVariables.put("oldOwnerName", getDisplayName(getUserDetails(oldOwner)));
        mailTemplateVariables.put("newOwnerName", getDisplayName(getUserDetails(newOwner)));
        mailTemplateVariables.put("oldOwnerEmail", getUserEmail(getUserDetails(oldOwner)));
        mailTemplateVariables.put("newOwnerEmail", getUserEmail(getUserDetails(newOwner)));
        mailTemplateVariables.put(SSLCertificateConstants.CERT_TYPE, StringUtils.capitalize(metaDataParams.get(SSLCertificateConstants.CERT_TYPE)));
        mailTemplateVariables.put(SSLCertificateConstants.CERT_NAME, metaDataParams.get("certificateName"));
        mailTemplateVariables.put("certStartDate", (Objects.nonNull(metaDataParams.get("createDate"))) ?
                metaDataParams.get("createDate") : "N/A");
        mailTemplateVariables.put("certEndDate", (Objects.nonNull(metaDataParams.get("expiryDate"))) ?
                metaDataParams.get("expiryDate") : "N/A");
        mailTemplateVariables.put(SSLCertificateConstants.CONTACT_LINK, fromEmail);
        String subject =
                SSLCertificateConstants.TRANSFER_EMAIL_SUBJECT + " - " + metaDataParams.get("certificateName");
        if (Objects.nonNull(metaDataParams.get(SSLCertificateConstants.DNS_NAMES))) {
            String dnsNames = Collections.singletonList(metaDataParams.get(SSLCertificateConstants.DNS_NAMES)).toString();
            mailTemplateVariables.put(SSLCertificateConstants.DNS_NAMES, dnsNames.substring(2, dnsNames.length() - 2));
        } else {
            mailTemplateVariables.put(SSLCertificateConstants.DNS_NAMES, "N/A");
        }

        emailUtils.sendTransferEmail(fromEmail, mailTemplateVariables, subject);
    }


    /**
	 * Method to validate certificate approval status in nclm and get the latest
	 * certificate details
	 * @param certName
	 * @param certType
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> validateApprovalStatusAndGetCertificateDetails(String certName, String certType,
			UserDetails userDetails) {
		if (!isValidInputs(certName, certType)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
		if (!certType.equalsIgnoreCase("external")) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ? SSLCertificateConstants.SSL_CERT_PATH
				: SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
		String certificatePath = metaDataPath + '/' + certName;
		String authToken = null;
		if (!ObjectUtils.isEmpty(userDetails)) {
			if (userDetails.isAdmin()) {
				authToken = userDetails.getClientToken();
			} else {
				authToken = userDetails.getSelfSupportToken();
			}
			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken,
					certName, certType);
			if (ObjectUtils.isEmpty(certificateMetaData)) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
						.put(LogMessage.MESSAGE, "No certificate available")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"No certificate available\"]}");
			} else {

				if (certificateMetaData.getRequestStatus()!=null && certificateMetaData.getRequestStatus().equalsIgnoreCase(SSLCertificateConstants.APPROVED)) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("{\"errors\":[\"Certificate already approved\"]}");
				}

				if ((!userDetails.isAdmin()) && (userDetails.getUsername() != null)
						&& (!userDetails.getUsername().equalsIgnoreCase(certificateMetaData.getCertOwnerNtid()))) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
							.put(LogMessage.MESSAGE, "Access denied: No permission to access this certificate")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("{\"errors\":[\"Access denied: No permission to access this certificate\"]}");
				}
				return getCertificateDetailsAndProcessMetadata(certificatePath, authToken, certificateMetaData, userDetails);
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
					.put(LogMessage.MESSAGE, "Access denied: No permission to access this certificate")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Access denied: No permission to access this certificate\"]}");
		}
	}

    /**
     * To delete r/w/o/d policies
     * @param certType
     * @param certificateName
     * @param token
     * @return
     */

    private boolean deletePolicies(String certType,String certificateName, String token) {
        boolean policiesDeleted = false;
        Map<String, Object> policyMap = new HashMap<>();
        Map<String, String> accessMap = new HashMap<>();
        String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ?
                SSLCertificateConstants.SSL_CERT_PATH : SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;

        String certPathVal = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ?
                SSLCertificateConstants.SSL_CERT_PATH_VALUE : SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

        String policyValue = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ?
                SSLCertificateConstants.INTERNAL_POLICY_NAME : SSLCertificateConstants.EXTERNAL_POLICY_NAME;

        String certMetadataPath = metaDataPath + '/' + certificateName;
        String certPath = certPathVal + certificateName;
        
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
        		.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
        		.put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE)
        		.put(LogMessage.MESSAGE, "Trying to delete SSL certificate policies")
        		.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
        		.build()));
        //Read Policy
        accessMap.put(certPath, TVaultConstants.READ_POLICY);
        accessMap.put(certMetadataPath, TVaultConstants.READ_POLICY);
        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.READ_CERT_POLICY_PREFIX + policyValue + "_" + certificateName);
        policyMap.put(SSLCertificateConstants.ACCESS_STRING, accessMap);

        String policyRequestJson = ControllerUtil.convetToJson(policyMap);
        Response readResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_DELETE_ENDPOINT, policyRequestJson, token);

        //Write Policy
        accessMap.put(certPath, TVaultConstants.WRITE_POLICY);
        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX + policyValue + "_" + certificateName);
        policyRequestJson = ControllerUtil.convetToJson(policyMap);
        Response writeResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_DELETE_ENDPOINT, policyRequestJson, token);

        //Deny Policy
        accessMap.put(certPath, TVaultConstants.DENY_POLICY);
        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.DENY_CERT_POLICY_PREFIX + policyValue + "_" + certificateName);
        policyRequestJson = ControllerUtil.convetToJson(policyMap);
        Response denyResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_DELETE_ENDPOINT, policyRequestJson, token);

        //Owner Policy
        accessMap.put(certPath, TVaultConstants.SUDO_POLICY);
        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX + policyValue + "_" + certificateName);
        policyRequestJson = ControllerUtil.convetToJson(policyMap);
        Response sudoResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_DELETE_ENDPOINT, policyRequestJson, token);

        if ((readResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
                writeResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
                denyResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
                && sudoResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
        ) ||
                (readResponse.getHttpstatus().equals(HttpStatus.OK) &&
                        writeResponse.getHttpstatus().equals(HttpStatus.OK) &&
                        denyResponse.getHttpstatus().equals(HttpStatus.OK))
                        && sudoResponse.getHttpstatus().equals(HttpStatus.OK)
        ) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
                    put(LogMessage.MESSAGE, "SSL Certificate Policies Deletion Success").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            policiesDeleted = true;
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
                    put(LogMessage.MESSAGE, "SSL Certificate policies deletion failed").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }

        return policiesDeleted;
    }


    public String getExternalCertReqStatus(SSLCertificateMetadataDetails certificateMetaData) throws Exception {
	    String status= null;
        String actionRequest = "/certmanager/actionRequestStatus";
        String reqStatusUrl = requestStatusUrl.replace(SSLCertificateConstants.ACTION_ID, String.valueOf(certificateMetaData.getActionId()));
        CertResponse certResponse= reqProcessor.processCert(actionRequest, "", getNclmToken(),
                getCertmanagerEndPoint(reqStatusUrl));
        Map<String, Object> responseMap = ControllerUtil.parseJson(certResponse.getResponse());
        if (!MapUtils.isEmpty(responseMap)) {
            status = responseMap.get(SSLCertificateConstants.CONCLUSION).toString();
        }
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getExternalCertReqStatus").
                put(LogMessage.MESSAGE, String.format("Status  =  [%s] for certificate = [%s] ", status,
                        certificateMetaData.getCertificateName())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return status;
    }

    /**
     * This method will be responsible for deleting the metadata and permissions
     * @param certificateMetaData
     * @param certificatePath
     * @param authToken
     * @return
     */
    //Delete the Metadata and permissions
    private boolean deleteMetaDataAndPermissions(SSLCertificateMetadataDetails certificateMetaData,
                                                 String certificatePath, String authToken) {
        //Delete Metadata
        Response response = reqProcessor.process("/delete", "{\"path\":\"" + certificatePath + "\"}", authToken);
        if (HttpStatus.NO_CONTENT.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.DELETE_METADATA_PERMISSIONS).
                    put(LogMessage.MESSAGE, String.format("Certificate Metadata deleted for certificatePath =  [%s] " +
                            "and certificate name = [%s]" , certificatePath,certificateMetaData.getCertificateName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.DELETE_METADATA_PERMISSIONS).
                    put(LogMessage.MESSAGE, String.format("Failed to delete Certificate Metadata  for certificate =  " +
                            "[%s] and certificate name = [%s] ", certificatePath,certificateMetaData.getCertificateName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return false;
        }

        //Delete permissions
        String permissionMetaDataPath = (certificateMetaData.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ?
                SSLCertificateConstants.SSL_CERT_PATH_VALUE + certificateMetaData.getCertificateName() :
                SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT + certificateMetaData.getCertificateName();
        Response metadataResponse = reqProcessor.process("/delete", "{\"path\":\"" + permissionMetaDataPath + "\"}", authToken);
        if (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.DELETE_METADATA_PERMISSIONS).
                    put(LogMessage.MESSAGE, String.format("Certificate Metadata Permissions deleted for certificate " +
                            "=[%s]  and certificate name=[%s] ", permissionMetaDataPath,
                            certificateMetaData.getCertificateName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.DELETE_METADATA_PERMISSIONS).
                    put(LogMessage.MESSAGE, String.format("Failed to delete Certificate Metadata  for certificate   " +
                            "[%s] and certificate name = [%s] ", permissionMetaDataPath,certificateMetaData.getCertificateName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return false;
        }

        //Delete policies
       if(deletePolicies(certificateMetaData.getCertType(),certificateMetaData.getCertificateName(),authToken)) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.DELETE_METADATA_PERMISSIONS).
                    put(LogMessage.MESSAGE, String.format("Policies(r/w/s/o)  deleted for certificate =[%s] ",certificateMetaData.getCertificateName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        } else {
           log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                   put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                   put(LogMessage.ACTION, SSLCertificateConstants.DELETE_METADATA_PERMISSIONS).
                   put(LogMessage.MESSAGE, String.format("Failed to delete Policies(r/w/s/o) for certificate=[%s] ",
                           certificateMetaData.getCertificateName())).
                   put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                   build()));
           return false;
       }

        return true;
    }
	/**
	 * @param certificatePath
	 * @param authToken
	 * @param certificateMetaData
	 * @return
	 */
	private ResponseEntity<String> getCertificateDetailsAndProcessMetadata(String certificatePath, String authToken,
			SSLCertificateMetadataDetails certificateMetaData, UserDetails userDetails) {
		try {
            if (certificateMetaData.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)) {
                String status = getExternalCertReqStatus(certificateMetaData);
                if ((status != null) && status.equalsIgnoreCase(SSLCertificateConstants.STATUS_REJECTED)) {
                    if (certificateMetaData.getRequestStatus().equalsIgnoreCase(SSLCertificateConstants.RENEW_PENDING)) {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, SSLCertificateConstants.GET_CERTIFICATE_DETAILS_PROCESS_METADATA).
                                put(LogMessage.MESSAGE, String.format("Renew Certificate has been rejected from NCLM =" +
                                                " [%s]  But user still access old certificate until expiry",
                                        certificateMetaData.getCertificateName())).
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors" +
                                "\":[\"Renew Certificate has been rejected . Validate request and try " +
                                "again\"]}");
					} else if (deleteAllPermissionAddedToCertificate(certificateMetaData, certificatePath, authToken,
							userDetails)) {
						// Delete certificate metadata, policies and permission path details
						deleteMetaDataAndPermissions(certificateMetaData, certificatePath, authToken);

						//delete certificate name from application metadata list
		                   boolean sslApplicationMetaDataSaveStatus;
		                  String jsonStr = new ObjectMapper().writeValueAsString(certificateMetaData);
		                  JsonParser jsonParser = new JsonParser();
		                  JsonObject object = ((JsonObject) jsonParser.parse(jsonStr));
		                   sslApplicationMetaDataSaveStatus = updatecertificateMetadataForApplicationDetails(object, authToken);
						
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, SSLCertificateConstants.GET_CERTIFICATE_DETAILS_PROCESS_METADATA).
                                put(LogMessage.MESSAGE, String.format("Certificate has been rejected from NCLM =  " +
                                                "[%s]  and deleted MetaData , Permissions and policies ",
                                        certificateMetaData.getCertificateName())).
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Certificate has been rejected . " +
                                " Validate request and try again\"]}");
                    } else {
                        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, SSLCertificateConstants.GET_CERTIFICATE_DETAILS_PROCESS_METADATA).
                                put(LogMessage.MESSAGE, String.format("Error While deleting the metadata and " +
                                                "permission for certificate =[%s]  and deleted MetaData and Permissions",
                                        certificateMetaData.getCertificateName())).
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":[\"Failed to " +
                                "delete the metadata or permissions\"]}");
                    }
                }
            }
			CertificateData certificateData = getExternalCertificate(certificateMetaData);
			if(!ObjectUtils.isEmpty(certificateData)) {
				return processCertificateDataAndUpdateMetadata(certificatePath, authToken, certificateMetaData,
						certificateData);
			}else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG).
						put(LogMessage.MESSAGE, "Certificate may not be approved  from NCLM").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Certificate may not be approved \"]}");
			}
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG).
					put(LogMessage.MESSAGE, String.format("Failed to verify the certificate = [%s] approval status", certificateMetaData.getCertificateName())).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to verify the certificate approval status \"]}");
		}
	}

	/**
	 * Method to delete all permissions added to the certificate.
	 * @param certificateMetaData
	 * @param certificatePath
	 * @param authToken
	 * @param userDetails
	 * @return
	 */
	private boolean deleteAllPermissionAddedToCertificate(SSLCertificateMetadataDetails certificateMetaData,
			String certificatePath, String authToken, UserDetails userDetails) {
		boolean permissionRemoved = true;
		try {
			Response response = getCertificateDetailsByMatadataPath(certificatePath, authToken);
			JsonParser jsonParser = new JsonParser();
			JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");

			//remove user permissions
			deleteUserPermissionForCertificate(certificateMetaData.getCertType(), certificateMetaData.getCertificateName(), userDetails, jsonParser, object);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
			        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
			        .put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
			        .put(LogMessage.MESSAGE, String.format("DeleteUserPermissionForCertificate Completed for certificate = [%s]", certificateMetaData.getCertificateName()))
			        .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			//remove group permissions
			removeGroupPermissionsToCertificate(certificateMetaData.getCertType(), certificateMetaData.getCertificateName(), userDetails, jsonParser, object);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
			        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
			        .put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
			        .put(LogMessage.MESSAGE, String.format("RemoveGroupPermissionsToCertificate Completed for certificate = [%s]", certificateMetaData.getCertificateName()))
			        .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			//remove AWS role permissions
			deleteAwsRoleOnCertificateDelete(certificateMetaData.getCertificateName(), authToken, jsonParser, object);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
			        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
			        .put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
			        .put(LogMessage.MESSAGE, String.format("DeleteAwsRoleOnCertificate Completed for certificate = [%s]", certificateMetaData.getCertificateName()))
			        .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			//remove App role permissions
			deleteApprolePolicyAssociationOnCertificate(certificateMetaData.getCertificateName(), authToken, jsonParser, object);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
			        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
			        .put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
			        .put(LogMessage.MESSAGE, String.format("DeleteApprolePolicyAssociationOnCertificate Completed for certificate = [%s]", certificateMetaData.getCertificateName()))
			        .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			// remove Sudo permissions
			removeSudoPermissionForPreviousOwner(certificateMetaData.getCertOwnerNtid().toLowerCase(),
					certificateMetaData.getCertificateName(), userDetails,
					certificateMetaData.getCertType());
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
			        .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
			        .put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
			        .put(LogMessage.MESSAGE, String.format("RemoveSudoPermissionForPreviousOwner Completed for certificate = [%s]", certificateMetaData.getCertificateName()))
			        .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		} catch (JsonSyntaxException e) {
			permissionRemoved = false;
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG).
					put(LogMessage.MESSAGE, "Remove permissions to the certificate failed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}

		return permissionRemoved;
	}

    /**
     * Approle policy delete as part of certificate delete
     * @param certificateName
     * @param token
     * @param jsonParser
     * @param object
     */
    private void deleteApprolePolicyAssociationOnCertificate(String certificateName, String token, JsonParser jsonParser,
			JsonObject object) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG).
                put(LogMessage.MESSAGE, "Trying delete ApprolePolicyAssociationOnCertificate").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        Map<String, String> appRoleParams = new HashMap<>();
		if (object.get("app-roles") != null) {
			JsonObject appRoleObj = ((JsonObject) jsonParser.parse(object.get("app-roles").toString()));
			appRoleParams = new Gson().fromJson(appRoleObj.toString(), Map.class);
			if (!appRoleParams.isEmpty()) {
				for (Map.Entry<String, String> entry : appRoleParams.entrySet()) {
					ResponseEntity<String> response = checkPolicyDetailsAndRemoveApproleFromCertificate(token,
			entry.getKey(), certificateName, entry.getValue(), object.get(SSLCertificateConstants.CERT_TYPE).getAsString());
					if (response.getStatusCode().equals(HttpStatus.OK)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
								.put(LogMessage.MESSAGE,
										String.format("%s, App role is deleted as part of deleting certificate.",
												entry.getKey()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					} else {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)
								.put(LogMessage.MESSAGE,
										String.format("%s, App Role deletion as part of deleting certificate failed.",
												entry.getKey()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					}
				}
			}
		}
    }

	/**
	 * @param certificatePath
	 * @param authToken
	 * @param certificateMetaData
	 * @param certificateData
	 * @return
	 * @throws JsonProcessingException
	 */
	private ResponseEntity<String> processCertificateDataAndUpdateMetadata(String certificatePath, String authToken,
			SSLCertificateMetadataDetails certificateMetaData, CertificateData certificateData)
			throws JsonProcessingException {
		Response response = reqProcessor.process("/read", "{\"path\":\"" + certificatePath + "\"}", authToken);
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG).
					put(LogMessage.MESSAGE, "Certficate unavailable").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Certficate unavailable\"]}");
		}
		Map<String, String> metaDataParams = setMetadataParamsForCertificateDetails(certificateMetaData,
				certificateData, response);
		return updateCertificateMetadata(certificatePath, authToken, metaDataParams);
	}

	/**
	 * @param certificateMetaData
	 * @param certificateData
	 * @param response
	 * @return
	 */
	private Map<String, String> setMetadataParamsForCertificateDetails(
			SSLCertificateMetadataDetails certificateMetaData, CertificateData certificateData, Response response) {
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		Map<String, String> metaDataParams = new Gson().fromJson(object.toString(), Map.class);

		if(certificateData.getPreviousCertId() != null) {
			if(certificateData.getPreviousCertId() != certificateMetaData.getCertificateId()) {
				metaDataParams.put("requestStatus", SSLCertificateConstants.APPROVED);
			}
		}else {
			metaDataParams.put("requestStatus", SSLCertificateConstants.APPROVED);
		}
		metaDataParams.put("authority", certificateData.getAuthority()!=null ? certificateData.getAuthority(): object.get("authority").getAsString());
		metaDataParams.put(SSLCertificateConstants.CERTIFICATE_ID,((Integer)certificateData.getCertificateId()).toString()!=null ?
				((Integer)certificateData.getCertificateId()).toString() : object.get(SSLCertificateConstants.CERTIFICATE_ID).getAsString());
		metaDataParams.put("createDate", certificateData.getCreateDate()!=null ? certificateData.getCreateDate() : object.get("createDate").getAsString());
		metaDataParams.put("expiryDate", certificateData.getExpiryDate()!=null ? certificateData.getExpiryDate() : object.get("expiryDate").getAsString());
		metaDataParams.put("certificateStatus", certificateData.getCertificateStatus()!=null ?
				certificateData.getCertificateStatus(): object.get("certificateStatus").getAsString());
		metaDataParams.put(SSLCertificateConstants.CONTAINER_NAME, certificateData.getContainerName()!=null ?
				certificateData.getContainerName() : object.get(SSLCertificateConstants.CONTAINER_NAME).getAsString());
        if(Objects.nonNull(certificateData.getDnsNames())) {
            metaDataParams.put(SSLCertificateConstants.DNS_NAMES, certificateData.getDnsNames().toString());
        }

		return metaDataParams;
	}

	/**
	 * @param certificatePath
	 * @param authToken
	 * @param metaDataParams
	 * @return
	 * @throws JsonProcessingException
	 */
	private ResponseEntity<String> updateCertificateMetadata(String certificatePath, String authToken,
			Map<String, String> metaDataParams) throws JsonProcessingException {
		boolean sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(certificatePath, metaDataParams, authToken);
		if(sslMetaDataUpdationStatus) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format ("Certificate approved [%s] ", certificatePath)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Certificate approved and metadata successfully updated \"]}");
		}else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Metadata update failed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Metadata update failed\"]}");
		}
	}

    /**
     * Method is to get the certificate details if exists
     * @param certificateMetaData
     * @return
     * @throws Exception
     */
	private CertificateData getExternalCertificate(SSLCertificateMetadataDetails certificateMetaData) throws Exception {
		CertificateData certificateData = null;
		int containerId = certificateMetaData.getContainerId();
		String certName = certificateUtils.getActualCertifiacteName(certificateMetaData.getCertificateName());
		String nclmAccessToken = getNclmToken();
		if (StringUtils.isEmpty(nclmAccessToken)) {
			return null;
		}
		String findCertificateEndpoint = "/certmanager/findCertificate";
		String targetEndpoint = findCertificate.replace("certname", String.valueOf(certName)).replace("cid",
				String.valueOf(containerId));
		CertResponse response = reqProcessor.processCert(findCertificateEndpoint, "", nclmAccessToken,
				getCertmanagerEndPoint(targetEndpoint));
		Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
		if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse())
				.get(SSLCertificateConstants.CERTIFICATES) != null)) {
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
			if (jsonObject != null) {
				JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
				certificateData = setLatestCertificate(certificateData, certName, jsonArray);
			}
		}
		return certificateData;
	}

	/**
	 * @param certificateData
	 * @param certName
	 * @param jsonArray
	 * @return
	 */
	private CertificateData setLatestCertificate(CertificateData certificateData, String certName,
			JsonArray jsonArray) {
		for (int i = 0; i < jsonArray.size(); i++) {
		    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
		    if ((Objects.equals(getCertficateName(jsonElement.get("sortedSubjectName").getAsString()), certName))
		            && jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS).getAsString().
		            equalsIgnoreCase(SSLCertificateConstants.ACTIVE)) {
		        certificateData= new CertificateData();
		        certificateData.setCertificateId(Integer.parseInt(jsonElement.get(SSLCertificateConstants.CERTIFICATE_ID).getAsString()));
		        certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
		        certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));
		        certificateData.setContainerName(validateString(jsonElement.get(SSLCertificateConstants.CONTAINER_NAME)));
		        certificateData.setCertificateStatus(validateString(jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS)));
		        certificateData.setCertificateName(certificateUtils.getVaultCompactibleCertifiacteName(certName));
		        certificateData.setAuthority((!StringUtils.isEmpty(jsonElement.get("enrollServiceInfo")) ?
		                 validateString(jsonElement.get("enrollServiceInfo").getAsJsonObject().get("name")) :
		                 null));
				certificateData.setPreviousCertId((!ObjectUtils.isEmpty(jsonElement.get("previous")) ? 
						Integer.parseInt(jsonElement.get("previous").getAsString()) : null));
				if(Objects.nonNull(jsonElement.getAsJsonObject("subjectAltName"))){
                    JsonObject subjectAltNameObject = jsonElement.getAsJsonObject("subjectAltName");
                    JsonArray jsonArr = subjectAltNameObject.getAsJsonArray("dns");
                    List<String> list = new ArrayList<>();
                    for(int index=0; index < jsonArr.size(); index++) {
                        list.add(jsonArr.get(index).getAsString());
                    }
                    certificateData.setDnsNames(list);
	                }
		        break;
		    }

		}
		return certificateData;
	}

	/**
	 * Validates User inputs
	 * @param certName
	 * @param certType
	 * @return
	 */
	private boolean isValidInputs(String certName, String certType) {
		boolean isValid = true;
		if (ObjectUtils.isEmpty(certName)
				|| ObjectUtils.isEmpty(certType)
				|| certName.contains(" ")
	            || (!certName.endsWith(certificateNameTailText))
	            || (certName.contains(".-"))
	            || (certName.contains("-."))
	            || (!certType.matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))
				) {
			isValid = false;
		}
		return isValid;
	}

    
    /**
	 * @param userName
	 * @param certificateName
	 * @param authToken
	 * @return
	 */
	private ResponseEntity<String> removeSudoPermissionForPreviousOwner(String userName, String certificateName,
			UserDetails userDetails, String certificateType) {
		OIDCEntityResponse oidcEntityResponse = new OIDCEntityResponse();
		String authToken = "";
		String certPrefix=(certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		
		String metaDataPath = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

	        	authToken = userDetails.getSelfSupportToken();
		String certificatePath = metaDataPath + certificateName;
		
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
				
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		Response userResponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}", authToken);	
		}
		else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
			userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}", authToken);
		}
		else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC implementation changes
						ResponseEntity<OIDCEntityResponse> responseEntity = oidcUtil.oidcFetchEntityDetails(authToken, userName, userDetails, true);
						if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
							if (responseEntity.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
								log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
										.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION, "checkUserPolicyAndRemoveFromCertificate")
										.put(LogMessage.MESSAGE, "Trying to fetch OIDC user policies, failed")
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
										.build()));
							}
							return ResponseEntity.status(HttpStatus.NOT_FOUND)
									.body("{\"messages\":[\"User configuration failed. Invalid user\"]}");
						}
						oidcEntityResponse.setEntityName(responseEntity.getBody().getEntityName());
						oidcEntityResponse.setPolicies(responseEntity.getBody().getPolicies());
						userResponse.setResponse(oidcEntityResponse.getPolicies().toString());
						userResponse.setHttpstatus(responseEntity.getStatusCode());
		}

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse.getHttpstatus())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				ObjectMapper objMapper = new ObjectMapper();
				if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
					currentpolicies.addAll(oidcEntityResponse.getPolicies());
				} else {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
					if (TVaultConstants.LDAP.equals(vaultAuthMethod)) {
						groups = objMapper.readTree(responseJson).get("data").get("groups").asText();
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			
			policies.addAll(currentpolicies);			
			policies.remove(writePolicy);
			policies.remove(sudoPolicy);
			policies.remove(readPolicy);
			policies.remove(denyPolicy);
			
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		Response ldapConfigresponse = new Response();
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, authToken);
		}
		else if (TVaultConstants.LDAP.equals(vaultAuthMethod)){
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, authToken);
		}
		else if (TVaultConstants.OIDC.equals(vaultAuthMethod)) {
			// OIDC Implementation : Entity Update
			try {
				ldapConfigresponse = oidcUtil.updateOIDCEntity(policies,
						oidcEntityResponse.getEntityName());
                oidcUtil.renewUserToken(userDetails.getClientToken());
			} catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "removeSudoPermissionForPreviousOwner")
						.put(LogMessage.MESSAGE, String.format("Exception while updating the identity"))
						.put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
			}
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
			return updateMetadataForRemoveUserFromCertificate(userName, certificatePath, authToken, groups,
					currentpoliciesString, userDetails, currentpolicies, oidcEntityResponse.getEntityName());
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Failed to remvoe the user from the certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the " +
                    "user from the certificate\"]}");
		}
	}


    /**
     * This method will be used to unlick the certificate info from application no changes in nclm
     *
     * @param userDetails
     * @param token
     * @param certificateName
     * @param certType
     * @return
     */
    public ResponseEntity<String> unLinkCertificate(UserDetails userDetails, String certificateName,
                                                    String certType,String releaseReason) {
        try {

            boolean isValid = ControllerUtil.validateInputs(certificateName,certType);

            if(!userDetails.isAdmin()){
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "unLinkCertificate").
                        put(LogMessage.MESSAGE, "Access denied: No permission to delete certificate").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"errors\":[\"Access denied: No " +
                        "permission to release certificate[" + certificateName + "]\"]}");
            }


            if(!isValid){
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "unLinkCertificate").
                        put(LogMessage.MESSAGE, "Invalid input values").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
            }
			String authToken = "";
			// Get the token
			if (userDetails.isAdmin()) {
				authToken = userDetails.getClientToken();
			} else {
				authToken = userDetails.getSelfSupportToken();
			}

            //Metadata path

            String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ?
                    SSLCertificateConstants.SSL_CERT_PATH + '/' + certificateName : SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + '/' + certificateName;
            //Permission path

            String permissionMetaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ?
                    SSLCertificateConstants.SSL_CERT_PATH_VALUE + '/' + certificateName : SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT + '/' + certificateName;
            Response response = getCertificateDetailsByMatadataPath(metaDataPath, authToken);

            JsonParser jsonParser = new JsonParser();
            JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
            
            //remove certificate name from application metadata
            boolean appMetadataStatus = updatecertificateMetadataForApplicationDetails(object, authToken);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("delete details from application metadata status is [%s] for certificate " +
                            "= [%s]", appMetadataStatus,certificateName))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

            //remove user permissions
            deleteUserPermissionForCertificate(certType, certificateName, userDetails, jsonParser, object);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("deleteUserPermissionForCertificate Completed for certificate " +
                            "= [%s]", certificateName))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

            //remove group permissions
            removeGroupPermissionsToCertificate(certType, certificateName, userDetails, jsonParser, object);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("removeGroupPermissionsToCertificate Completed for certificate " +
                            "= [%s]", certificateName))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));


            //Remove certificate policies
            boolean isDeleted = deletePolicies(certType, certificateName, authToken);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate-")
                    .put(LogMessage.MESSAGE, String.format("deletePolicies Completed for certificate " +
                            "= [%s] = isDeleted = [%s]", certificateName, isDeleted))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

            //deleteCertificateDetailsFromCertMetaPath - metaDataPath
            response = deleteCertificateDetailsFromCertMetaPath(metaDataPath, authToken);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("deleteCertificateDetailsFromCertMetaPath->metaDataPath Completed for " +
                            "certificate = [%s] = responseStatus = [%s]", certificateName, response.getHttpstatus()))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

            //deleteCertificateDetailsFromCertMetaPath - permissionMetaDataPath
            Response metadataResponse = deleteCertificateDetailsFromCertMetaPath(permissionMetaDataPath, authToken);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("deleteCertificateDetailsFromCertMetaPath->metadataResponse Completed for " +
                            "certificate = [%s] = responseStatus = [%s]", certificateName, metadataResponse.getResponse()))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

            //remove AWS role permissions
            deleteAwsRoleOnCertificateDelete(certificateName, authToken, jsonParser, object);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("DeleteAwsRoleOnCertificate Completed for certificate = [%s]", certificateName))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

            //remove App role permissions
            deleteApprolePolicyAssociationOnCertificate(certificateName, authToken, jsonParser, object);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("DeleteApprolePolicyAssociationOnCertificate Completed for certificate = [%s]", certificateName))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));


            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate")
                    .put(LogMessage.MESSAGE, String.format("unLinkCertificate-> Certificate released from application" +
                                    " successfully - details = certificate name = [%s] = certType = [%s] = release by = [%s] = on date = [%s] = reason = [%s]",
                            certificateName, certType,userDetails.getUsername(),LocalDateTime.now(),releaseReason))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));




            return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Certificate [" + certificateName + "] details " +
                    " removed from the application \"]}");
        } catch (Exception ex) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "unLinkCertificate -> metadataResponse")
                    .put(LogMessage.MESSAGE, String.format("Exception while removing the certificate from application " +
                            "certificatename = [%s] = message = [%s]", certificateName, ex.getMessage()))
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
            return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Certificate [" + certificateName + "] details " +
                    " failed to remove from the application \"]}");
        }
    }



	 /**
	 * Delete SSL Certificate and update metadata
	 * 
	 * @param certificateId
	 * @param token
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public ResponseEntity<String> deleteCertificate( String token, String certType, String certificateName, UserDetails userDetails) {
		if (!isValidInputs(certificateName, certType)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteSSLCertificate")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "deleteSSLCertificate")
				.put(LogMessage.MESSAGE, String.format("Trying to delete SSL cerrtificate [%S]", certificateName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		
		Map<String, String> metaDataParams = new HashMap<>();
		String endPoint =certificateName;	
		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                new StringBuilder(SSLCertificateConstants.SSL_CERT_PATH).append("/").append(endPoint).toString() : new StringBuilder(SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH).append("/").append(endPoint).toString();
		String permissionMetaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
				new StringBuilder(SSLCertificateConstants.SSL_CERT_PATH_VALUE).append("/").append(endPoint).toString() : new StringBuilder(SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT).append("/").append(endPoint).toString();
		
		Response response = null;
		Response metadataResponse = new Response();
		CertResponse unAssignResponse = null;
		String authToken = userDetails.getSelfSupportToken();
		if (!userDetails.isAdmin()) {
			boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certificateName,certType);
			if (!isPermission) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "validateCertOwnerPermissionForNonAdmin")
						.put(LogMessage.MESSAGE, String.format("User has no permission to access the SSL Certificate [%s]", certificateName))
						.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("{\"errors\":[\"Access denied: No permission to delete certificate\"]}");
			}
		}
		response = getCertificateDetailsByMatadataPath(metaDataPath, authToken);
		
		if (ObjectUtils.isEmpty(response) || !HttpStatus.OK.equals(response.getHttpstatus())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_CERT_DETAILS_BY_METADATA_PATH)
					.put(LogMessage.MESSAGE,
							String.format("Failed to get certificate details from metadatapath  [%s]", metaDataPath))
					.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"Certificate unavailable\"]}");
		}
		
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");				
		
		int certID = object.get(SSLCertificateConstants.CERTIFICATE_ID).getAsInt();	
		int containerId = object.get(SSLCertificateConstants.CONTAINER_ID).getAsInt();
		try {
			metaDataParams = new Gson().fromJson(object.toString(), Map.class);	
			String certificateUserId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_NTID);
			//Adding mock flag
	    	String nclmAccessToken = isMockingEnabled(certType) ? TVaultConstants.NCLM_MOCK_VALUE:  getNclmToken();
	    	if(!StringUtils.isEmpty(nclmAccessToken)) {
               //find certificates - adding mock flag
               CertificateData certData = (!isMockingEnabled(certType)) ?
                       getLatestCertificate(certificateName, nclmAccessToken, containerId) : nclmMockUtil.getDeleteCertMockResponse(metaDataParams);
               if ((!ObjectUtils.isEmpty(certData) && certData.getCertificateId() != 0)) {
            	   //Delete certificate from nclm
                   unAssignResponse = deleteCertificateFromNclm(certType, certID, nclmAccessToken, certData);

                   //remove user permissions
                   deleteUserPermissionForCertificate(certType, certificateName, userDetails, jsonParser, object);

                   //remove group permissions
                   removeGroupPermissionsToCertificate(certType, certificateName, userDetails, jsonParser, object);
                   
                   //remove AWS role permissions
                   if (object.get(SSLCertificateConstants.AWS_ROLES) != null) {
	                   deleteAwsRoleOnCertificateDelete(certificateName, authToken, jsonParser, object);
                   }

                   //remove Sudo permissions
                   removeSudoPermissionForPreviousOwner(certificateUserId.toLowerCase(), certificateName, userDetails, certType);
                   
                 //delete certificate name from application metadata list
                   boolean sslApplicationMetaDataSaveStatus;
                   sslApplicationMetaDataSaveStatus = updatecertificateMetadataForApplicationDetails(object, authToken);
                   
                   //Remove certificate policies
                   deletePolicies(certType, certificateName, authToken);
                   deleteApprolePolicyAssociationOnCertificate(certificateName, authToken, jsonParser, object);

                   if (unAssignResponse != null && (HttpStatus.OK.equals(unAssignResponse.getHttpstatus()) || (HttpStatus.NO_CONTENT.equals(unAssignResponse.getHttpstatus())))) {
                       response = deleteCertificateDetailsFromCertMetaPath(metaDataPath, authToken);
                       if(!ObjectUtils.isEmpty(response)) {
                    	   metadataResponse = deleteCertificateDetailsFromCertMetaPath(permissionMetaDataPath, authToken); 
                       }                   
                       if(ObjectUtils.isEmpty(metadataResponse)) {
                    	   log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
       							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
       							.put(LogMessage.ACTION, "deleteCertificate")
       							.put(LogMessage.MESSAGE, String.format("Certificate [%s] metadata deletion failed ", certificateName))
       							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
       							.build()));
                           return ResponseEntity.status(response.getHttpstatus())
                                   .body("{\"messages\":[\"Certificate metadata deletion failed\"]}");
                       }                   
                       

                       String certOwnerEmailId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_EMAILID);
                       String certOwnerNtId = metaDataParams.get(SSLCertificateConstants.CERT_OWNER_NTID);
                       
                       //Send an email for delete in case of internal and external
                       sendDeleteEmail(token, certType, certificateName, certOwnerEmailId, certOwnerNtId,
                               SSLCertificateConstants.CERT_DELETE_SUBJECT + " - " + certificateName,
                               "deleted", certData,metaDataParams);
                       
                       log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
           					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
           					.put(LogMessage.ACTION, "deleteCertificate")
           					.put(LogMessage.MESSAGE, String.format("CERTIFICATE [%s] - DELETED SUCCESSFULLY - BY [%s] - ON- [%s] AND TYPE [%s]", certificateName, certOwnerEmailId, LocalDateTime.now(), certType))
           					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
                       return ResponseEntity.status(HttpStatus.OK)
                               .body(MESSAGES + "Certificate deleted successfully" + "\"]}");
                   } else {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "Delete certificate Failed.")
							.put(LogMessage.MESSAGE, "Delete Request failed for CertificateID")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
					  	return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					          .body("{\"errors\":[\"Certificate deletion failed\"]}");
					}
				} else {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, "deletecertificate")
							.put(LogMessage.MESSAGE, "Delete Request failed for CertificateID")
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
							.body("{\"errors\":[\"Certificate unavailable in system.\"]}");
				}
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
					   put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).	
					   put(LogMessage.ACTION, "deletecertificate").	
					   put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).                        	
					   put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).	
					   build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + nclmErrorMessage + "\"]}");
			}
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]", Arrays.toString(e.getStackTrace()), e.getMessage()))
				.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ERRORS + e.getMessage() + "\"]}");
		}
	}

	/**
	 * Method to delete certificate from nclm
	 * @param certType
	 * @param certID
	 * @param nclmAccessToken
	 * @param certData
	 * @return
	 * @throws Exception
	 */
	private CertResponse deleteCertificateFromNclm(String certType, int certID, String nclmAccessToken,
			CertificateData certData) throws Exception {
		CertResponse unAssignResponse;
		//Adding mocking flag
		   if(!isMockingEnabled(certType) ) {
		       //Unassign certificate from target system
		       JsonObject jo = new JsonObject();
		       jo.add("targetSystemServiceIds", new GsonBuilder().create().toJsonTree(certData.getDeployStatus()));
		       String nclmApiAssignEndpoint = unassignCertificateEndpoint.replace("certID", String.valueOf(certID));
		       unAssignResponse = reqProcessor.processCert("/certificates/services/assigned", jo,
		               nclmAccessToken, getCertmanagerEndPoint(nclmApiAssignEndpoint));
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "deleteCertificateFromNclm")
					.put(LogMessage.MESSAGE, "Trying to delete SSL Certificate from NCLM")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));

		       if (unAssignResponse != null && HttpStatus.OK.equals(unAssignResponse.getHttpstatus())) {
		           //delete the certiicate
		           String nclmApiDeleteEndpoint = deleteCertificateEndpoint.replace("certID", String.valueOf(certID));
		           unAssignResponse = reqProcessor.processCert("/certificates", "",
		                   nclmAccessToken, getCertmanagerEndPoint(nclmApiDeleteEndpoint));
		       }
		   } else  {
		       unAssignResponse = nclmMockUtil.getDeleteMockResponse();
		   }
		return unAssignResponse;
	}

	/**
	 * Method to get the certificate metadata details
	 * @param metaDataPath
	 * @param authToken
	 * @return
	 */
	private Response getCertificateDetailsByMatadataPath(String metaDataPath, String authToken) {
		Response response = new Response();
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.GET_CERT_DETAILS_BY_METADATA_PATH)
				.put(LogMessage.MESSAGE, String.format("Trying to get metadata details from [%s]", metaDataPath))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));	
		try {
			response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", authToken);
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "Delete certificate Failed")
					.put(LogMessage.MESSAGE, String.format("Exception = [%s] =  Message [%s]", Arrays.toString(e.getStackTrace()), response.getResponse()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));			
		}		
		return response;
	}

	/**
	 * Method to delete group permission added to the certificate
	 * @param certType
	 * @param certificateName
	 * @param userDetails
	 * @param jsonParser
	 * @param object
	 */
	private void removeGroupPermissionsToCertificate(String certType, String certificateName, UserDetails userDetails,
			JsonParser jsonParser, JsonObject object) {
		CertificateGroup certificateGroup = new CertificateGroup();
		   Map<String, String> groupParams = new HashMap<>();
		   if (object.get("groups") != null) {
		       JsonObject groupObj = ((JsonObject) jsonParser.parse(object.get("groups").toString()));
		       groupParams = new Gson().fromJson(groupObj.toString(), Map.class);
		       if (!groupParams.isEmpty()) {
		           for (Map.Entry<String, String> entry : groupParams.entrySet()) {
		               certificateGroup.setCertificateName(certificateName);
		               certificateGroup.setCertType(certType);
		               certificateGroup.setGroupname(entry.getKey());
		               certificateGroup.setAccess(entry.getValue());
		               removeGroupFromCertificateForDelete(certificateGroup, userDetails);
		           }
		       }
		   }
	}

	/**
	 * Method to delete user permissions added to the certificate
	 * @param certType
	 * @param certificateName
	 * @param userDetails
	 * @param jsonParser
	 * @param object
	 */
	private void deleteUserPermissionForCertificate(String certType, String certificateName, UserDetails userDetails,
			JsonParser jsonParser, JsonObject object) {
		CertificateUser certificateUser = new CertificateUser();
		   Map<String, String> userParams = new HashMap<>();
		   if (object.get("users") != null) {
		       JsonObject userObj = ((JsonObject) jsonParser.parse(object.get("users").toString()));
		       userParams = new Gson().fromJson(userObj.toString(), Map.class);
		       if (!userParams.isEmpty()) {
		           for (Map.Entry<String, String> entry : userParams.entrySet()) {
		               certificateUser.setCertificateName(certificateName);
		               certificateUser.setCertType(certType);
		               certificateUser.setUsername(entry.getKey());
		               certificateUser.setAccess(entry.getValue());
		               removeUserFromCertificateForDelete(certificateUser, userDetails);
		           }
		       }
		   }
	}

	/**
	 * Delete certificate metadata details
	 * @param metaDataPath
	 * @param authToken
	 * @return
	 */
	private Response deleteCertificateDetailsFromCertMetaPath(String metaDataPath, String authToken) {
		Response response = new Response();
		
		try {
			response = reqProcessor.process("/delete", "{\"path\":\"" + metaDataPath + "\"}", authToken);
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "deleteCertificateDetailsFromCertMetaPath")
					.put(LogMessage.MESSAGE, "Trying to delete SSL certificate metadata details")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "Delete certificate Failed")
				.put(LogMessage.MESSAGE, String.format("Exception = [%s] =  Message [%s]", Arrays.toString(e.getStackTrace()), response.getResponse()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));
		}
		return response;
	}

	/**
	 * Aws role deletion as part of certificate delete
	 *
	 * @param svcAccName
	 * @param acessInfo
	 * @param token
	 */
	private void deleteAwsRoleOnCertificateDelete(String certificateName, String token, JsonParser jsonParser,
			JsonObject object) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "deleteAwsRoleOnCertificateDelete")
				.put(LogMessage.MESSAGE,
						String.format("Trying to delete AwsRole On Certificate [%s] delete", certificateName))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Map<String, String> awsParams = new HashMap<>();
		if (object.get(SSLCertificateConstants.AWS_ROLES) != null) {
			JsonObject awsObj = ((JsonObject) jsonParser.parse(object.get(SSLCertificateConstants.AWS_ROLES).toString()));
			awsParams = new Gson().fromJson(awsObj.toString(), Map.class);
			if (!awsParams.isEmpty()) {
				for (Map.Entry<String, String> entry : awsParams.entrySet()) {
					Response response = reqProcessor.process("/auth/aws/roles/delete",
							"{\"role\":\"" + entry.getKey() + "\"}", token);
					if (response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "deleteAwsRoleOn CertificateDelete")
								.put(LogMessage.MESSAGE,
										String.format("%s, AWS Role is deleted as part of deleting certificate.",
												entry.getKey()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					} else {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, "deleteAwsRoleOnCertificateDelete")
								.put(LogMessage.MESSAGE,
										String.format("%s, AWS Role deletion as part of deleting certificate failed.",
												entry.getKey()))
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
								.build()));
					}
				}
			}
		}
	}

    /**
     * Check whether mocking as enabled or not
     * @param certType
     * @return true if mocking flag as enabled and certtype as internal
     */
    private boolean isMockingEnabled(String certType) {
        return (nclmMockEnabled.equalsIgnoreCase(TVaultConstants.TRUE) && (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)
                || certType.equalsIgnoreCase(SSLCertificateConstants.EXTERNAL))) ?
                Boolean.TRUE : Boolean.FALSE;
    }

	
    
    /**
	 * Validate Permission for Non-admin User.
	 * 
	 * @param userDetails
	 * @param certificateName
	 * @return
	 */
	public boolean validateCertOwnerPermissionForNonAdmin(UserDetails userDetails, String certificateName, String certType) {
		String ownerPermissionCertName = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
				SSLCertificateConstants.OWNER_PERMISSION_CERTIFICATE + certificateName :SSLCertificateConstants.OWNER_PERMISSION_EXT_CERTIFICATE + certificateName;
		boolean isPermission = false;
		if (ArrayUtils.isNotEmpty(userDetails.getPolicies())) {
			isPermission = Arrays.stream(userDetails.getPolicies()).anyMatch(ownerPermissionCertName::equals);
			if (isPermission) {
				log.debug(
						JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Certificate permission for user " + userDetails.getUsername())
								.put(LogMessage.MESSAGE,
										"User has permission to access the certificate " + certificateName)
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
				return isPermission;
			}
		}

		return isPermission;
	}
	
	/**
     * To get all certificate metadata details.
     * @param token
     * @param certName
     * @param limit
     * @param offset
     * @return
     */
	public ResponseEntity<String> getAllCertificates(String token, String certName, Integer limit, Integer offset) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getAllCertificate").
                put(LogMessage.MESSAGE, "Trying to get all certificates").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));       
        String path = SSLCertificateConstants.SSL_CERT_PATH ;        
        String extPath = SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH ;        

        Response response;
        String certListStr = "";
        List<String> certNames = new ArrayList<String>();
        List<String> certNamesExt = new ArrayList<String>();

        response = getMetadata(token, path);        
       
            String pathStr= "";
            String endPoint = "";
            Response metadataResponse = new Response();
            JsonParser jsonParser = new JsonParser();
            JsonArray responseArray = new JsonArray();
            JsonObject metadataJsonObj=new JsonObject();    
            if (HttpStatus.OK.equals(response.getHttpstatus())) {
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
            certNames = geMatchCertificates(jsonArray,certName);            
            }
            
            response = getMetadata(token, extPath);
            if(HttpStatus.OK.equals(response.getHttpstatus())) {
            JsonObject jsonObjectExt = (JsonObject) jsonParser.parse(response.getResponse());
            JsonArray jsonArrayExt = jsonObjectExt.getAsJsonObject("data").getAsJsonArray("keys");
            certNamesExt = geMatchCertificates(jsonArrayExt,certName);            
            certNames.addAll(certNamesExt);
            }
            
            if (ObjectUtils.isEmpty(certNames)) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "getAllCertificate").
                        put(LogMessage.MESSAGE, "Retrieved empty certificate list from metadata").
                        put(LogMessage.STATUS, response.getHttpstatus().toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.OK).body(certListStr);
            }
            else if(!ObjectUtils.isEmpty(certNames)){
            Collections.sort(certNames);
            
            if(limit == null) {
                limit = certNames.size();
            }
            if (offset ==null) {
                offset = 0;
            }
            

            int maxVal = certNames.size()> (limit+offset)?limit+offset : certNames.size();
            for (int i = offset; i < maxVal; i++) {            	
                endPoint = certNames.get(i).replaceAll(CERTNAMEREGEX, "");
                if(certNamesExt.contains(certNames.get(i))) {  
                	pathStr = extPath + TVaultConstants.PATH_DELIMITER + endPoint;    
                	certNamesExt.remove(certNames.get(i));
                }else{
                	pathStr = path + TVaultConstants.PATH_DELIMITER + endPoint;
                }
                metadataResponse = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);
                if (HttpStatus.OK.equals(metadataResponse.getHttpstatus())) {
                    JsonObject certObj = ((JsonObject) jsonParser.parse(metadataResponse.getResponse())).getAsJsonObject("data");
                    certObj.remove("users");
                    certObj.remove("groups");
                    responseArray.add(certObj);
                }
            }

            if(ObjectUtils.isEmpty(responseArray)) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "get ssl metadata").
                        put(LogMessage.MESSAGE, "Certificates metadata is not available").
                        put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
            metadataJsonObj.add("keys", responseArray);
            metadataJsonObj.addProperty("offset", offset);
            certListStr = metadataJsonObj.toString();

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getAllCertificates").
                    put(LogMessage.MESSAGE, "All Certificates fetched from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
            }     
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getAllCertificates").
                put(LogMessage.MESSAGE, "Failed to get certificate list from metadata").
                put(LogMessage.STATUS, response.getHttpstatus().toString()).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
    }    
    /**
	 * Method to validate the certificate name
	 *
	 * @param certName
	 * @return
	 */
	private boolean validateCertficateEmail(String email) {
		String emailPattern =
				"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(emailPattern);
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}

	/**
	 * Get Certificates for non-admin
	 * @param userDetails
	 * @param certificateType
	 * @param limit
	 * @param offset
	 * @return
	 */
	public ResponseEntity<String> getAllCertificatesOnCertType(UserDetails userDetails, String certificateType, Integer limit, Integer offset) {
		oidcUtil.renewUserToken(userDetails.getClientToken());
		String token = userDetails.getSelfSupportToken();
		if (userDetails.isAdmin()) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "Get all owned certificates by certificate type")
					.put(LogMessage.MESSAGE, "Access denied: No permission to view certificate list")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to view certificate list\"]}");
		}
		if(!(certificateType.equals(SSLCertificateConstants.INTERNAL) || certificateType.equals(SSLCertificateConstants.EXTERNAL))) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "getAllCertificatesOnCertType")
				.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}
		String certificatePrefix = TVaultConstants.CERT_POLICY_PREFIX;
		if (certificateType.equals(SSLCertificateConstants.EXTERNAL)) {
			certificatePrefix = TVaultConstants.CERT_POLICY_EXTERNAL_PREFIX;
		}
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);

		policies = filterPoliciesBasedOnPrecedence(Arrays.asList(policies));

		List<Map<String, String>> certListUsers = new ArrayList<>();
		Map<String, List<Map<String, String>>> certificateList = new HashMap<>();
		if (policies != null) {
			for (String policy : policies) {
				getCertificateDetailsFromPolicies(certificatePrefix, certListUsers, policy);
			}
			limit = (limit == null) ? certListUsers.size() : limit;
			offset = (offset == null) ? 0 : offset;
			Integer totalCount = certListUsers.size();
			List<Map<String, String>> certSubListUsers = getSublistFromCertificatesList(limit, offset, certListUsers);
			certificateList.put(certificatePrefix, certSubListUsers);

			List<Map<String, String>> certificateCounts = new ArrayList<>();
			Map<String, String> certCount = new HashedMap();

			certCount.put("total", String.valueOf(totalCount));
			certCount.put("next", (totalCount - (certificateList.get(certificatePrefix).size() + offset)>0?String.valueOf((certificateList.get(certificatePrefix).size() + offset)):"-1"));
			certificateCounts.add(certCount);
			certificateList.put("certCount", certificateCounts);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(certificateList));
	}

	/**
	 * Method to get the sublist from certificates list.
	 * @param limit
	 * @param offset
	 * @param certListUsers
	 * @return
	 */
	private List<Map<String, String>> getSublistFromCertificatesList(Integer limit, Integer offset,
			List<Map<String, String>> certListUsers) {
		List<Map<String, String>> certSubListUsers = new ArrayList<>();
		if (!certListUsers.isEmpty()) {
			Integer totCount = certListUsers.size();
			Integer offsetVal = 0;
			Integer toindex = 0;
			Integer limitVal = offset + limit;

			offsetVal = (offset <= totCount) ? offset : totCount;
			toindex = (limitVal <= totCount) ? limitVal : totCount;

			certSubListUsers = certListUsers.subList(offsetVal, toindex);
		}
		return certSubListUsers;
	}

	/**
	 * Method to get the certificate details from policies
	 * @param certificatePrefix
	 * @param certListUsers
	 * @param policy
	 */
	private void getCertificateDetailsFromPolicies(String certificatePrefix, List<Map<String, String>> certListUsers,
			String policy) {
		Map<String, String> certificatePolicy = new HashMap<>();
		String[] certificatePolicies = policy.split("_", -1);
		if (certificatePolicies.length >= 3) {
			String[] policyName = Arrays.copyOfRange(certificatePolicies, 2, certificatePolicies.length);
			String certificateName = String.join("_", policyName);
			String sslCertType = certificatePolicies[1];
			certificateName = certificateUtils.getActualCertifiacteName(certificateName);
			if (policy.startsWith("r_")) {
				certificatePolicy.put(certificateName, "read");
			} else if (policy.startsWith("w_")) {
				certificatePolicy.put(certificateName, "write");
			} else if (policy.startsWith("d_")) {
				certificatePolicy.put(certificateName, "deny");
			}
			if (!certificatePolicy.isEmpty() && sslCertType.equals(certificatePrefix)) {
				certListUsers.add(certificatePolicy);
			}
		}
	}

	/**
	 * Filter certificates policies based on policy precedence.
	 * @param policies
	 * @return
	 */
	private String [] filterPoliciesBasedOnPrecedence(List<String> policies) {
		List<String> filteredList = new ArrayList<>();
		for (int i = 0; i < policies.size(); i++ ) {
			String policyName = policies.get(i);
			String[] _policy = policyName.split("_", -1);
			if (_policy.length >= 3) {
				String itemName = policyName.substring(1);
				List<String> matchingPolicies = filteredList.stream().filter(p->p.substring(1).equals(itemName)).collect(Collectors.toList());
				if (!matchingPolicies.isEmpty()) {
					/* deny has highest priority. Read and write are additive in nature
						Removing all matching as there might be duplicate policies from user and groups
					*/
					if (policyName.startsWith("d_") || (policyName.startsWith("w_") && !matchingPolicies.stream().anyMatch(p-> p.equals("d"+itemName)))) {
						filteredList.removeAll(matchingPolicies);
						filteredList.add(policyName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("d"+itemName))) {
						// policy is read and deny already in the list. Then deny has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("d"+itemName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("w"+itemName))) {
						// policy is read and write already in the list. Then write has precedence.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("w"+itemName);
					}
					else if (matchingPolicies.stream().anyMatch(p-> p.equals("r"+itemName)) || matchingPolicies.stream().anyMatch(p-> p.equals("o"+itemName))) {
						// policy is read and read already in the list. Then remove all duplicates read and add single read permission for that certifcates.
						filteredList.removeAll(matchingPolicies);
						filteredList.add("r"+itemName);
					}
				}
				else {
					filteredList.add(policyName);
				}
			}
		}
		return filteredList.toArray(new String[0]);
	}
	
	/**	
	 * Method to check certificate status	
	 * certificate details	
	 * @param certName	
	 * @param certType	
	 * @param userDetails	
	 * @return	
	 */	
	public ResponseEntity<String> checkCertificateStatus(String certName, String certType,	
			UserDetails userDetails) {	
		if (!isValidInputs(certName, certType)) {	
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()	
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))	
					.put(LogMessage.ACTION, "checkCertificateStatus")	
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)	
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));	
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);	
		}	
		String authToken = null;	
		if (!ObjectUtils.isEmpty(userDetails)) {	
			if (userDetails.isAdmin()) {	
				authToken = userDetails.getClientToken();	
			} else {	
				authToken = userDetails.getSelfSupportToken();	
			}	
			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken,	
					certName, certType);	
			if (ObjectUtils.isEmpty(certificateMetaData)) {	
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()	
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))	
						.put(LogMessage.ACTION, "checkCertificateStatus")	
						.put(LogMessage.MESSAGE, "No certificate available")	
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));	
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)	
						.body("{\"errors\":[\"No certificate available\"]}");	
			} else {	
				int containerId = certificateMetaData.getContainerId();	
				String nclmAccessToken = (isMockingEnabled(certType)) ? TVaultConstants.NCLM_MOCK_VALUE:getNclmToken();
				if(!StringUtils.isEmpty(nclmAccessToken)) {	
				try {
					CertificateData certData = (!isMockingEnabled(certType)) ? getLatestCertificate(certificateUtils.getActualCertifiacteName(certName),
                            nclmAccessToken, containerId):nclmMockUtil.getMockDataForRevoked();
					if(!ObjectUtils.isEmpty(certData) && certData.getCertificateId() != 0) {
						if(!certData.getCertificateStatus().equalsIgnoreCase("Revoked")) {	
							 return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Certificate is in Revoke Requested status\"]}");	
						}	
					}else {	
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
								.body("{\"errors\":[\"Certificate unavailable in system.\"]}");	
					}	
				} catch (Exception e) {	
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()	
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())	
							.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",	
									Arrays.toString(e.getStackTrace()), e.getMessage()))	
							.build()));	
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
							.body(ERRORS + e.getMessage() + "\"]}");	
				}	
				}else {	
	            	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().	
	                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).	
	                        put(LogMessage.ACTION, "checkStatus").	
	                        put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG).                        	
	                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).	
	                        build()));	
	            	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
	    					.body(ERRORS + nclmErrorMessage + "\"]}");
	            }	
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Certifictae is in Revoked status \"]}");	
			}	
		} else {	
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()	
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))	
					.put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG)	
					.put(LogMessage.MESSAGE, "Access denied: No permission to add users to this certificate")	
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));	
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)	
					.body("{\"errors\":[\"Access denied: No permission to access this certificate\"]}");	
		}	
	}

	/**
	 * Method to get all application names based on the self service groups of the
	 * user.
	 *
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getAllSelfServiceGroups(UserDetails userDetails) {
		if (!ObjectUtils.isEmpty(userDetails)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_APPLICATIONS_STRING)
					.put(LogMessage.MESSAGE, "Get all self service groups based on the user")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			String userEmail = null;
			List<String> selfServiceGroups = null;
			DirectoryUser directoryUser = getUserDetails(userDetails.getUsername());
			if (Objects.nonNull(directoryUser)) {
				userEmail = directoryUser.getUserEmail();
			}

			String accessToken = oidcUtil.getSSOToken();
			String userAADId = null;
			if ((!StringUtils.isEmpty(accessToken)) && (!StringUtils.isEmpty(userEmail))) {
				userAADId = oidcUtil.getIdOfTheUser(accessToken, userEmail);
			}

			if ((!StringUtils.isEmpty(accessToken)) && (!StringUtils.isEmpty(userAADId))) {
				selfServiceGroups = oidcUtil.getSelfServiceGroupsFromAADById(accessToken, userAADId,
						userDetails.getUsername());
			} else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_APPLICATIONS_STRING)
						.put(LogMessage.MESSAGE, "Access denied: No permission to access certificate management")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body("{\"errors\":[\"Access denied: No permission to access the certificate management\"]}");
			}

			if (selfServiceGroups != null && !selfServiceGroups.isEmpty()) {
				return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(selfServiceGroups));
			} else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_APPLICATIONS_STRING)
						.put(LogMessage.MESSAGE, "Access denied: No permission to access certificate management")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body("{\"errors\":[\"Access denied: No permission to access certificate management \"]}");
			}
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_APPLICATIONS_STRING)
					.put(LogMessage.MESSAGE, "Access denied: No permission to access certificate")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Access denied: No permission to access certificate\"]}");
		}
	}
	
	/**
     * Remove Group from certificate
     * @param certificateGroup
     * @param userDetails
     * @return
     */
    public ResponseEntity<String> removeGroupFromCertificateForDelete(CertificateGroup certificateGroup, UserDetails userDetails) {
    	
    	if(!areCertificateGroupInputsValid(certificateGroup)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG).
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
   		}
    	
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
                put(LogMessage.MESSAGE, String.format("Trying to remove Group from certificate - [%s]", certificateGroup.toString())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        
        String groupName = certificateGroup.getGroupname().toLowerCase();
   		String certificateName = certificateGroup.getCertificateName().toLowerCase();
   		String certificateType = certificateGroup.getCertType();
   		String authToken = null;
   		
   		boolean isAuthorized = true;
   		
   		if (!ObjectUtils.isEmpty(userDetails)) {
 			if (userDetails.isAdmin()) {
 				authToken = userDetails.getClientToken();   	            
 	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
 	        }
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certificateType);
   			if (!userDetails.isAdmin()) {
   			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
   			}
   			
   		}else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to remove group from this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove group from this certificate\"]}");
   		} 
   		
        if(isAuthorized){  
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG)
					.put(LogMessage.MESSAGE,
							String.format("Removed Group from certificate successsfully - [%s]",
									certificateGroup.toString()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return checkPolicyDetailsAndRemoveGroupFromCertificate(groupName, certificateName, authToken,
					certificateType, userDetails);
        } else {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
                    put(LogMessage.MESSAGE, "Access denied: No permission to remove groups from this certificate").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove groups from this certificate\"]}");
        }

    }
    /**
	 * Removes user from certificate
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromCertificateForDelete(CertificateUser certificateUser, UserDetails userDetails) {
		
		if(!areCertificateUserInputsValid(certificateUser)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG).
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
   		}
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG)
				.put(LogMessage.MESSAGE,
						String.format("Trying to remove user from certificate - [%s]", certificateUser.toString()))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
				.build()));
		String userName = certificateUser.getUsername().toLowerCase();
   		String certificateName = certificateUser.getCertificateName().toLowerCase(); 
   		String certificateType = certificateUser.getCertType();
   		String authToken = null;   		
   		boolean isAuthorized = true;
   		
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();   	            
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certificateType);
   			
   			if (!userDetails.isAdmin()) {
   				isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData); 
   	   			}
   			
   		}else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to remove user from this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}");
   		}
		
		if(isAuthorized){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG)
					.put(LogMessage.MESSAGE,
							String.format("Removed user from certificate - [%s]", certificateUser.toString()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			return checkUserPolicyAndRemoveFromCertificate(userName, certificateName, authToken, certificateType, userDetails);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Access denied: No permission to remove user from this certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}");
		}

	}

	//###################################################  ONBOARD CERTIFICATES	#######################################################
	
			/**
			 * Login to pacbot to get bearer token
			 * @param clientid
			 * @param clientsecret
			 * @return
			 * @throws Exception
			 */
		    private String authenticatePacbot(String clientid,String clientsecret) throws Exception {
		        String certManagerAPIEndpoint = pacbotGetTokenEndpoint;
		        JsonParser jsonParser = new JsonParser();
		        String encoding = Base64.getEncoder().encodeToString((clientid+":"+clientsecret).getBytes());
		        
		        		String authHeader = "Basic " + new String(encoding);
		        		String responseStr = "";
		        		String bearerToken="";    
		        
		        HttpClient httpClient;
		        try {
		            httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
		                    NoopHostnameVerifier.INSTANCE).
		                    setSSLContext(
		                            new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
		                                @Override
		                                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		                                    return true;
		                                }
		                            }).build()
		                    ).setRedirectStrategy(new LaxRedirectStrategy()).build();
		        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
		                    put(LogMessage.MESSAGE, "Failed to get certificates. Failed to create hhtpClient").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		            return null;
		        }
				try {
					HttpPost postRequest = new HttpPost(certManagerAPIEndpoint);
				        postRequest.addHeader(HttpHeaders.AUTHORIZATION, authHeader);         
				               
				        HttpResponse apiResponse = httpClient.execute(postRequest);

			            if (apiResponse.getStatusLine().getStatusCode() != 200) {
			                return null;
			            }	            
			            responseStr =  EntityUtils.toString(apiResponse.getEntity());	          
				} catch (Exception e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, "authenticatePacbot").
		                    put(LogMessage.MESSAGE,"Pacbot authentication failed.").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		            return null;
				}
				JsonObject res = jsonParser.parse(responseStr).getAsJsonObject();
		        if(!ObjectUtils.isEmpty(res)) {
		        	bearerToken = res.get("access_token").getAsString();
		        	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, "authenticatePacbot").
		                    put(LogMessage.MESSAGE,"Pacbot authentication successful.").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		        }
		            return bearerToken;
		        
		    }
		    
		       
		    /**
		     * API to onboard all the valid certificates from NCLM in the Venafibin_12345 container to tvault
		     * @param userDetails
		     * @param bearerToken
		     * @return
		     * @throws Exception 
		     */
		    public ResponseEntity<String> onboardCerts(UserDetails userDetails,  String token, Integer from, Integer size) throws Exception {
		    	JsonArray responseArray;
		    	ResponseEntity<String> response;
		    	String bearerToken = authenticatePacbot(pacbotClientId,pacbotClientSecret);
		    	responseArray = getActiveCertificaesFromPacbot(bearerToken, from, size);
		    	JsonObject jsonObject;
		    	int successCount = 0;

				if(!ObjectUtils.isEmpty(responseArray) && responseArray.size()>0) {
					for (int i = 0; i < responseArray.size(); i++) {
						jsonObject  = responseArray.get(i).getAsJsonObject();
						if(!isCertAvailableInMetadata(jsonObject, token)) {
							response = createObjectAndOnboardCert(jsonObject,userDetails);
							if (response !=null && HttpStatus.OK.equals(response.getStatusCode())) {
							successCount++;
							}
						}
						else {
						continue;
						}
					}
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, "onboardCerts").
							put(LogMessage.MESSAGE, String.format("[%s] - Certificates onboarded successfully.",successCount)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				}	else {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		   					put(LogMessage.ACTION, "onboardCerts").
		   					put(LogMessage.MESSAGE, "No valid certificates available for onbaording").
		   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		   					build()));
		   			
		   			return ResponseEntity.status(HttpStatus.NO_CONTENT).body("{\"errors\":[\"No certificate available for onbaording.\"]}");
				}
		    	return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"ssl certificates onboard completed successfully\"]}");
		    }
		    
		   /**
		    * To check if metadata is already created for the given certificate. If yes, skip the certificate
		    * @param jsonObject
		    * @param token
		    * @return
		    */
		    private boolean isCertAvailableInMetadata(JsonObject jsonObject, String token) {
		    
		    boolean isMetadataAvailable = true;
		    String endPoint = jsonObject.get(SSLCertificateConstants.COMMON_NAME).getAsString();
			String metaDataPath = (jsonObject.get(SSLCertificateConstants.CERT_TYPE).getAsString().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
		            SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + endPoint;
			Response response = new Response();
			try {		
					response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", token);		
			} catch (Exception e) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
						.put(LogMessage.MESSAGE, String.format("\"[%s]  - Skipping Onboard flow -  Error occured while checking certificate "
								+ "is available in metadata.Exception = [%s] =  Message [%s]", 
								endPoint,Arrays.toString(e.getStackTrace()), response.getResponse()))
						.build()));
				 isMetadataAvailable = false;
			}
			if (!HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
							put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
							put(LogMessage.MESSAGE, String.format("[%s] - Metadata is not available.",endPoint)).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
							build()));
				isMetadataAvailable = false;
			}else if (HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, "isCertAvailableInMetadata").
						put(LogMessage.MESSAGE, String.format("[%s]  - Skipping Onboard flow -  Certificate already available in metadata. ",endPoint)).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			return isMetadataAvailable;
		    } 
		    
		    /**
		     * Method to get certificate details from pacbot
		     * @param bearerToken
		     * @return
		     */
		    private  JsonArray getActiveCertificaesFromPacbot(String bearerToken,Integer from, Integer size) {
		    	JsonArray responseArray;
		    	JsonArray validCertArray = new JsonArray();
		    	JsonParser jsonParser = new JsonParser();
		    	String certManagerAPI = pacbotGetCertEndpoint;
		    	PacbotCertRequest pacbotCertRequest = new PacbotCertRequest();
		    	FilterDetails filterObj = new FilterDetails();
		    	
		    	HttpClient httpClient;
		        try {
		            httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
		                    NoopHostnameVerifier.INSTANCE).
		                    setSSLContext(
		                            new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
		                                @Override
		                                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		                                    return true;
		                                }
		                            }).build()
		                    ).setRedirectStrategy(new LaxRedirectStrategy()).build();
		        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
		                    put(LogMessage.MESSAGE, "Failed to get certificates. Failed to create httpClient").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		            return null;
		        }
				try {
					HttpPost postRequest = new HttpPost(certManagerAPI);
				        postRequest.addHeader("Authorization", "Bearer "+ bearerToken);
				        postRequest.addHeader("Content-type", "application/json");
				        postRequest.addHeader("Accept","application/json");
				        		        
				        ObjectMapper obj = new ObjectMapper(); 
				        pacbotCertRequest.setAg("aws-all");
				        filterObj.setResourceType("cert");
				        pacbotCertRequest.setFilterDetails(filterObj);
				        pacbotCertRequest.setFrom(from==null?0:from);
				        pacbotCertRequest.setSize(size==null?0:size);
				        String json = obj.writeValueAsString(pacbotCertRequest);
				        json = json.replaceAll("filterDetails","filter");

				        
				        
				        StringEntity entity = new StringEntity(json);
				        postRequest.setEntity(entity);		        
				        HttpResponse apiResponse = httpClient.execute(postRequest);

			            if ((!ObjectUtils.isEmpty(apiResponse)) && apiResponse.getStatusLine().getStatusCode() != 200) {
			            	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			        				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			        				put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
			        				put(LogMessage.MESSAGE,"Failed to get certificate list from pacbot.").
			        				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			        				build()));
			                return null;
			            }
			            else {
			            String responseStr =  EntityUtils.toString(apiResponse.getEntity());
			            if(!StringUtils.isEmpty(responseStr)) {
			            JsonObject res = jsonParser.parse(responseStr).getAsJsonObject();
			            responseArray= res.getAsJsonObject("data").getAsJsonArray("response");
			            validCertArray = getValidcertificateDetails(responseArray);
			            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			    				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			    				put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
			    				put(LogMessage.MESSAGE,String.format("Fetching [%s] certificates list from pacbot completed. List contains [%s] valid certificates",
			    						responseArray.size(),validCertArray.size())).
			    				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			    				build()));
			            }}
				
				} catch (Exception e) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, "getActiveCertificaesFromPacbot").
		                    put(LogMessage.MESSAGE, "Failed to get certificate list from pacbot.").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		            return null;
				}
				return validCertArray;
		    }
		    
		    /**
		     * Method to filter out the invalid certificates
		     * @param certArray
		     * @return
		     */
		    private JsonArray getValidcertificateDetails(JsonArray certArray) {
		    	 JsonObject jsonObject;
		    	 JsonArray validCertArray = new JsonArray();
		         for (int i = 0; i < certArray.size(); i++) {
		             jsonObject = certArray.get(i).getAsJsonObject();
		             if(jsonObject.get("certificateStatus").getAsString().equalsIgnoreCase("Active") &&
//		            		 (!(jsonObject.get(SSLCertificateConstants.COMMON_NAME).getAsString().toUpperCase().startsWith("CERTTEST"))) &&
		            		 !(jsonObject.get(SSLCertificateConstants.COMMON_NAME).getAsString().startsWith("*")) &&
		            		 ((!StringUtils.isEmpty(jsonObject.get(SSLCertificateConstants.CONTAINER_NAME))) && (jsonObject.get(SSLCertificateConstants.CONTAINER_NAME).getAsString().equalsIgnoreCase(container_name)) )) {
		            	 validCertArray.add(certArray.get(i));
		             }
		         }
		         return validCertArray;
		    }
		    
		       
		    private ResponseEntity<String> createObjectAndOnboardCert(JsonObject certObject,UserDetails userDetails) {
		    	ResponseEntity<String> response;
		    	SSLCertificateRequest sslCertificateRequest = new SSLCertificateRequest();		    	
		    	String certificateName = ObjectUtils.isEmpty(certObject.get(SSLCertificateConstants.COMMON_NAME))?"":certObject.get(SSLCertificateConstants.COMMON_NAME).getAsString();
		    	String containerPath = ObjectUtils.isEmpty(certObject.get("containerPath"))?"":certObject.get("containerPath").getAsString();
		    	int containerId = getContainerIdForOnboard(containerPath);
		    	
		    	 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                 put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                 put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
		                 put(LogMessage.MESSAGE, String.format("[%s] Onbaord certificate - STARTED ", certificateName)).
		                 put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                 build()));		    	 
		    	if(container_name.equalsIgnoreCase("T-Vault-Test")) {
		    		certObject.addProperty("app.ou",certObject.get("app.ou")==null?"tvt":certObject.get("app.ou").getAsString());
		    	}
		    	 String appNameStr = certObject.get("app.ou")==null?"":certObject.get("app.ou").getAsString();
		    	 if(!StringUtils.isEmpty(appNameStr)) {
		    	 String[] appNames = Arrays.stream(appNameStr.split(","))
		 		        .map(String::trim)
		 		        .toArray(String[]::new);
		    	 sslCertificateRequest.setAppName(appNames[0]);   	 
		    	 }
		    	 String tagsOwner = certObject.get("tags.Owner")==null?"":certObject.get("tags.Owner").getAsString();
		    	sslCertificateRequest.setCertificateName(certificateName);
		    	sslCertificateRequest.setCertType(ObjectUtils.isEmpty(certObject.get(SSLCertificateConstants.CERT_TYPE))?"":certObject.get(SSLCertificateConstants.CERT_TYPE).getAsString().toLowerCase());		    	
		    	response = onboardCertificate(sslCertificateRequest,userDetails,containerId,tagsOwner);
		    	return response;
		    }
		    
		    
		    /**
		     * Method to Create policies and metadata for the given certificate
		     * @param certObject
		     */
		    public ResponseEntity<String> onboardCertificate(SSLCertificateRequest sslCertificateRequest,UserDetails userDetails, int containerId, String tagsOwner) {
		    	
		    	if(StringUtils.isEmpty(sslCertificateRequest.getAppName())) {
		    		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		 					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		 					put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
		 					put(LogMessage.MESSAGE, String.format("[%s] - Certificate onboard Failed. Application Name is not available. ", sslCertificateRequest.getCertificateName())).
		 					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		 					build()));
		    		 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		    					.body(ERRORS + "Application name unavailable. " + "\"]}");
		    	}
		    	
		    	try {
		    		ResponseEntity<String> metadataResponse = createCertMetadataAndPolicy(sslCertificateRequest,userDetails,containerId, tagsOwner);
		    		if(!HttpStatus.OK.equals(metadataResponse.getStatusCode())){
		    			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		    					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		    					put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
		    					put(LogMessage.MESSAGE, String.format("[%s] - Certficate metadata creation failed.  ", sslCertificateRequest.getCertificateName())).
		    					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		    					build()));
		    			return ResponseEntity.status(HttpStatus.FORBIDDEN)
		    					.body(ERRORS + "Certficate metadata creation failed. " + "\"]}");
		    		}
		    		else if(HttpStatus.OK.equals(metadataResponse.getStatusCode())) {
		    			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		    					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		    					put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
		    					put(LogMessage.MESSAGE, String.format("[%s] - Certficate onboarding is successfully - completed.  ", sslCertificateRequest.getCertificateName())).
		    					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		    					build()));
		    			
		    		}
				} catch (Exception tex) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
		                    put(LogMessage.ACTION, String.format(" Onboardcertificate " +
		                                    "Exception = [%s] =  Message [%s] = certificate name = [%s]",
		                            Arrays.toString(tex.getStackTrace()), tex.getMessage(),sslCertificateRequest.getCertificateName())).build()));
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
							.body(ERRORS + "Certificate onboard failed. " + "\"]}");
				}
		    	return ResponseEntity.status(HttpStatus.OK)
						.body(MESSAGES + "Certficate onboarding is successfully completed. " + "\"]}");
		    }
		    
		    
		    /**
		     * This Method used to get the container id
		     * @param sslCertificateRequest
		     * @return
		     */
		    private int getContainerIdForOnboard(String containerPath){
		        int containerId=0;
		        if(!StringUtils.isEmpty(containerPath)) {
		        	
		        String[] splittedStr = Arrays.stream(containerPath.split(">"))
		        .map(String::trim)
		        .toArray(String[]::new);
		        int size = splittedStr.length;
		        if(splittedStr[size-1].equals("Private Certificates")) {
		        	containerId =getTargetSystemGroupIdForDev(SSLCertType.valueOf(SSLCertificateConstants.PRIVATE_SINGLE_SAN));
		        }
		        else if(splittedStr[size-2].equals("Entrust Single SAN")) {
		        	containerId =getTargetSystemGroupIdForDev(SSLCertType.valueOf(SSLCertificateConstants.PUBLIC_SINGLE_SAN));
		        }
		        else if(splittedStr[size-2].equals("Entrust Multiple SANs")) {
		        	containerId =getTargetSystemGroupIdForDev(SSLCertType.valueOf("PUBLIC_MULTI_SAN"));
		        }
		        }
		        return containerId;
		    }
		    		   
		    
		    /**
		     * Method to create the metadata and policy for a certificate
		     * @param sslCertificateRequest
		     * @param userDetails
		     * @param containerId
		     * @return
		     */
		    private ResponseEntity<String> createCertMetadataAndPolicy(SSLCertificateRequest sslCertificateRequest, UserDetails userDetails, int containerId, String tagsOwner) {
		    	
		    	// Policy Creation
				boolean isPoliciesCreated = false;
				CertResponse enrollResponse = new CertResponse();
				ResponseEntity<String> permissionResponse ;
				
				boolean isDeleted =false;
				try {
					String metadataJson = populateSSLCertificateMetadataForOnboard(sslCertificateRequest, userDetails,containerId, tagsOwner);
					if(metadataJson == null) {
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate onboard failed.\"]}");
					}

					boolean sslMetaDataCreationStatus;

					sslMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson,tokenUtils.getSelfServiceToken());

					 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
			                    put(LogMessage.ACTION, String.format(" [%s] - Metadata creation status - [%s] for metadatajson - [%s]",
			                            sslCertificateRequest.getCertificateName(),sslMetaDataCreationStatus,metadataJson)).
			                    build()));

					 JsonParser jsonParser = new JsonParser();
				        JsonObject object = ((JsonObject) jsonParser.parse(metadataJson)).getAsJsonObject("data");
						if(object.get(SSLCertificateConstants.CERT_TYPE).getAsString().equalsIgnoreCase("external")) {

							String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ? SSLCertificateConstants.SSL_CERT_PATH
									: SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
							String certificatePath = metaDataPath + '/' + sslCertificateRequest.getCertificateName();
							SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(tokenUtils.getSelfServiceToken(),
									sslCertificateRequest.getCertificateName(), sslCertificateRequest.getCertType());
							String nclmAccessToken = getNclmToken();
							if (StringUtils.isEmpty(nclmAccessToken)) {
								return null;
							}
							CertificateData certificateData = getLatestCertificateFromNCLM(sslCertificateRequest.getCertificateName(), nclmAccessToken, containerId);

							if(!ObjectUtils.isEmpty(certificateData)) {
								 processCertificateDataAndUpdateMetadata(certificatePath, tokenUtils.getSelfServiceToken(), certificateMetaData,
										certificateData);
						}
						}

		        if (sslMetaDataCreationStatus) {
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
		                    put(LogMessage.ACTION, String.format(" [%s] - Metadata Creation - Completed ",
		                            sslCertificateRequest.getCertificateName())).
		                    build()));
		            isPoliciesCreated = createPolicies(sslCertificateRequest, tokenUtils.getSelfServiceToken());
		        }


		        if(isPoliciesCreated) {
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
		                    put(LogMessage.ACTION, String.format(" [%s] - Policycreation - Completed",
		                            sslCertificateRequest.getCertificateName())).
		                    build()));
		        }
		        else {
		        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER,
									ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION,
									String.format(
											" ERROR [%s] - Onboard failed. Policy creation failed . policyStatus[%s]",
											sslCertificateRequest.getCertificateName(), isPoliciesCreated))	.build()));
		            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS+enrollResponse.getResponse()+"\"]}");
		        }
		        
		        boolean sslApplicationMetaDataSaveStatus;
                //save certificate name into application metadata path
                	sslApplicationMetaDataSaveStatus = certificateMetadataForApplicationDetails(metadataJson, tokenUtils.getSelfServiceToken(), "create");
				
                
                if (sslApplicationMetaDataSaveStatus) {
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("Certificate details added to Application Metadata for SSL certificate name [%s]",
                                    sslCertificateRequest.getCertificateName())).
                            build()));
                }


		        //Send failed certificate response in case of any issues in Policy/Meta data creation
		        if ((!isPoliciesCreated) || (!sslMetaDataCreationStatus)) {
		            enrollResponse.setResponse("Metadatacreation failed");
		            enrollResponse.setSuccess(Boolean.FALSE);
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER,
									ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION,
									String.format(
											"ERROR [%s] - Onboard failed. Metadata creation failed - metaDataStatus[%s] - policyStatus[%s]",
											sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus,
											isPoliciesCreated))
							.build()));
		            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS+enrollResponse.getResponse()+"\"]}");
		        } else {		        	
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE).
		                    put(LogMessage.MESSAGE, "Sudo Policy Creation - Started  ").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		            permissionResponse =  addSudoPermissionToCertificateOwner(sslCertificateRequest, userDetails, enrollResponse
		                    , isPoliciesCreated, sslMetaDataCreationStatus,tokenUtils.getSelfServiceToken(),SSLCertificateConstants.ONBOARD);
		            if (permissionResponse !=null && !(HttpStatus.OK.equals(permissionResponse.getStatusCode()))) {
		            	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			                    put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE).
			                    put(LogMessage.MESSAGE, String.format(" [%s] - ERROR - Sudo Policy Creation - Failed  ",sslCertificateRequest.getCertificateName())).
			                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			                    build()));
		            	SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(tokenUtils.getSelfServiceToken(),
		            			sslCertificateRequest.getCertificateName(), sslCertificateRequest.getCertType());
		        		String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) ? SSLCertificateConstants.SSL_CERT_PATH
		        				: SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
		        		String certificatePath = metaDataPath + '/' + sslCertificateRequest.getCertificateName();
		        		isDeleted = deleteMetaDataAndPermissions(certificateMetaData, certificatePath, tokenUtils.getSelfServiceToken());
		        		if(isDeleted) {
		        			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				                    put(LogMessage.ACTION, SSLCertificateConstants.GENERATE_SSL_CERTIFICTAE).
				                    put(LogMessage.MESSAGE, String.format(" [%s] - ERROR - Metadata and policy deletion - Completed  ",sslCertificateRequest.getCertificateName())).
				                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				                    build()));
		        		}
		            }
		            return permissionResponse;
		        }
		    	} catch (Exception e) {
		    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS+enrollResponse.getResponse()+"\"]}");
				}
		    }
		    
		    
		    
		    /**
			 * Method to populate certificate metadata details
			 * @param sslCertificateRequest
			 * @param userDetails
			 * @param certManagerLogin
			 * @return
			 * @throws Exception
			 */
			private String populateSSLCertificateMetadataForOnboard(SSLCertificateRequest sslCertificateRequest, UserDetails userDetails, int containerId, String tagsOwner) throws Exception {
				
				String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
			            SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
			    String projectLeadEmail = "";
			    String certOwnerNtId = "";
			    String displayName = "";
			    String certMetadataPath = metaDataPath + '/' + sslCertificateRequest.getCertificateName();		

		        SSLCertificateMetadataDetails sslCertificateMetadataDetails = new SSLCertificateMetadataDetails();

		        //Get Application details
		        String applicationName = sslCertificateRequest.getAppName();
		        if(StringUtils.isEmpty(applicationName)) {
		        	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, "populateSSLCertificateMetadataForOnboard ").
		                    put(LogMessage.MESSAGE, String.format("ERROR -[%s]- Application Name is not available in the certificate details. ",
		                    		sslCertificateRequest.getCertificateName())).
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		        }else {        	
		        ResponseEntity<String> appResponse = workloadDetailsService.getWorkloadDetailsByAppName(applicationName);
		        if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
		            JsonParser jsonParser = new JsonParser();
		            JsonObject response = (JsonObject) jsonParser.parse(appResponse.getBody());
		        JsonObject jsonElement = null;
		        if (Objects.nonNull(response)) {
		            jsonElement = response.get("spec").getAsJsonObject();
		            if (Objects.nonNull(jsonElement)) {
		                String applicationTag = validateString(jsonElement.get("tag"));
		                
		                projectLeadEmail = validateString(jsonElement.get("projectLeadEmail"));		
		              
		                if(projectLeadEmail!= null && !StringUtils.isEmpty(projectLeadEmail)) {
		   		    	 String[] projectLeadEmails = Arrays.stream(projectLeadEmail.split(","))
		   		 		        .map(String::trim)
		   		 		        .toArray(String[]::new);
		   		    	sslCertificateMetadataDetails.setProjectLeadEmailId(projectLeadEmails[0]);
		   		    	 }else {
		   		    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				                    put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_ONBOARD_METADATA).
				                    put(LogMessage.MESSAGE, String.format("Project lead email id is not available for given " +
				                            "certificate= [%s]", sslCertificateRequest.getCertificateName())).
				                    build()));
				            return null;
		   		    	 }
		                
		                String appOwnerEmail = validateString(jsonElement.get("brtContactEmail"));
		                String akmid = validateString(jsonElement.get("akmid"));
		                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                        put(LogMessage.ACTION,"Populate Application details in SSL Certificate Metadata").
		                        put(LogMessage.MESSAGE, String.format("Application Details  for an " +
		                                        "applicationName = [%s] , applicationTag = [%s], " +
		                                        "projectLeadEmail =  [%s],appOwnerEmail =  [%s], akmid = [%s]", applicationName,
		                                applicationTag, projectLeadEmail, appOwnerEmail, akmid)).build()));

		                if(!StringUtils.isEmpty(tagsOwner)) {
		                    tagsOwner = tagsOwner.replace(";", ",");
		                    }		                
		                sslCertificateMetadataDetails.setAkmid(akmid);
		                sslCertificateMetadataDetails.setApplicationOwnerEmailId(appOwnerEmail);
		                sslCertificateMetadataDetails.setApplicationTag(applicationTag);
		                sslCertificateMetadataDetails.setApplicationName(applicationName);
		                sslCertificateMetadataDetails.setNotificationEmails(tagsOwner);
		            }
		            }
		        } else {
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, "Getting Application Details by app name during Meta data creation ").
		                    put(LogMessage.MESSAGE, String.format("Application details will not insert/update in metadata  " +
		                                    "for an application =  [%s] ",  applicationName)).
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
		        }}

		        
		        CertificateData certDetails = null;
		        String nclmAccessToken = getNclmToken();	
		                
		        //Get Certificate Details
				if(!StringUtils.isEmpty(nclmAccessToken)) {		                 
		            
		            certDetails = getLatestCertificateFromNCLM(sslCertificateRequest.getCertificateName(), nclmAccessToken, containerId);
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_ONBOARD_METADATA).
		                    put(LogMessage.MESSAGE, String.format("Fetching certificate details  status = [%s]"
		                            , Objects.nonNull(certDetails))).build()));
		         
		        }
		        if (Objects.nonNull(certDetails) && certDetails.getCertificateId()>0) {
		            sslCertificateMetadataDetails.setCertificateId(certDetails.getCertificateId());
		            sslCertificateMetadataDetails.setCertificateName(certDetails.getCertificateName());
		            sslCertificateMetadataDetails.setCreateDate(certDetails.getCreateDate());
		            sslCertificateMetadataDetails.setExpiryDate(certDetails.getExpiryDate());
		            sslCertificateMetadataDetails.setAuthority(certDetails.getAuthority());
		            sslCertificateMetadataDetails.setCertificateStatus(certDetails.getCertificateStatus());
		            sslCertificateMetadataDetails.setContainerName(certDetails.getContainerName());
		            sslCertificateMetadataDetails.setDnsNames(certDetails.getDnsNames());
		            if(!sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
		            	sslCertificateMetadataDetails.setCertType("external");
		            	sslCertificateRequest.setCertType("external");

		            }else {
		            	sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
		            	sslCertificateMetadataDetails.setDnsNames(certDetails.getDnsNames());
		            }

		        } else {
		            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_ONBOARD_METADATA).
		                    put(LogMessage.MESSAGE, String.format("Certificate Details is not available in NCLM for given " +
		                            "certificate= [%s]", sslCertificateRequest.getCertificateName())).
		                    build()));
		            return null;
		        }
		        
		        ResponseEntity<DirectoryObjects> userResponse = directoryService.searchByUPNInGsmAndCorp(projectLeadEmail);
		        Object[] users = null;
		    	DirectoryUser dirUser;
		    	if(userResponse.getStatusCode().equals(HttpStatus.OK)) {
		    		 users = userResponse.getBody().getData().getValues();
		    		 if(!ObjectUtils.isEmpty(users)) {
		    		 dirUser = (DirectoryUser) users[0];
		    		 certOwnerNtId = dirUser.getUserName();
		    		 }
		    	}  
			        sslCertificateMetadataDetails.setCertCreatedBy(certOwnerNtId);
			        sslCertificateMetadataDetails.setCertOwnerEmailId(projectLeadEmail);
			        sslCertificateMetadataDetails.setCertOwnerNtid(certOwnerNtId);
			        sslCertificateMetadataDetails.setContainerId(containerId);
			        sslCertificateMetadataDetails.setOnboardFlag(Boolean.TRUE);
			        
			        sslCertificateRequest.setCertOwnerEmailId(projectLeadEmail);
			        sslCertificateRequest.setCertOwnerNtid(certOwnerNtId);

		        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                put(LogMessage.ACTION, String.format("MetaData info details = [%s]", sslCertificateMetadataDetails.toString())).
		                build()));     


		        SSLCertMetadata sslCertMetadata = new SSLCertMetadata(certMetadataPath, sslCertificateMetadataDetails);
		        String jsonStr = JSONUtil.getJSON(sslCertMetadata);
		        Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		        rqstParams.put("path", certMetadataPath);
		        return ControllerUtil.convetToJson(rqstParams);
			}
			
			/**
			 * Method to get the latesst certificate from NCLM
			 * @param sslCertificateRequest
			 * @param certManagerLogin
			 * @return
			 * @throws Exception
			 */
			   private CertificateData getLatestCertificateFromNCLM(String certName, String nclmAccessToken, int containerId) throws Exception {
			        CertificateData certificateData=null;
			        String findCertificateEndpoint = "/certmanager/findCertificate";
			        String targetEndpoint = findCertificate.replace("certname", String.valueOf(certName)).replace("cid", String.valueOf(containerId));
			        CertResponse response = reqProcessor.processCert(findCertificateEndpoint, "", nclmAccessToken, getCertmanagerEndPoint(targetEndpoint));
			        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
			        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.CERTIFICATES) != null)) {
			            JsonParser jsonParser = new JsonParser();
			            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
			            if (jsonObject != null) {
			                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
			                certificateData = setLatestCertificateFromNCLM(certificateData, certName, jsonArray);
			            }
			        }
			         return certificateData;
			    }
			      
			   
			   /**
				 * @param certificateData
				 * @param certName
				 * @param jsonArray
				 * @return
				 */
				private CertificateData setLatestCertificateFromNCLM(CertificateData certificateData, String certName,
						JsonArray jsonArray) {
					LocalDateTime  createdDate = null ;
		            LocalDateTime  certCreatedDate;
		            JsonArray jsonArrayvalid = new JsonArray();
					for (int i = 0; i < jsonArray.size(); i++) {
					    JsonObject jsonElements = jsonArray.get(i).getAsJsonObject();
					    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			                    put(LogMessage.ACTION, "setLatestCertificateFromNCLM").
			                    put(LogMessage.MESSAGE, String.format("Certificate name from NCLM - [%s] and " +
			                            "certificate name from pacbot - [%s]", jsonElements.get("sortedSubjectName").getAsString(),certName)).
			                    build()));

					    if ((Objects.equals(getCertficateName(jsonElements.get("sortedSubjectName").getAsString()), certName))
					            && jsonElements.get(SSLCertificateConstants.CERTIFICATE_STATUS).getAsString().
					            equalsIgnoreCase(SSLCertificateConstants.ACTIVE)) {
					    	jsonArrayvalid.add(jsonElements);
					    }
					}
					String notBeforeDate = null;
					    	for (int j = 0; j < jsonArrayvalid.size(); j++) {
								JsonObject jsonElement = jsonArrayvalid.get(j).getAsJsonObject();
								if (j == 0) {
									notBeforeDate = validateString(jsonElement.get("NotBefore"));
									createdDate = notBeforeDate != null ? LocalDateTime.parse(notBeforeDate.substring(0, 19)) : null;
								} else if (j > 0) {
									if (jsonArrayvalid.get(j - 1).getAsJsonObject()
											.get(SSLCertificateConstants.CERTIFICATE_STATUS).getAsString()
											.equalsIgnoreCase(SSLCertificateConstants.ACTIVE)) {
										notBeforeDate = validateString(jsonArrayvalid.get(j - 1).getAsJsonObject().get("NotBefore"));
									} else {
										notBeforeDate = validateString(jsonArrayvalid.get(j).getAsJsonObject().get("NotBefore"));
									}
									createdDate = notBeforeDate != null ? LocalDateTime.parse(notBeforeDate.substring(0, 19)) : null;
								}
								notBeforeDate = validateString(jsonElement.get("NotBefore"));
								certCreatedDate = notBeforeDate != null ? LocalDateTime.parse(notBeforeDate.substring(0, 19)) : null;

								if (createdDate != null && !ObjectUtils.isEmpty(createdDate)
										&& (createdDate.isBefore(certCreatedDate)
												|| createdDate.isEqual(certCreatedDate))) {
									certificateData = new CertificateData();
					        certificateData.setCertificateId(Integer.parseInt(jsonElement.get(SSLCertificateConstants.CERTIFICATE_ID).getAsString()));

					        certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
					        certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));
					        certificateData.setContainerName(validateString(jsonElement.get(SSLCertificateConstants.CONTAINER_NAME)));
					        certificateData.setCertificateStatus(validateString(jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS)));
					        certificateData.setCertificateName(certName);
					        certificateData.setAuthority((!StringUtils.isEmpty(jsonElement.get("enrollServiceInfo")) ?
					                 validateString(jsonElement.get("enrollServiceInfo").getAsJsonObject().get("name")) :
					                 null));
							certificateData.setPreviousCertId((!ObjectUtils.isEmpty(jsonElement.get("previous")) ? 
									Integer.parseInt(jsonElement.get("previous").getAsString()) : null));
							if(Objects.nonNull(jsonElement.getAsJsonObject("subjectAltName"))){
			                    JsonObject subjectAltNameObject = jsonElement.getAsJsonObject("subjectAltName");
			                    JsonArray jsonArr = subjectAltNameObject.getAsJsonArray("dns");
			                    List<String> list = new ArrayList<>();
			                    if(jsonArr!=null && jsonArr.size()>0) {
				                    for(int index=0; index < jsonArr.size(); index++) {
				                        list.add(jsonArr.get(index).getAsString());
				                    }
				                    certificateData.setDnsNames(list);
								}
					                }
					        break;
					    }
					    }



					return certificateData;
				}
				
				
				/**
			     * API to onboard single certificate to tvault
			     * @param userDetails
			     * @param bearerToken
			     * @return
			     * @throws Exception 
			     */
			    public ResponseEntity<String> onboardSingleCert(UserDetails userDetails,  String token, String certType, 
			    									String commonname,  String appName) throws Exception {
			    	
			    	SSLCertificateRequest sslCertificateRequest = new SSLCertificateRequest();
			    	ResponseEntity<String> response;
			    	JsonObject jsonObject = new JsonObject();;
			    	jsonObject.addProperty(SSLCertificateConstants.COMMON_NAME,commonname);
			    	jsonObject.addProperty(SSLCertificateConstants.CERT_TYPE,certType);
			    	
			    	if (!isValidInputs(commonname, certType)) {
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
								.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
					}
			    	
			    	if(isCertAvailableInMetadata(jsonObject, token)) {
			    		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
								.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
								.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
								.put(LogMessage.MESSAGE, "Certificate already available in metadata.")
								.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Certificate already available in tvault\"]}");
			    	}
			    	
			    	int containerId=0;
			    	List<Integer> containerList = new ArrayList<>();
			    	sslCertificateRequest.setCertificateName(commonname);
			    	sslCertificateRequest.setCertType(certType);
			    	sslCertificateRequest.setAppName(appName);

			    	if(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
			    		containerList.add(private_single_san_ts_gp_id_test!=0?private_single_san_ts_gp_id_test:private_single_san_ts_gp_id);
			    	}
			    	
			    	else {
			    		CertificateData certDetailsExtSsan = null;
			    		CertificateData certDetailsExtMsan = null;
				        String nclmAccessToken = getNclmToken();
				        int containerIdExtSsan=public_single_san_ts_gp_id_test!=0?public_single_san_ts_gp_id_test:public_single_san_ts_gp_id;
				        int containerIdExtMsan=public_multi_san_ts_gp_id_test!=0?public_multi_san_ts_gp_id_test:public_multi_san_ts_gp_id;
						certDetailsExtSsan = getLatestCertificateFromNCLM(commonname, nclmAccessToken,
								containerIdExtSsan);
						if (certDetailsExtSsan != null && (!ObjectUtils.isEmpty(certDetailsExtSsan))
								&& (!ObjectUtils.isEmpty(certDetailsExtSsan.getCertificateName()))) {
							containerList.add(containerIdExtSsan);
						}
						certDetailsExtMsan = getLatestCertificateFromNCLM(commonname, nclmAccessToken,
								containerIdExtMsan);
						if (certDetailsExtMsan != null && (!ObjectUtils.isEmpty(certDetailsExtMsan))
								&& (!ObjectUtils.isEmpty(certDetailsExtMsan.getCertificateName()))) {
							containerList.add(containerIdExtMsan);
						}
			    		
			    	}
			    	if(containerList.size() >0) {
			    		for(int i=0; i<containerList.size();i++) {
			    	response = onboardCertificate(sslCertificateRequest,userDetails,containerList.get(i),"");
			    	
			    	if (response !=null && !HttpStatus.OK.equals(response.getStatusCode())) {
			    		log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
								.put(LogMessage.MESSAGE,String.format(
												"Onboarding failed for certificate name [%s]", commonname ))
								.build()));
			    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate onboard failed\"]}");
			    	}
			    	
			    	else {
			    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			                    put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).
			                    put(LogMessage.MESSAGE, String.format("Onbaording successful for certificate name [%s]", commonname )).
			                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			                    build()));
			    	}
			    		}
			    	}else {
			    		log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "onboardSingleCert")
								.put(LogMessage.MESSAGE,String.format(
												"Onboarding failed for certificate name [%s]. Certificate not available in NCLM.", commonname ))
								.build()));
			    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate onboard failed\"]}");
			    	}
			    	return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"ssl certificate onboard successfully\"]}");
			    }
			    
			    /**
			     * Get the target System Group ID
			     * @param sslCertType
			     * @return
			     */
			    private int getTargetSystemGroupIdForDev(SSLCertType sslCertType) {
			        int ts_gp_id = private_single_san_ts_gp_id_test;
			        switch (sslCertType) {
			            case PRIVATE_SINGLE_SAN:
			                ts_gp_id = private_single_san_ts_gp_id_test!=0?private_single_san_ts_gp_id_test:private_single_san_ts_gp_id;
			                break;
			            case PUBLIC_SINGLE_SAN:
			                ts_gp_id = public_single_san_ts_gp_id_test!=0?public_single_san_ts_gp_id_test:public_single_san_ts_gp_id; //2276
			                break;
			            case PUBLIC_MULTI_SAN:
			                ts_gp_id = public_multi_san_ts_gp_id_test!=0?public_multi_san_ts_gp_id_test:public_multi_san_ts_gp_id; //2277
			                break;
			        }
			        return ts_gp_id;
			    }

	public ResponseEntity<String> deleteApproleFromCertificate(CertificateApprole certificateApprole,
			UserDetails userDetails) {
		String authToken = null;
        boolean isAuthorized = true;
        if(!areCertificateApproleInputsValid(certificateApprole)) {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
        			put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
        		   	put(LogMessage.ACTION, SSLCertificateConstants.DELETE_APPROLE_TO_CERT_MSG).
        		   	put(LogMessage.MESSAGE, "Invalid input values").
        		   	put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
        		   	build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
        }

        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, SSLCertificateConstants.DELETE_APPROLE_TO_CERT_MSG).
                put(LogMessage.MESSAGE, String.format("Trying to delete Approle to Certificate - Request [%s]", certificateApprole.toString())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String approleName = certificateApprole.getApproleName().toLowerCase();
        String certificateName = certificateApprole.getCertificateName().toLowerCase();
        String access = certificateApprole.getAccess().toLowerCase();
        String certType = certificateApprole.getCertType().toLowerCase();
        if (approleName.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
        	
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.DELETE_APPROLE_TO_CERT_MSG)
					.put(LogMessage.MESSAGE,
							String.format("No permission to associate this AppRole [%s] to any Certificate",
									approleName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Certificate\"]}");
        }
        if (!ObjectUtils.isEmpty(userDetails)) {

	        if (userDetails.isAdmin()) {
	        	authToken = userDetails.getClientToken();
	        }else {
	        	authToken = userDetails.getSelfSupportToken();
	        }
	        SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certType);

			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);

        }else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.DELETE_APPROLE_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to delete approle from this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));

   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to delete approle from this certificate\"]}");
   		}

		if(isAuthorized){
        	return checkPolicyDetailsAndRemoveApproleFromCertificate(authToken, approleName, certificateName, access, certType);
        } else{
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.DELETE_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Access denied: No permission to delete Approle [%s] from the Certificate [%s]", approleName, certificateName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to delete Approle from this Certificate\"]}");
        }
	}

	private ResponseEntity<String> checkPolicyDetailsAndRemoveApproleFromCertificate(String authToken,
			String approleName, String certificateName, String access, String certType) {
String policyPrefix = getCertificatePolicyPrefix(access, certType);

		String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

		String certificatePath = metaDataPath + certificateName;

		if(TVaultConstants.EMPTY.equals(policyPrefix)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Incorrect access requested. Valid values are read, write, deny").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,deny \"]}");
		}

		String policy = policyPrefix + certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String certPrefix=(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;

		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Approle Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approleName+"\"}",authToken);

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
		        put(LogMessage.MESSAGE, String.format("roleResponse status is [%s]", roleResponse.getHttpstatus())).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
			responseJson = roleResponse.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
				if (null != policiesArry) {
					for (JsonNode policyNode : policiesArry) {
						currentpolicies.add(policyNode.asText());
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while creating currentpolicies").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);

		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Non existing role name. Please configure approle as first step - Approle = [%s]", approleName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

		    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Either Approle doesn't exists or you don't have enough permission to remove this approle from Certificate\"]}");
		}

		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureApprole", policies)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response approleControllerResp = appRoleService.configureApprole(approleName,policiesString,authToken);
		if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT) || approleControllerResp.getHttpstatus().equals(HttpStatus.OK)){
			Map<String,String> params = new HashMap<>();
			params.put("type", "app-roles");
			params.put("name",approleName);
			params.put("path",certificatePath);
			params.put("access","delete");
			Response metadataResponse = ControllerUtil.updateMetadata(params, authToken);
			if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
						put(LogMessage.MESSAGE, String.format("Approle [%s] successfully deleted from Certificate [%s]", approleName, certificatePath)).
						put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle is successfully removed(if existed) from Certificate\"]}");
			}
		 approleControllerResp = appRoleService.configureApprole(approleName,policiesString,authToken);
		 if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Remove AppRole from Certificate").
						put(LogMessage.MESSAGE, "Reverting, approle policy update success").
						put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
						put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
			}else{
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Remove AppRole from Certificate").
						put(LogMessage.MESSAGE, "Reverting approle policy update failed").
						put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
						put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin \"]}");
			}
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to remove approle from SSL Certificate\"]}");
		}
		
	}


			    public ResponseEntity<String> addUserToCertificateOnboard(CertificateUser certificateUser, UserDetails userDetails, boolean addSudoPermission) {
			   		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			   				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			   				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
			   				put(LogMessage.MESSAGE, "Trying to add user to Certificate folder ").
			   				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			   				build()));


			   		String userName = certificateUser.getUsername().toLowerCase();
			   		String certificateName = certificateUser.getCertificateName().toLowerCase();
			   		String access = certificateUser.getAccess().toLowerCase();
			   		String certificateType = certificateUser.getCertType();
			   		String authToken = null;

			   		boolean isAuthorized = true;
			   		if (!ObjectUtils.isEmpty(userDetails)) {

			   	        	authToken = userDetails.getSelfSupportToken();
			   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, certificateType);

			   			if(!addSudoPermission){
			   				isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
			   			}

			   			if((!addSudoPermission) && (isAuthorized) && (userName.equalsIgnoreCase(certificateMetaData.getCertOwnerNtid()))) {
			   				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			   	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			   	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
			   	   					put(LogMessage.MESSAGE, "Certificate owner cannot be added as a user to the certificate owned by him").
			   	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			   	   					build()));

			   				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate owner cannot be added as a user to the certificate owned by him\"]}");
			   			}
			   		}else {
			   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
			   					put(LogMessage.MESSAGE, "Access denied: No permission to add users to this certificate").
			   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			   					build()));

			   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
			   		}

			   		if(isAuthorized){
			   			return checkUserDetailsAndAddCertificateToUser(authToken, userName, certificateName, access, certificateType, userDetails);
			   		}else{
			   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
			   					put(LogMessage.MESSAGE, "Access denied: No permission to add users to this certificate").
			   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			   					build()));

			   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
			   		}
			   	}


	/**
	 * API to get all pending certificates to onboard
	 *
	 * @param userDetails
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public ResponseEntity<String> getAllOnboardPendingCertificates(String token, UserDetails userDetails, Integer limit, Integer offset)
			throws Exception {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_PENDING_CERT_MSG)
				.put(LogMessage.MESSAGE, "Trying to get all pending certificates from nclm to onboard")
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

		if (ObjectUtils.isEmpty(userDetails) || (!userDetails.isAdmin())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_PENDING_CERT_MSG)
					.put(LogMessage.MESSAGE, "Access denied: No permission to get the pending certificates")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to get the pending certificates\"]}");
		}

		String nclmAccessToken = getNclmToken();
		if (StringUtils.isEmpty(nclmAccessToken)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_PENDING_CERT_MSG)
					.put(LogMessage.MESSAGE, SSLCertificateConstants.NCLM_DOWN_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ERRORS + nclmErrorMessage + "\"]}");
		}
		List<CertificateData> certificatesList = new ArrayList<>();
		String targetEndpointVal = findAllCertificate;
		Map<String, Object> certificateMap = new HashMap<>();
		// Getting all on-boarded internal certificates
		List<String> onboardedInternalCerts = getListOfCertificatesForValidation(token, SSLCertificateConstants.INTERNAL);
		// Getting all on-boarded external certificates
		List<String> onboardedExternalCerts = getListOfCertificatesForValidation(token, SSLCertificateConstants.EXTERNAL);
		getCertificateListFromNclm(nclmAccessToken, certificatesList, targetEndpointVal, onboardedInternalCerts,
				onboardedExternalCerts);

		limit = (limit == null) ? certificatesList.size() : limit;
		offset = (offset == null) ? 0 : offset;

		if (!certificatesList.isEmpty()) {
			Integer totCount = certificatesList.size();
			Integer offsetVal = 0;
			Integer toindex = 0;
			Integer limitVal = offset + limit;

			offsetVal = (offset <= totCount) ? offset : totCount;
			toindex = (limitVal <= totCount) ? limitVal : totCount;

			certificatesList = certificatesList.subList(offsetVal, toindex);

			certificateMap.put("keys", certificatesList);
			certificateMap.put("total", totCount);
			certificateMap.put("next", (totCount - (certificatesList.size()+ offset)>0?totCount - (certificatesList.size() + offset):-1));
		}

		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(certificateMap));
	}

	/**
	 * Method to call NCLM API and get the list of active certificates
	 *
	 * @param nclmAccessToken
	 * @param certificatesList
	 * @param targetEndpointVal
	 * @param onboardedInternalCerts
	 * @param onboardedExternalCerts
	 * @return
	 * @throws Exception
	 */
	private List<CertificateData> getCertificateListFromNclm(String nclmAccessToken,
			List<CertificateData> certificatesList, String targetEndpointVal, List<String> onboardedInternalCerts,
			List<String> onboardedExternalCerts) throws Exception {
		String findAllCertificateEndpoint = "/certmanager/findAllCertificates";
		CertResponse response = reqProcessor.processCert(findAllCertificateEndpoint, "", nclmAccessToken,
				getCertmanagerEndPoint(targetEndpointVal));
		Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
		if (!MapUtils.isEmpty(responseMap) && (responseMap.get(SSLCertificateConstants.CERTIFICATES) != null)) {
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
			if (jsonObject != null) {
				JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
				setAllActiveCertificates(jsonArray, certificatesList, onboardedInternalCerts, onboardedExternalCerts);
				if (responseMap.get("next") != null) {
					String limitVal = responseMap.get("limit").toString();
					String offset = responseMap.get("offset").toString();
					Integer offsetVal = Integer.parseInt(limitVal) + Integer.parseInt(offset);
					String offsetValString = "offset="+offsetVal;
					String nextURL = targetEndpointVal.replace("offset=0", offsetValString);
					certificatesList = getCertificateListFromNclm(nclmAccessToken, certificatesList, nextURL,
							onboardedInternalCerts, onboardedExternalCerts);
				}
			}
		}
		return certificatesList;
	}

	/**
	 * Method to set all active certificates based on the active status and
	 * container name
	 *
	 * @param certificateData
	 * @param certName
	 * @param jsonArray
	 * @return
	 */
	private List<CertificateData> setAllActiveCertificates(JsonArray jsonArray, List<CertificateData> certificatesList,
			List<String> onboardedInternalCerts, List<String> onboardedExternalCerts) {
		try {
			for (int i = 0; i < jsonArray.size(); i++) {
				JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
				String certificateName = null;
				if (!ObjectUtils.isEmpty(jsonElement.get("sortedSubjectName"))) {
					certificateName = getCertficateName(jsonElement.get("sortedSubjectName").getAsString());
				}
				if ((certificateName != null) && (!certificateName.toUpperCase().startsWith("CERTTEST")) && (!certificateName.toUpperCase().startsWith("*.CERTTEST"))) {
					CertificateData certificateData = new CertificateData();
					boolean isOnboarded = false;
					constructCertificateData(jsonElement, certificateData);
					isOnboarded = isCertificateAlreadyOnboarded(onboardedInternalCerts, onboardedExternalCerts,
							certificateData, isOnboarded);
					if (!isOnboarded && !containsCertificateName(certificatesList, certificateName)) {
						certificatesList.add(certificateData);
					}
				}
			}
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_PENDING_CERT_MSG)
					.put(LogMessage.MESSAGE, "Error while setting the active certificates from nclm")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		}
		return certificatesList;
	}

	/**
	 * Method to check whether the certificate already added to the list
	 *
	 * @param certificatesList
	 * @param certName
	 * @return
	 */
	public boolean containsCertificateName(List<CertificateData> certificatesList, String certName) {
		return certificatesList.stream().map(CertificateData::getCertificateName).anyMatch(certName::equalsIgnoreCase);
	}

	/**
	 * Method to check whether the certificate already on-boarded
	 *
	 * @param onboardedInternalCerts
	 * @param onboardedExternalCerts
	 * @param certificateData
	 * @param isOnboarded
	 * @return
	 */
	private boolean isCertificateAlreadyOnboarded(List<String> onboardedInternalCerts,
			List<String> onboardedExternalCerts, CertificateData certificateData, boolean isOnboarded) {
		String certificateName = certificateData.getCertificateName();
		if (certificateData.getCertType().equals(SSLCertificateConstants.INTERNAL)
				&& !CollectionUtils.isEmpty(onboardedInternalCerts)) {
			isOnboarded = onboardedInternalCerts.stream()
					.anyMatch(certificateName::equalsIgnoreCase);
		} else if (certificateData.getCertType().equals(SSLCertificateConstants.EXTERNAL)
				&& !CollectionUtils.isEmpty(onboardedExternalCerts)) {
			isOnboarded = onboardedExternalCerts.stream()
					.anyMatch(certificateName::equalsIgnoreCase);
		}
		return isOnboarded;
	}

	/**
	 * Method to populate CertificateData object
	 *
	 * @param jsonElement
	 * @param certificateData
	 */
	private void constructCertificateData(JsonObject jsonElement, CertificateData certificateData) {
		certificateData.setCertificateId(Integer.parseInt(jsonElement.get(SSLCertificateConstants.CERTIFICATE_ID).getAsString()));
		certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
		certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));
		certificateData.setContainerName(validateString(jsonElement.get(SSLCertificateConstants.CONTAINER_NAME)));
		certificateData
				.setCertificateStatus(validateString(jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS)));
		certificateData.setCertificateName(getCertficateName(jsonElement.get("sortedSubjectName").getAsString()));
		certificateData.setAuthority((!StringUtils.isEmpty(jsonElement.get("enrollServiceInfo"))
				? validateString(jsonElement.get("enrollServiceInfo").getAsJsonObject().get("name"))
				: null));
		certificateData.setPreviousCertId((!ObjectUtils.isEmpty(jsonElement.get("previous"))
				? Integer.parseInt(jsonElement.get("previous").getAsString())
				: null));
		if (Objects.nonNull(jsonElement.getAsJsonObject("subjectAltName"))) {
			JsonObject subjectAltNameObject = jsonElement.getAsJsonObject("subjectAltName");
			JsonArray jsonArr = subjectAltNameObject.getAsJsonArray("dns");
			if (!ObjectUtils.isEmpty(jsonArr)) {
				List<String> list = new ArrayList<>();
				for (int index = 0; index < jsonArr.size(); index++) {
					list.add(jsonArr.get(index).getAsString());
				}
				certificateData.setDnsNames(list);
			}
		}
		setCertificateTypeBasedOnContainerId(jsonElement, certificateData);
	}

	/**
	 * Method to set the certificate type based on the container name and Id
	 *
	 * @param jsonElement
	 * @param certificateData
	 */
	private void setCertificateTypeBasedOnContainerId(JsonObject jsonElement, CertificateData certificateData) {
		if (jsonElement.get("containerPath") != null) {
			JsonArray containerPathArray = jsonElement.getAsJsonArray("containerPath");
			for (int j = 0; j < containerPathArray.size(); j++) {
				JsonObject containerPathElement = containerPathArray.get(j).getAsJsonObject();
				if (containerPathElement.get(SSLCertificateConstants.CONTAINER_NAME) != null && containerPathElement.get(SSLCertificateConstants.CONTAINER_NAME)
						.getAsString().equalsIgnoreCase(SSLCertificateConstants.CERTIFICATE_TYPE_EXTERNAL)) {
					certificateData.setCertType(SSLCertificateConstants.EXTERNAL);
					return;
				}else if (containerPathElement.get(SSLCertificateConstants.CONTAINER_NAME) != null && containerPathElement.get(SSLCertificateConstants.CONTAINER_NAME)
						.getAsString().equalsIgnoreCase(SSLCertificateConstants.CERTIFICATE_TYPE_INTERNAL)) {
					certificateData.setCertType(SSLCertificateConstants.INTERNAL);
					return;
				}
			}
		}
	}

	/**
     * API to Onboard single SSL certificate to TVault
     * @param userDetails
     * @param sslCertificateRequest
     * @return
     * @throws Exception
     */
	public ResponseEntity<String> onboardSSLcertificate(UserDetails userDetails, String token,
			SSLCertificateOnboardRequest sslCertificateRequest) throws Exception {
		if (ObjectUtils.isEmpty(sslCertificateRequest)
				|| !isValidInputs(sslCertificateRequest.getCertificateName(), sslCertificateRequest.getCertType())
				|| (!validateNotificationEmailsForOnboard(sslCertificateRequest.getNotificationEmail()))
				|| (StringUtils.isEmpty(sslCertificateRequest.getNotificationEmail()))) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE).put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		}

		if (ObjectUtils.isEmpty(userDetails) || (!userDetails.isAdmin())) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
					.put(LogMessage.MESSAGE, "Access denied: No permission to onboard certificates")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Access denied: No permission to onboard certificates\"]}");
		}

		boolean isValidAppName = validateApplicationNameForOnboard(sslCertificateRequest);
		if(!isValidAppName) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "onboard SSLcertificate").put(LogMessage.MESSAGE, "Invalid application name")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid app name\"]}");
		}

		boolean isValidUser = validateOwnerEmailForOnboard(sslCertificateRequest);

		if(!isValidUser) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "onboard SSLcertificate").put(LogMessage.MESSAGE, "Invalid owner email")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid owner email\"]}");
		}

		ResponseEntity<String> response;
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(SSLCertificateConstants.COMMON_NAME, certificateUtils.getVaultCompactibleCertifiacteName(sslCertificateRequest.getCertificateName()));
		jsonObject.addProperty(SSLCertificateConstants.CERT_TYPE, sslCertificateRequest.getCertType());

		if (isCertAvailableInMetadata(jsonObject, token)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
					.put(LogMessage.MESSAGE, "Certificate already available in metadata.")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"Certificate already available in tvault\"]}");
		}

		List<Integer> containerList = new ArrayList<>();
		String nclmAccessToken = getNclmToken();
		//Get the container Id to onboard certificate
		findContainerIdForOnboardCertificate(sslCertificateRequest, containerList, nclmAccessToken);
		if (!containerList.isEmpty()) {
			for (int i = 0; i < containerList.size(); i++) {
				response = processAndSaveCertificateMetadata(sslCertificateRequest, userDetails, containerList.get(i),
						nclmAccessToken);

				if (response != null && !HttpStatus.OK.equals(response.getStatusCode())) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
							.put(LogMessage.MESSAGE, String.format("Onboarding failed for certificate name [%s]",
									sslCertificateRequest.getCertificateName()))
							.build()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("{\"errors\":[\"Certificate onboard failed\"]}");
				}

				else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
							.put(LogMessage.MESSAGE,
									String.format("Onbaording successful for certificate name [%s]",
											sslCertificateRequest.getCertificateName()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));
				}
			}
		} else {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
							.put(LogMessage.MESSAGE, String.format(
									"Onboarding failed for certificate name [%s]. Certificate not available in NCLM.",
									sslCertificateRequest.getCertificateName()))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate onboard failed\"]}");
		}
		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SSL certificate onboarded successfully\"]}");
	}

	/**
	 * Method to find the container Id
	 * @param sslCertificateRequest
	 * @param containerList
	 * @param nclmAccessToken
	 * @throws Exception
	 */
	private void findContainerIdForOnboardCertificate(SSLCertificateOnboardRequest sslCertificateRequest,
			List<Integer> containerList, String nclmAccessToken) throws Exception {
		if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
			containerList.add(private_single_san_ts_gp_id_test != 0 ? private_single_san_ts_gp_id_test
					: private_single_san_ts_gp_id);
		} else {
			CertificateData certDetailsExtSsan = null;
			CertificateData certDetailsExtMsan = null;
			int containerIdExtSsan = public_single_san_ts_gp_id_test != 0 ? public_single_san_ts_gp_id_test
					: public_single_san_ts_gp_id;
			int containerIdExtMsan = public_multi_san_ts_gp_id_test != 0 ? public_multi_san_ts_gp_id_test
					: public_multi_san_ts_gp_id;
			certDetailsExtSsan = getLatestCertificateFromNCLM(sslCertificateRequest.getCertificateName(),
					nclmAccessToken, containerIdExtSsan);
			if (certDetailsExtSsan != null && (!ObjectUtils.isEmpty(certDetailsExtSsan))
					&& (!ObjectUtils.isEmpty(certDetailsExtSsan.getCertificateName()))) {
				containerList.add(containerIdExtSsan);
			}
			certDetailsExtMsan = getLatestCertificateFromNCLM(sslCertificateRequest.getCertificateName(),
					nclmAccessToken, containerIdExtMsan);
			if (certDetailsExtMsan != null && (!ObjectUtils.isEmpty(certDetailsExtMsan))
					&& (!ObjectUtils.isEmpty(certDetailsExtMsan.getCertificateName()))) {
				containerList.add(containerIdExtMsan);
			}
		}
	}

	/**
	 * Method to validate the owner email
	 * @param sslCertificateRequest
	 * @return
	 */
	private boolean validateOwnerEmailForOnboard(SSLCertificateOnboardRequest sslCertificateRequest) {
		boolean isValidUser = false;
		ResponseEntity<DirectoryObjects> userResponse = directoryService
				.searchByUPNInGsmAndCorp(sslCertificateRequest.getCertOwnerEmailId());
		Object[] users = null;
		if (userResponse.getStatusCode().equals(HttpStatus.OK)) {
			users = userResponse.getBody().getData().getValues();
			if (!ObjectUtils.isEmpty(users)) {
				isValidUser = true;
			}
		}
		return isValidUser;
	}

	/**
	 * Method to validate the owner email
	 * @param sslCertificateRequest
	 * @return
	 */
	private boolean validateNotificationEmailsForOnboard(String notificationEmail) {
		boolean isValidNotificationEmail = true;
		String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";		
		for (String notifyEmail: notificationEmail.split(",")) {
			if((!Pattern.matches(regex, notifyEmail))) {
				isValidNotificationEmail = false;
			}
	    }
		return isValidNotificationEmail;
	}

	/**
	 * Method to validate whether the given application name is valid
	 * @param sslCertificateRequest
	 * @return
	 */
	private boolean validateApplicationNameForOnboard(SSLCertificateOnboardRequest sslCertificateRequest) {
		boolean isValidAppName = false;
		ResponseEntity<String> appResponse = workloadDetailsService.getWorkloadDetailsByAppName(sslCertificateRequest.getAppName());
		if ((!sslCertificateRequest.getAppName().equalsIgnoreCase(SSLCertificateConstants.APP_NAME_OTHER)) && (appResponse != null && HttpStatus.OK.equals(appResponse.getStatusCode()))) {
			isValidAppName = true;
		}
		return isValidAppName;
	}

    /**
     * Method to Create policies and metadata for the given certificate
     * @param certObject
     */
	private ResponseEntity<String> processAndSaveCertificateMetadata(SSLCertificateOnboardRequest sslCertificateRequest,
			UserDetails userDetails, int containerId, String nclmAccessToken) {
		try {
			ResponseEntity<String> metadataResponse = createCertificateMetadataAndPolicies(sslCertificateRequest,
					userDetails, containerId, nclmAccessToken);

			if (metadataResponse != null && !HttpStatus.OK.equals(metadataResponse.getStatusCode())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
						.put(LogMessage.MESSAGE,
								String.format("[%s] - Certficate metadata creation failed.  ",
										sslCertificateRequest.getCertificateName()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(ERRORS + "Certficate metadata creation failed. " + "\"]}");
			} else if (metadataResponse != null && HttpStatus.OK.equals(metadataResponse.getStatusCode())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
						.put(LogMessage.MESSAGE,
								String.format("[%s] - Certficate onboarding is successfully - completed.  ",
										sslCertificateRequest.getCertificateName()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			}
		} catch (Exception ex) {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
							.put(LogMessage.MESSAGE, String.format(
									"Onboardcertificate Exception = [%s] =  Message [%s] = certificate name = [%s]",
									Arrays.toString(ex.getStackTrace()), ex.getMessage(),
									sslCertificateRequest.getCertificateName()))
							.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
							.build()));

			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ERRORS + "Certificate onboard failed. " + "\"]}");
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"Certficate onboarding is successfully completed.\"]}");
	}

    /**
     * Method to create the metadata and policy for a certificate
     * @param sslCertificateRequest
     * @param userDetails
     * @param containerId
     * @return
     */
	private ResponseEntity<String> createCertificateMetadataAndPolicies(SSLCertificateOnboardRequest sslCertificateRequest,
			UserDetails userDetails, int containerId, String nclmAccessToken) {
		// Policy Creation
		boolean isPoliciesCreated = false;
		CertResponse enrollResponse = new CertResponse();
		ResponseEntity<String> permissionResponse;
		try {
			String metadataJson = populateMetadataForSSLOnboard(sslCertificateRequest, containerId,
					nclmAccessToken);
			if (metadataJson == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"Certificate onboard failed.\"]}");
			}
			SSLCertificateRequest sslCertRequest = new SSLCertificateRequest();
			BeanUtils.copyProperties(sslCertRequest, sslCertificateRequest);

			boolean sslMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson,
					tokenUtils.getSelfServiceToken());
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION,
							String.format(" [%s] - Metadata creation status - [%s] for metadatajson - [%s]",
									sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus,
									metadataJson))
					.build()));
			
			//add certificate name into application metadata list
            boolean sslApplicationMetaDataSaveStatus = certificateMetadataForApplicationDetails(metadataJson, tokenUtils.getSelfServiceToken(),"create");
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION,
							String.format(" [%s] - Applicatio Metadata creation status - [%s] for metadatajson - [%s]",
									sslCertificateRequest.getCertificateName(), sslApplicationMetaDataSaveStatus,
									metadataJson))
					.build()));

			if (sslMetaDataCreationStatus) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, String.format(" [%s] - Metadata Creation - Completed ",
								sslCertificateRequest.getCertificateName()))
						.build()));
				isPoliciesCreated = createPolicies(sslCertRequest, tokenUtils.getSelfServiceToken());
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION,
								String.format(" ERROR [%s] - Onboard failed. Metadata creation failed. metaDataStatus[%s]",
										sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus))
						.build()));
			}

			if (isPoliciesCreated) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, String.format(" [%s] - Policycreation - Completed",
								sslCertificateRequest.getCertificateName()))
						.build()));
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION,
								String.format(" ERROR [%s] - Onboard failed. Policy creation failed . policyStatus[%s]",
										sslCertificateRequest.getCertificateName(), isPoliciesCreated))
						.build()));
			}

			// Send failed certificate response in case of any issues in Policy/Meta data
			// creation
			if ((!sslMetaDataCreationStatus) || (!isPoliciesCreated)) {
				enrollResponse.setResponse("Metadatacreation failed");
				enrollResponse.setSuccess(Boolean.FALSE);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, String.format(
								"ERROR [%s] - Onboard failed. Metadata creation failed - metaDataStatus[%s] - policyStatus[%s]",
								sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus,
								isPoliciesCreated))
						.build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(ERRORS + enrollResponse.getResponse() + "\"]}");
			} else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "Onboard SSL certificate")
						.put(LogMessage.MESSAGE, "Sudo Policy Creation Started  ")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
				permissionResponse = addSudoPermissionToCertificateOwner(sslCertRequest, userDetails,
						enrollResponse, isPoliciesCreated, sslMetaDataCreationStatus, tokenUtils.getSelfServiceToken(),
						SSLCertificateConstants.ONBOARD);
				//Delete metadata and permissions if add sudo permission failed
				deleteMeataDataIfOwnerPermissionFailed(sslCertificateRequest, permissionResponse);

				return permissionResponse;
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ERRORS + enrollResponse.getResponse() + "\"]}");
		}
	}

	/**
	 * Method to process and call the deleteMetaDataAndPermissions method for
	 * deleting the metadata and permissions
	 *
	 * @param sslCertificateRequest
	 * @param permissionResponse
	 */
	private void deleteMeataDataIfOwnerPermissionFailed(SSLCertificateOnboardRequest sslCertificateRequest,
			ResponseEntity<String> permissionResponse) {
		boolean isDeleted = false;
		if (permissionResponse != null && !(HttpStatus.OK.equals(permissionResponse.getStatusCode()))) {
			String certificateName = certificateUtils.getVaultCompactibleCertifiacteName(sslCertificateRequest.getCertificateName());
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "Onboard SSL certificate")
					.put(LogMessage.MESSAGE,
							String.format(" [%s] - ERROR - Sudo Policy Creation Failed  ",
									certificateName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(
					tokenUtils.getSelfServiceToken(), certificateName,
					sslCertificateRequest.getCertType());
			String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))
					? SSLCertificateConstants.SSL_CERT_PATH
					: SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
			String certificatePath = metaDataPath + '/' + certificateName;
			isDeleted = deleteMetaDataAndPermissions(certificateMetaData, certificatePath,
					tokenUtils.getSelfServiceToken());
			if (isDeleted) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.ONBOARD_SSL_CERTIFICATE)
						.put(LogMessage.MESSAGE,
								String.format(" [%s] - ERROR - Metadata and policy deletion Completed  ",
										certificateName))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
						.build()));
			}
		}
	}

    /**
	 * Method to populate certificate metadata details
	 * @param sslCertificateRequest
	 * @param userDetails
	 * @param certManagerLogin
	 * @return
	 * @throws Exception
	 */
	private String populateMetadataForSSLOnboard(SSLCertificateOnboardRequest sslCertificateRequest,
			int containerId, String nclmAccessToken) throws Exception {

		String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))
				? SSLCertificateConstants.SSL_CERT_PATH
				: SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
		String certOwnerNtId = "";
		String displayName = "";
		String certMetadataPath = metaDataPath + '/' + certificateUtils.getVaultCompactibleCertifiacteName(sslCertificateRequest.getCertificateName());

		SSLCertificateMetadataDetails sslCertificateMetadataDetails = new SSLCertificateMetadataDetails();

		//Populate the sslCertificateMetadataDetails based on the App name details from workload api
		populateMetadataDetailsByAppNameDetails(sslCertificateRequest, sslCertificateMetadataDetails);

		CertificateData certDetails = null;
		certDetails = getLatestCertificateFromNCLM(sslCertificateRequest.getCertificateName(), nclmAccessToken,
				containerId);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_ONBOARD_METADATA)
				.put(LogMessage.MESSAGE,
						String.format("Fetching certificate details  status = [%s]", Objects.nonNull(certDetails)))
				.build()));
		if (Objects.nonNull(certDetails) && certDetails.getCertificateId() > 0) {
			sslCertificateMetadataDetails.setCertificateId(certDetails.getCertificateId());
			sslCertificateMetadataDetails.setCertificateName(certificateUtils.getVaultCompactibleCertifiacteName(certDetails.getCertificateName()));
			sslCertificateMetadataDetails.setCreateDate(certDetails.getCreateDate());
			sslCertificateMetadataDetails.setExpiryDate(certDetails.getExpiryDate());
			sslCertificateMetadataDetails.setAuthority(certDetails.getAuthority());
			sslCertificateMetadataDetails.setCertificateStatus(certDetails.getCertificateStatus());
			sslCertificateMetadataDetails.setContainerName(certDetails.getContainerName());
			sslCertificateMetadataDetails.setDnsNames(certDetails.getDnsNames());
			if (!sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
				sslCertificateMetadataDetails.setCertType("external");
				sslCertificateRequest.setCertType("external");
				sslCertificateMetadataDetails.setRequestStatus(SSLCertificateConstants.APPROVED);
			} else {
				sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
				sslCertificateMetadataDetails.setDnsNames(certDetails.getDnsNames());
			}
		} else {
			log.debug(
					JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_ONBOARD_METADATA)
							.put(LogMessage.MESSAGE,
									String.format("Certificate Details is not available in NCLM for given "
											+ "certificate = [%s]", sslCertificateRequest.getCertificateName()))
							.build()));
			return null;
		}

		ResponseEntity<DirectoryObjects> userResponse = directoryService
				.searchByUPNInGsmAndCorp(sslCertificateRequest.getCertOwnerEmailId());
		Object[] users = null;
		DirectoryUser dirUser;
		if (userResponse.getStatusCode().equals(HttpStatus.OK)) {
			users = userResponse.getBody().getData().getValues();
			if (!ObjectUtils.isEmpty(users)) {
				dirUser = (DirectoryUser) users[0];
				certOwnerNtId = dirUser.getUserName().toLowerCase();
			}
		}
		sslCertificateMetadataDetails.setCertCreatedBy(certOwnerNtId);
		sslCertificateMetadataDetails.setCertOwnerEmailId(sslCertificateRequest.getCertOwnerEmailId());
		sslCertificateMetadataDetails.setCertOwnerNtid(certOwnerNtId);
		sslCertificateMetadataDetails.setContainerId(containerId);
        sslCertificateMetadataDetails.setOnboardFlag(Boolean.TRUE);
		sslCertificateRequest.setCertOwnerNtid(certOwnerNtId);

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION,
						String.format("MetaData info details = [%s]", sslCertificateMetadataDetails.toString()))
				.build()));

		SSLCertMetadata sslCertMetadata = new SSLCertMetadata(certMetadataPath, sslCertificateMetadataDetails);
		String jsonStr = JSONUtil.getJSON(sslCertMetadata);
		Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
		rqstParams.put("path", certMetadataPath);
		return ControllerUtil.convetToJson(rqstParams);
	}

	/**
	 * Method to populate the sslCertificateMetadataDetails based on the App name details from workload api
	 * @param sslCertificateRequest
	 * @param sslCertificateMetadataDetails
	 */
	private void populateMetadataDetailsByAppNameDetails(SSLCertificateOnboardRequest sslCertificateRequest,
			SSLCertificateMetadataDetails sslCertificateMetadataDetails) {
		// Get Application details
		String applicationName = sslCertificateRequest.getAppName();
		if (StringUtils.isEmpty(applicationName)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "populateSSLCertificateMetadataForOnboard ")
					.put(LogMessage.MESSAGE,
							String.format("ERROR -[%s]- Application Name is not available in the certificate details. ",
									sslCertificateRequest.getCertificateName()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		} else {
			ResponseEntity<String> appResponse = workloadDetailsService.getWorkloadDetailsByAppName(applicationName);
			if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
				JsonParser jsonParser = new JsonParser();
				JsonObject response = (JsonObject) jsonParser.parse(appResponse.getBody());
				processWorkloadDetailsAndConstructMetadata(sslCertificateRequest, sslCertificateMetadataDetails,
						applicationName, response);
			} else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "Getting Application Details by app name during Meta data creation ")
						.put(LogMessage.MESSAGE,
								String.format("Application details will not insert/update in metadata  "
										+ "for an application =  [%s] ", applicationName))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			}
		}
	}

	/**
	 * Method to process workload details and construct the metadata for onboarding the certificate
	 * @param sslCertificateRequest
	 * @param sslCertificateMetadataDetails
	 * @param applicationName
	 * @param response
	 */
	private void processWorkloadDetailsAndConstructMetadata(SSLCertificateOnboardRequest sslCertificateRequest,
			SSLCertificateMetadataDetails sslCertificateMetadataDetails, String applicationName, JsonObject response) {
		String projectLeadEmail = "";
		JsonObject jsonElement = null;
		if (Objects.nonNull(response)) {
			jsonElement = response.get("spec").getAsJsonObject();
			if (Objects.nonNull(jsonElement)) {
				String applicationTag = validateString(jsonElement.get("tag"));
				projectLeadEmail = validateString(jsonElement.get("projectLeadEmail"));

				if (projectLeadEmail != null && !StringUtils.isEmpty(projectLeadEmail)) {
					String[] projectLeadEmails = Arrays.stream(projectLeadEmail.split(",")).map(String::trim)
							.toArray(String[]::new);
					sslCertificateMetadataDetails.setProjectLeadEmailId(projectLeadEmails[0]);
				} else {
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION, SSLCertificateConstants.POPULATE_ONBOARD_METADATA)
							.put(LogMessage.MESSAGE,
									String.format(
											"Project lead email id is not available for given "
													+ "certificate = [%s]",
											sslCertificateRequest.getCertificateName()))
							.build()));
					projectLeadEmail = sslCertificateRequest.getCertOwnerEmailId();
					sslCertificateMetadataDetails
							.setProjectLeadEmailId(sslCertificateRequest.getCertOwnerEmailId());
				}
				String appOwnerEmail = validateString(jsonElement.get("brtContactEmail"));
				String akmid = validateString(jsonElement.get("akmid"));
				String notificationEmails = sslCertificateRequest.getNotificationEmail();
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "Populate Application details in SSL Certificate Metadata")
						.put(LogMessage.MESSAGE,
								String.format("Application Details  for an "
										+ "applicationName = [%s] , applicationTag = [%s], "
										+ "projectLeadEmail =  [%s],appOwnerEmail =  [%s], akmid = [%s]",
										applicationName, applicationTag, projectLeadEmail, appOwnerEmail,
										akmid))
						.build()));

				sslCertificateMetadataDetails.setAkmid(akmid);
				sslCertificateMetadataDetails.setApplicationOwnerEmailId(appOwnerEmail);
				sslCertificateMetadataDetails.setApplicationTag(applicationTag);
				sslCertificateMetadataDetails.setApplicationName(applicationName);
				String[] notifEmailLst = notificationEmails.split(",");
				notifEmailLst = Arrays.stream(notifEmailLst).map(String::toLowerCase).distinct().toArray(String[]::new);
				sslCertificateMetadataDetails.setNotificationEmails(String.join(",", notifEmailLst));
			}
		}
	}
	
	   
    /**
     * To update the metadata for certificate
     * @param certificateUpdateRequest
     * @param userDetails
     * @param token
     * @return
     */
	public ResponseEntity<String> updateSSLCertificate(CertificateUpdateRequest certificateUpdateRequest, UserDetails userDetails,  String token) {  

    	boolean isValidData = false;
    	int count =0;
    	String[] notifEmailLst =new String[] {};
    	
    	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
    			.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
    			.put(LogMessage.ACTION, SSLCertificateConstants.UPDATE_SSL_CERTIFICATE)
    			.put(LogMessage.MESSAGE, String.format("Trying to update the metadata of the certificate [%s]", certificateUpdateRequest.getCertificateName()))
    			.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
    			.build()));
    	if(isValidInputs(certificateUpdateRequest.getCertificateName(), certificateUpdateRequest.getCertType()) && (certificateUpdateRequest.getApplicationOwnerEmail()!=null ? validateCertficateEmail(certificateUpdateRequest.getApplicationOwnerEmail()):true)
    			&& (certificateUpdateRequest.getProjectLeadEmail()!=null ? validateCertficateEmail(certificateUpdateRequest.getProjectLeadEmail() ):true)) {
    		isValidData = true;
    		if(certificateUpdateRequest.getNotificationEmail()!=null) {
				notifEmailLst = certificateUpdateRequest.getNotificationEmail().split(",");
				notifEmailLst = Arrays.stream(notifEmailLst).map(String::toLowerCase).distinct().toArray(String[]::new);
    			for(int i=0; i<notifEmailLst.length;i++) {
    				if(validateCertficateEmail(notifEmailLst[i] )) {
    					count++;
    				}
    			}
    			if(count==notifEmailLst.length) {
    				isValidData = true;
    			}else if(count<notifEmailLst.length) {
    				isValidData = false;
    			}
    		}
    	}
    	Map<String, String> metaDataParams = new HashMap<String, String>();
		if (!isValidData) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "invalidInputData")
					.put(LogMessage.MESSAGE, SSLCertificateConstants.INVALID_INPUT_MSG)
					.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ERRORINVALID);
		} else {
			String endPoint = certificateUpdateRequest.getCertificateName();
			String metaDataPath = (certificateUpdateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	                SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + endPoint;
			Response response = new Response();
			if (!userDetails.isAdmin()) {
				Boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certificateUpdateRequest.getCertificateName(),certificateUpdateRequest.getCertType());
				
				if (!isPermission) {
					log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
							.put(LogMessage.ACTION,
									String.format("Certificate permission for user [%s] ", userDetails.getUsername()))
							.put(LogMessage.MESSAGE,
									String.format("User has no permission to access the certificate [%s]",
											certificateUpdateRequest.getCertificateName()))
							.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString()).put(LogMessage.APIURL,
									ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
							.build()));
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
							.body(ERRORS
									+ "Access denied: No permission to update certificate"
									+ "\"]}");
				}
			}
			try {
				if (userDetails.isAdmin()) {
					response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", token);
				} else {
					response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}",
							userDetails.getSelfSupportToken());
				}
			} catch (Exception e) {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION,
												String.format("Exception = [%s] =  Message [%s]",
														Arrays.toString(e.getStackTrace()), response.getResponse()))
										.build()));
				return ResponseEntity.status(response.getHttpstatus())
						.body(MESSAGES + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
			}
			if (!HttpStatus.OK.equals(response.getHttpstatus())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.UPDATE_SSL_CERTIFICATE)
						.put(LogMessage.MESSAGE, SSLCertificateConstants.CERTIFICATE_UNAVAILABLE)
						.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
				return ResponseEntity.status(response.getHttpstatus())
						.body(ERRORS + SSLCertificateConstants.CERTIFICATE_UNAVAILABLE + "\"]}");
			}
			JsonParser jsonParser = new JsonParser();
			JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
			metaDataParams = new Gson().fromJson(object.toString(), Map.class);	
			boolean sslMetaDataUpdationStatus;
			if(userDetails.isAdmin()) {
			if(certificateUpdateRequest.getApplicationOwnerEmail()!=null  ) {
			metaDataParams.put("applicationOwnerEmailId", certificateUpdateRequest.getApplicationOwnerEmail());
			}
			if(certificateUpdateRequest.getProjectLeadEmail()!=null) {
			metaDataParams.put("projectLeadEmailId", certificateUpdateRequest.getProjectLeadEmail());
			}
			}else if(!((certificateUpdateRequest.getApplicationOwnerEmail()==null ?true: certificateUpdateRequest.getApplicationOwnerEmail().equalsIgnoreCase(metaDataParams.get("applicationOwnerEmailId")))
					&& (certificateUpdateRequest.getProjectLeadEmail()==null ?true: certificateUpdateRequest.getProjectLeadEmail().equalsIgnoreCase(metaDataParams.get("projectLeadEmailId"))))){
				
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.UPDATE_SSL_CERTIFICATE)
						.put(LogMessage.MESSAGE, String.format(
								"No permission to update application owner email [%s] or project lead email [%s]",
								certificateUpdateRequest.getApplicationOwnerEmail(),
								certificateUpdateRequest.getProjectLeadEmail()))
						.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ERRORS
								+ "Access denied: No permission to update application owner email or project lead email"
								+ "\"]}");
			}
			if(certificateUpdateRequest.getNotificationEmail()!=null ){						
			metaDataParams.put("notificationEmails", String.join(",", notifEmailLst));
			}
		try {
		if (userDetails.isAdmin()) {
			sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams, token);

		} else {
			sslMetaDataUpdationStatus = ControllerUtil.updateMetaDataOnPath(metaDataPath, metaDataParams,
					userDetails.getSelfSupportToken());	

		}
		if (sslMetaDataUpdationStatus) {

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.UPDATE_SSL_CERTIFICATE).
                    put(LogMessage.MESSAGE, String.format("Successfully updated the metadata for   " +
                                    "[%s] ",
                             metaDataParams.get("certificateName"),java.time.LocalDateTime.now())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));

			return ResponseEntity.status(HttpStatus.OK)
					.body(MESSAGES + "Certificate details updated successfully" + "\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, SSLCertificateConstants.UPDATE_SSL_CERTIFICATE)
					.put(LogMessage.MESSAGE, "Certificate details updation failed")
					.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(ERRORS + "Certificate details updation failed" + "\"]}");
		}

		}
		 catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ERRORS + e.getMessage() + "\"]}");
		}
		}
    }

    /**
     * To get metadata for a certificate path.
     * @param token
     * @param certPath
     * @return
     */
    public SSLCertMetadataResponse getCertMetadata(String token, String certPath) {
	    ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SSLCertMetadataResponse sslCertMetadataResponse = null;
        Response response = reqProcessor.process("/read", "{\"path\":\"" + certPath + "\"}", token);
	    if (HttpStatus.OK.equals(response.getHttpstatus())) {
            try {
                sslCertMetadataResponse = objectMapper.readValue(response.getResponse(), new TypeReference<SSLCertMetadataResponse>() {});
                log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "getCertMetadata").
                        put(LogMessage.MESSAGE, String.format("Parsed metadata for certificate [%s]", certPath)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
            catch (IOException e) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "getCertMetadata").
                        put(LogMessage.MESSAGE, String.format("Failed to parse metadata for certificate [%s]", certPath)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
        }
        return sslCertMetadataResponse;
    }

    /**
     * Application metadata creation
     * @param sslCertificateJson
     * @param token
     * @return
     */
    private boolean certificateMetadataForApplicationDetails(String sslCertificateJson, String token, String method) {
		 TMOAppMetadataDetails tmoAppMetadataDetails = new TMOAppMetadataDetails();
			JsonParser jsonParser = new JsonParser();
			
			JsonObject object = ((JsonObject) jsonParser.parse(sslCertificateJson)).getAsJsonObject("data");
			String appName = object.get("applicationName").getAsString();
			String certType = object.get("certType").getAsString();
			String certPath =  TVaultConstants.TMO_APP_METADATA_PATH + "/" + appName;
			List<String> certList = new ArrayList<>();
			
			boolean isAppDetailsAvailable = true;
			boolean isCertDataUpdated = false;
			isAppDetailsAvailable = checkAppDetailsAvailable( appName, token);
			
			
				tmoAppMetadataDetails.setApplicationName(object.get("applicationName").getAsString());
				tmoAppMetadataDetails.setApplicationTag(object.get("applicationTag").getAsString());
				tmoAppMetadataDetails.setApplicationOwnerEmailId(object.get("applicationOwnerEmailId")==null?null:object.get("applicationOwnerEmailId").getAsString());
				tmoAppMetadataDetails.setProjectLeadEmailId(object.get("projectLeadEmailId")!=null?object.get("projectLeadEmailId").getAsString():null);
				tmoAppMetadataDetails.setUpdateFlag(Boolean.TRUE);
				certList.add(object.get("certificateName").getAsString());
				if(!isAppDetailsAvailable) {
				if(certType.equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)) {
					tmoAppMetadataDetails.setExternalCertificateList(certList);
				}else {
					tmoAppMetadataDetails.setInternalCertificateList(certList);
				}			
				}
				else {
					if(!method.equalsIgnoreCase("create")) {
						boolean isDataMismatch= checkApplicationMismatch(tmoAppMetadataDetails, token);
						tmoAppMetadataDetails.setUpdateFlag(isDataMismatch);
		                log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                        put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
		                        put(LogMessage.MESSAGE, String.format("Application details mismatch for application [%s]", tmoAppMetadataDetails.getApplicationName())).
		                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                        build()));
						
					}
					
					tmoAppMetadataDetails = addCertToAppList(tmoAppMetadataDetails,appName,certType,object.get("certificateName").getAsString(),
							 token);
				}
			TMOAppMetadata sslCertMetadata = new TMOAppMetadata(certPath, tmoAppMetadataDetails);
	        String jsonStr = JSONUtil.getJSON(sslCertMetadata);
	        Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
	        rqstParams.put("path", certPath);
	        String certDataJson = ControllerUtil.convetToJson(rqstParams);
			Response response = reqProcessor.process("/write", certDataJson, token);			

			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
		                    put(LogMessage.MESSAGE, "SSL certificate metadata creation is success for application details creation ").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
				 isCertDataUpdated = true;
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
						put(LogMessage.MESSAGE, "SSL certificate metadata creation failed for application details creation").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			
						
			
			return isCertDataUpdated;
		}
    
    /**
     * Method to check application metadata details is already available for given application
     * @param appName
     * @param token
     * @return
     */ 
	 private boolean checkAppDetailsAvailable(String appName, String token) {
		 String certPath =  TVaultConstants.TMO_APP_METADATA_PATH + "/" + appName;
			Response response = new Response();
			boolean isDataAvailable = true;
			
			try {
					response = reqProcessor.process("/read", "{\"path\":\"" + certPath + "\"}", token);
					
			} catch (Exception e) {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION,
												String.format("Exception = [%s] =  Message [%s]",
														Arrays.toString(e.getStackTrace()), response.getResponse()))
										.build()));				
			}
			if (!HttpStatus.OK.equals(response.getHttpstatus())) {
				isDataAvailable = false;
			}
			return isDataAvailable;
	 }
	 
	 private boolean checkApplicationMismatch(TMOAppMetadataDetails tmoDetails, String token) {
		 String certPath =  TVaultConstants.TMO_APP_METADATA_PATH + "/" + tmoDetails.getApplicationName();
			Response response = new Response();
			JsonParser jsonParser = new JsonParser();
			boolean isNotMatching = true;
			
			try {
					response = reqProcessor.process("/read", "{\"path\":\"" + certPath + "\"}", token);
					
			} catch (Exception e) {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION,
												String.format("Exception = [%s] =  Message [%s]",
														Arrays.toString(e.getStackTrace()), response.getResponse()))
										.build()));				
			}
			if (HttpStatus.OK.equals(response.getHttpstatus())) {
				JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
				String newAppOwner = (object.get("applicationOwnerEmailId")==null?"":object.get("applicationOwnerEmailId").getAsString());
				String newLeadEmail = (object.get("projectLeadEmailId")==null?"":object.get("projectLeadEmailId").getAsString());
				isNotMatching = object.get("updateFlag").getAsBoolean();
				if(object.get("updateFlag").getAsBoolean()==true) {
				if((!newAppOwner.equalsIgnoreCase(tmoDetails.getApplicationOwnerEmailId())) || (!newLeadEmail.equalsIgnoreCase(tmoDetails.getProjectLeadEmailId()))) {
					isNotMatching = false;
				}
				}
			}
			return isNotMatching;
	 }
	 
	/**
	 * Method to add certificate name into the certlist in application metadata
	 * @param details
	 * @param appName
	 * @param certType
	 * @param certName
	 * @param token
	 * @return
	 */
	 private TMOAppMetadataDetails addCertToAppList(TMOAppMetadataDetails details,String appName, String certType, String certName, String token) {
		 String certPath =  TVaultConstants.TMO_APP_METADATA_PATH + "/" + appName;
			Response response = new Response();
			try {
					response = reqProcessor.process("/read", "{\"path\":\"" + certPath + "\"}", token);
					
			} catch (Exception e) {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION,
												String.format("Exception = [%s] =  Message [%s]",
														Arrays.toString(e.getStackTrace()), response.getResponse()))
										.build()));				
			}
			if (!HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, "SSL certificate metadata is not available for the given application ").
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
			}
			JsonParser jsonParser = new JsonParser();
			JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
			List<String> certList = new ArrayList<>();
			List<String> certListOld = new ArrayList<>();
			JsonArray jsonArr = new JsonArray();
			 jsonArr = object.getAsJsonArray(certType+"CertificateList"); 			 
			 if (jsonArr != null) { 
				 jsonArr.add(certName);
				   for (int i=0;i<jsonArr.size();i++){ 
					   certList.add(jsonArr.get(i).toString().substring(1, jsonArr.get(i).toString().length() - 1));
				   }
			 }else {
				 certList.add(certName);
			 }
			 LinkedHashSet<String> certSet =  new LinkedHashSet<String>(certList);
			 certList.clear();
			 certList.addAll(certSet);
			 if(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
				 details.setInternalCertificateList(certList);
			 }else {
				 details.setExternalCertificateList(certList);
			 }
			 
			 String certListExist = certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)?"externalCertificateList":"internalCertificateList";
			 JsonArray jsonArrExist = object.getAsJsonArray(certListExist); 
			 if (jsonArrExist != null) { 
				   for (int i=0;i<jsonArrExist.size();i++){ 
					   certListOld.add(jsonArrExist.get(i).toString().substring(1, jsonArrExist.get(i).toString().length() - 1));
				   }
			 
			 if(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
				 details.setExternalCertificateList(certListOld);
			 }else {
				 details.setInternalCertificateList(certListOld);
			 }
			 }
			return details;
	 }
	 
	 /**
	  * Function to update the application metadata
	  * @param object
	  * @param token
	  * @return
	  */
	 private boolean updatecertificateMetadataForApplicationDetails(JsonObject object, String token) {
		 TMOAppMetadataDetails tmoAppMetadataDetails = new TMOAppMetadataDetails();
			
			String appName = object.get("applicationName").getAsString();
			String certType = object.get("certType").getAsString();
			String certPath =  TVaultConstants.TMO_APP_METADATA_PATH + "/" + appName;
			List<String> certList = new ArrayList<>();
			
			boolean isCertDataUpdated = false;			
			
				tmoAppMetadataDetails.setApplicationName(object.get("applicationName").getAsString());
				tmoAppMetadataDetails.setApplicationTag(object.get("applicationTag").getAsString());
				tmoAppMetadataDetails.setApplicationOwnerEmailId(object.get("applicationOwnerEmailId")==null?null:object.get("applicationOwnerEmailId").getAsString());
				tmoAppMetadataDetails.setProjectLeadEmailId(object.get("projectLeadEmailId")!=null?object.get("projectLeadEmailId").getAsString():null);
				tmoAppMetadataDetails.setUpdateFlag(Boolean.TRUE);
				certList.add(object.get("certificateName").getAsString());				
				tmoAppMetadataDetails = deleteCertFromAppList(tmoAppMetadataDetails,appName,certType,object.get("certificateName").getAsString(),
							 token);
				if(tmoAppMetadataDetails!=null) {
			TMOAppMetadata sslCertMetadata = new TMOAppMetadata(certPath, tmoAppMetadataDetails);
	        String jsonStr = JSONUtil.getJSON(sslCertMetadata);
	        Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
	        rqstParams.put("path", certPath);
	        String certDataJson = ControllerUtil.convetToJson(rqstParams);
			Response response = reqProcessor.process("/write", certDataJson, token);			

			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
		                    put(LogMessage.MESSAGE, "certificate name deletion success from application metadata path details ").
		                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                    build()));
				 isCertDataUpdated = true;
			}else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
						put(LogMessage.MESSAGE, "certificate name deletion from application metadata path failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			
				}			
			
			return isCertDataUpdated;
		}
	 
	 /**
	  * Removes the given certificate name from the certlist in application metadata
	  * @param details
	  * @param appName
	  * @param certType
	  * @param certName
	  * @param token
	  * @return
	  */
	 private TMOAppMetadataDetails deleteCertFromAppList(TMOAppMetadataDetails details,String appName, String certType, String certName, String token) {
		 String certPath =  TVaultConstants.TMO_APP_METADATA_PATH + "/" + appName;
			Response response = new Response();
			try {
					response = reqProcessor.process("/read", "{\"path\":\"" + certPath + "\"}", token);
					
			} catch (Exception e) {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION,
												String.format("Exception = [%s] =  Message [%s]",
														Arrays.toString(e.getStackTrace()), response.getResponse()))
										.build()));				
			}
			if (!HttpStatus.OK.equals(response.getHttpstatus())) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, "SSL certificate metadata is not avvailable for the given application ").
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
				return null;
			}
			JsonParser jsonParser = new JsonParser();
			JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
			List<String> certList = new ArrayList<>();
			List<String> certListOld = new ArrayList<>();
			JsonArray jsonArr;
			 jsonArr = object.getAsJsonArray(certType+"CertificateList"); 			 
			 if (jsonArr != null) { 
				   for (int i=0;i<jsonArr.size();i++){ 
					   if(jsonArr.get(i).toString().substring(1, jsonArr.get(i).toString().length() - 1).equalsIgnoreCase(certName)) {
						   continue;
					   }else {
					   certList.add(jsonArr.get(i).toString().substring(1, jsonArr.get(i).toString().length() - 1));
					   }
				   }
			 }
			 if(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
				 details.setInternalCertificateList(certList);
			 }else {
				 details.setExternalCertificateList(certList);
			 }
			 
			 String certListExist = certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)?"externalCertificateList":"internalCertificateList";
			 JsonArray jsonArrExist = object.getAsJsonArray(certListExist); 
			 if (jsonArrExist != null) { 
				   for (int i=0;i<jsonArrExist.size();i++){ 
					   certListOld.add(jsonArrExist.get(i).toString().substring(1, jsonArrExist.get(i).toString().length() - 1));
				   }
			 
			 if(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
				 details.setExternalCertificateList(certListOld);
			 }else {
				 details.setInternalCertificateList(certListOld);
			 }
			 }
			
			return details;
	 }
	 
	 /**
	  * Method to save application details for older certificates
	  * @param token
	  * @param userDetails
	  * @return
	  * @throws Exception
	  */
	 public ResponseEntity<String> saveAllAppDetailsForOldCerts(String token, UserDetails userDetails)
				 {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_PENDING_CERT_MSG)
					.put(LogMessage.MESSAGE, "Trying to get all certificates to save the application path")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			if (ObjectUtils.isEmpty(userDetails) || (!userDetails.isAdmin())) {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, SSLCertificateConstants.GET_ALL_PENDING_CERT_MSG)
						.put(LogMessage.MESSAGE, "Access denied: No permission to get the certificates details")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"Access denied: No permission to get the certificates details\"]}");
			}
			
			List<CertificateData> certificatesList = new ArrayList<>();
			String internalMetaDataPath = SSLCertificateConstants.SSL_CERT_PATH;
			String externalMetaDataPath = SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
	       	Response internalResponse;
	       	Response externalResponse;
	       	String certListStr = "";
	       	JsonParser jsonParser = new JsonParser();

	        internalResponse = getMetadata(token, internalMetaDataPath);
	        externalResponse = getMetadata(token, externalMetaDataPath);
	        
	        if (HttpStatus.OK.equals(internalResponse.getHttpstatus())) {
	        JsonObject jsonObject = (JsonObject) jsonParser.parse(internalResponse.getResponse());
	   		JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
	   		List<String> internalCertNames = geMatchCertificates(jsonArray,"");
	   		
	   		boolean isInternalSaved = saveApplicationDetailsForOldCerts(internalCertNames,"internal", token);
	        }
	        
	        if (HttpStatus.OK.equals(externalResponse.getHttpstatus())) {
	   		JsonObject jsonObjectExt = (JsonObject) jsonParser.parse(externalResponse.getResponse());
	   		JsonArray jsonArrayExt = jsonObjectExt.getAsJsonObject("data").getAsJsonArray("keys");
	   		List<String> externalCertNames = geMatchCertificates(jsonArrayExt,"");
	   		boolean isExternalSaved = saveApplicationDetailsForOldCerts(externalCertNames,"external", token);
	        }
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Application details updation is successfully completed.\"]}");

	 }
	 
	 /**
	  * iterate the internal/external cert list and save the app details
	  * @param certList
	  * @param certType
	  * @param token
	  * @return
	  */
	 private boolean saveApplicationDetailsForOldCerts(List<String> certList, String certType, String token) {
		 boolean isSaved = true;
		 String pathStr= "";
	   		String endPoint = "";
	   		Response response = new Response();
	   		JsonParser jsonParser = new JsonParser();
	   		JsonArray responseArray = new JsonArray();
	   		JsonObject metadataJsonObj=new JsonObject();
		 String metaDataPath = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))?
	                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
		 for (int i = 0; i < certList.size(); i++) {
			  endPoint = certList.get(i).replaceAll(CERTNAMEREGEX, "");
				 pathStr = metaDataPath + "/" + endPoint;
				 response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);
				if (HttpStatus.OK.equals(response.getHttpstatus())) {
					certificateMetadataForApplicationDetails(response.getResponse(), token, "update");
				}
			}
		 return isSaved;
	 }
	 
	 /**
	  * Check if the application name is assigned to the user
	  * @param token
	  * @param userDetails
	  * @param appName
	  * @return
	  */
	 private boolean validateAppname(String token,UserDetails userDetails, String appName) {
		 boolean isValid = false;
		 if(userDetails.isAdmin()) {
			 isValid = true;
		 }
		 else {
		 ResponseEntity<String> appResponse = getAllSelfServiceGroups(userDetails);
		 if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
				JsonParser jsonParser = new JsonParser();
				JsonArray responseArray =  (JsonArray) jsonParser.parse(appResponse.getBody());
				 
				for (int i=0;i<responseArray.size();i++) {
					if(responseArray.get(i).toString().substring(1, responseArray.get(i).toString().length() - 1).equalsIgnoreCase(appName)) {
						isValid=true;
						break;
					}
				}
			}
	 }
		 return isValid;
	 }

	/**
	 * Get Certificates for non-admin
	 * @param userDetails
	 * @param path
	 * @param certificateNames
	 * @param limit
	 * @param offset
	 * @return
	 */
	private String getAllCertificateListFromPermissions(UserDetails userDetails, String path, List<String> certificateNames, Integer limit, Integer offset) {
		oidcUtil.renewUserToken(userDetails.getClientToken());
		String token = userDetails.getSelfSupportToken();

		JsonArray responseArray = new JsonArray();
		JsonArray responseSubArray = new JsonArray();
		JsonParser jsonParser = new JsonParser();
		JsonObject metadataJsonObj = new JsonObject();

		getCertificateMetadataForAllCertNames(path, certificateNames, token, responseArray, jsonParser);

		Integer totCount = responseArray.size();
		limit = (limit == null) ? totCount : limit;
		offset = (offset == null) ? 0 : offset;
		if (ObjectUtils.isEmpty(responseArray)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getAllCertificateListFromPermissions")
					.put(LogMessage.MESSAGE, "Certificates metadata is not available")
					.put(LogMessage.STATUS,"No certificates available")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL))
					.build()));
		}else {
			int maxVal = (totCount > (limit+offset))?limit+offset : totCount;
			for (int i = offset; i < maxVal; i++) {
				responseSubArray.add(responseArray.get(i));
			}
		}
		metadataJsonObj.add("keys", responseSubArray);
		metadataJsonObj.addProperty("total", String.valueOf(totCount));
		metadataJsonObj.addProperty("next",
				(totCount - (responseSubArray.size() + offset) > 0
						? String.valueOf((responseSubArray.size() + offset))
						: "-1"));
		return metadataJsonObj.toString();
	}

	/**
	 * Method to get certificate metadata for all given certificate names
	 * @param path
	 * @param certificateNames
	 * @param token
	 * @param responseArray
	 * @param jsonParser
	 */
	private void getCertificateMetadataForAllCertNames(String path, List<String> certificateNames, String token,
			JsonArray responseArray, JsonParser jsonParser) {
		Response response = null;
		String pathStr = "";
		String endPoint = "";
		for (String certName : certificateNames) {
			endPoint = certName.replaceAll(CERTNAMEREGEX, "");
			pathStr = path + '/' + endPoint;
			response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);

			if (response != null && HttpStatus.OK.equals(response.getHttpstatus())
					&& !ObjectUtils.isEmpty(response.getResponse())) {
				JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, "getAllCertificateListFromPermissions")
						.put(LogMessage.MESSAGE, String.format("CertificatesName [%s] and Status [%s] is ",
								object.get("certificateName") == null ? ""
										: object.get("certificateName").getAsString(),
								response.getHttpstatus()))
						.build()));
				object.addProperty("certificateName", certificateUtils.getActualCertifiacteName(object.get("certificateName").getAsString()));
				responseArray.add(object);
			}
		}
	}

	/**
	 * Get all certificates owned by a user by certificate type
	 * @param userDetails
	 * @param certificateType
	 * @return
	 */
	private List<String> getAllOwnedCertificateFromPermissionsByCertType(UserDetails userDetails, String certificateType) {
		oidcUtil.renewUserToken(userDetails.getClientToken());
		String token = userDetails.getSelfSupportToken();

		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername(), userDetails);

		String certPrefix = (certificateType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL))
				? SSLCertificateConstants.INTERNAL_POLICY_NAME
				: SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		String policyPrefix = new StringBuilder().append(SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX)
				.append(certPrefix).append("_").toString();

		List<String> certificateNames = new ArrayList<>();
		if (policies != null) {
			for (String policy : policies) {
				getCertificateNamesFromPolicies(policyPrefix, certificateNames, policy);
			}
		}
		return certificateNames;
	}

	/**
	 * Method to get the certificate details from policies
	 * @param certificatePrefix
	 * @param certListUsers
	 * @param policy
	 */
	private void getCertificateNamesFromPolicies(String certificatePrefix, List<String> certificateNames,
			String policy) {
		if (policy.startsWith(certificatePrefix)) {
			String[] certificatePolicies = policy.split("_", -1);
			if (certificatePolicies.length >= 3) {
				String[] policyName = Arrays.copyOfRange(certificatePolicies, 2, certificatePolicies.length);
				String certificateName = String.join("_", policyName);
				certificateNames.add(certificateName);
			}
		}
	}

	/**
	 * Get the certificate names matches the search keyword
	 * @param certNameList
	 * @param searchText
	 * @return
	 */
	private List<String> getMatchedCertificates(List<String> certNameList, String searchText) {
		List<String> matchedlist = new ArrayList<>();
		if (!ObjectUtils.isEmpty(certNameList)) {
			if (!StringUtils.isEmpty(searchText)) {
				for (String certName : certNameList) {
					if (certName.contains(searchText)) {
						matchedlist.add(certName);
					}
				}
			} else {
				matchedlist = certNameList;
			}
		}
		return matchedlist;
	}

    /**
     * To get all certificate names which the user has read/deny/owner permission
     * @param token
     * @param userDetails
     * @param searchText
     * @return
     */
    public ResponseEntity<String> getFullCertificateList(String token, UserDetails userDetails, String searchText) {
        if (!StringUtils.isEmpty(searchText) && searchText.length() < 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Search text must be of minimum 3 characters.\"]}");
        }
        Map<String, List<String>> certificateList = new HashMap<>();
        // For admin users, take certificate names from metadata list
        if (userDetails.isAdmin()) {
            // Get internal certificate list from metadata
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "getFullCertificateList")
                    .put(LogMessage.MESSAGE, "Trying to get all certificates for admin from metadata list")
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
            Response internalCertListResponse = getMetadata(token, SSLCertificateConstants.SSL_CERT_PATH);
            List<String> internalCertificateNames = getCertificateListFromResponse(internalCertListResponse);
            // Get external certificate list from metadata
            Response externalCertListResponse = getMetadata(token, SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH);
            List<String> externalCertificateNames = getCertificateListFromResponse(externalCertListResponse);

            certificateList.put(SSLCertificateConstants.INTERNAL, internalCertificateNames);
            certificateList.put(SSLCertificateConstants.EXTERNAL, externalCertificateNames);
        }
        else {
            // for normal users, take certificate names from policy list
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, "getFullCertificateList")
                    .put(LogMessage.MESSAGE, "Trying to get all certificates for normal user from policies")
                    .put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
            String[] policies = policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername(), userDetails);
            certificateList = extractCertificateNameFromPoilcies(policies);
        }
        // Filter based on searchText if exists
        if (!StringUtils.isEmpty(searchText) && searchText.length() >= 3) {
            List<String> filterCertNames = certificateList.get(SSLCertificateConstants.INTERNAL).stream().filter(s -> s.toLowerCase().contains(searchText)).collect(Collectors.toList());
            certificateList.put(SSLCertificateConstants.INTERNAL, filterCertNames);
            filterCertNames = certificateList.get(SSLCertificateConstants.EXTERNAL).stream().filter(s -> s.toLowerCase().contains(searchText)).collect(Collectors.toList());
            certificateList.put(SSLCertificateConstants.EXTERNAL, filterCertNames);
        }
        return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(certificateList));
    }

    /**
     * To get certificate name from metadata list response.
     * @param certResponse
     * @return
     */
    private List<String> getCertificateListFromResponse(Response certResponse) {
        JsonParser jsonParser = new JsonParser();
        List<String> certNames = new ArrayList<>();
        if (HttpStatus.OK.equals(certResponse.getHttpstatus())) {
            JsonObject jsonObject = (JsonObject) jsonParser.parse(certResponse.getResponse());
            JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
            certNames = geCertificateNamesFromJson(jsonArray);
        }
        return certNames;
    }

    /**
     * Get the certificate names from JsonArray
     * @param jsonArray
     * @return
     */
    private List<String> geCertificateNamesFromJson(JsonArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (!ObjectUtils.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(certificateUtils.getActualCertifiacteName(jsonArray.get(i).getAsString()));
            }
        }
        return list;
    }

    /**
     * To get certificate names from policy list.
     * @param policies
     * @return
     */
    private Map<String, List<String>> extractCertificateNameFromPoilcies(String[] policies) {
        List<String> internalCertificateNames = new ArrayList<>();
        List<String> externalCertificateNames = new ArrayList<>();

        if (policies != null) {
            for (String policy : policies) {
                if (isPolicyOfCertType(policy, SSLCertificateConstants.INTERNAL)) {
                    // check for internal cert policy
                    String certificateName = certificateUtils.getActualCertifiacteName(extractValidCertificateName(policy));
                    if (!StringUtils.isEmpty(certificateName) && !internalCertificateNames.contains(certificateName)) {
                        internalCertificateNames.add(certificateName);
                    }
                }
                else if (isPolicyOfCertType(policy, SSLCertificateConstants.EXTERNAL)) {
                    // check for external cert policy
                    String certificateName = certificateUtils.getActualCertifiacteName(extractValidCertificateName(policy));
                    if (!StringUtils.isEmpty(certificateName) && !externalCertificateNames.contains(certificateName)) {
                        externalCertificateNames.add(certificateName);
                    }
                }
            }
        }
        Map<String, List<String>> certificateList = new HashMap<>();
        certificateList.put(SSLCertificateConstants.INTERNAL, internalCertificateNames);
        certificateList.put(SSLCertificateConstants.EXTERNAL, externalCertificateNames);
        return certificateList;
    }

    /**
     * To check of the policy is of internal or external certificate type.
     * @param policy
     * @param certType
     * @return
     */
    private boolean isPolicyOfCertType(String policy, String certType) {
	    String certTypeName = SSLCertificateConstants.INTERNAL_POLICY_NAME;
	    if (certType.equals(SSLCertificateConstants.EXTERNAL)) {
            certTypeName = SSLCertificateConstants.EXTERNAL_POLICY_NAME;
        }
        String ownerPolicyPrefix = new StringBuilder().append(SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX)
                .append(certTypeName).append("_").toString();
        String readPolicyPrefix = new StringBuilder().append(SSLCertificateConstants.READ_CERT_POLICY_PREFIX)
                .append(certTypeName).append("_").toString();
        String writePolicyPrefix = new StringBuilder().append(SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX)
                .append(certTypeName).append("_").toString();
        String denyPolicyPrefix = new StringBuilder().append(SSLCertificateConstants.DENY_CERT_POLICY_PREFIX)
                .append(certTypeName).append("_").toString();

        if (policy.startsWith(ownerPolicyPrefix) || policy.startsWith(readPolicyPrefix)
                || policy.startsWith(writePolicyPrefix) || policy.startsWith(denyPolicyPrefix)) {
            return true;
        }
        return false;
    }

    /**
     * To extract valid certificate name from policy.
     * @param policy
     * @return
     */
    private String extractValidCertificateName(String policy) {
        String[] certificatePolicies = policy.split("_", -1);
        if (certificatePolicies.length >= 3) {
            String[] policyName = Arrays.copyOfRange(certificatePolicies, 2, certificatePolicies.length);
            return String.join("_", policyName);
        }
        return null;
    }

	/**
	 * Get the certificate names matches from the permissions list
	 *
	 * @param jsonArray
	 * @return
	 */
	private List<String> getMatchedCertificatesBasedOnPermissions(JsonArray jsonArray) {
		List<String> list = new ArrayList<>();
		if (!ObjectUtils.isEmpty(jsonArray)) {
			for (int i = 0; i < jsonArray.size(); i++) {
				list.add(certificateUtils.getActualCertifiacteName(jsonArray.get(i).getAsString()));
			}
		}
		return list;
	}
}

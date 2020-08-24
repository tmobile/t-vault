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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
import com.tmobile.cso.vault.api.validator.TokenValidator;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.CertificateUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Autowired
    private EmailUtils emailUtils;
    private static Logger log = LogManager.getLogger(SSLCertificateService.class);

    private static final String[] PERMISSIONS = {"read", "write", "deny", "sudo"};

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
        } else {
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
			if (Objects.nonNull(directoryUser)) {
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
                                                               UserDetails userDetails ,String token) {
        CertResponse enrollResponse = new CertResponse();

        //Validate the input data
        boolean isValidData = validateInputData(sslCertificateRequest, userDetails);
        if(!isValidData){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        }

		try {
			appendTmobileTextToCertificateName(sslCertificateRequest);
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
            CertManagerLogin certManagerLogin = login(certManagerLoginRequest);


            CertificateData certificateDetails = getCertificate(sslCertificateRequest, certManagerLogin);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Details for Certificate name =[%s] = Certificate Details " +
                                    "[%s]", sslCertificateRequest.getCertificateName(), certificateDetails)).
                    build()));

            token = (userDetails.isAdmin())?token : userDetails.getSelfSupportToken();

            if (Objects.isNull(certificateDetails)) {
                //Validate the certificate in metadata path  for external certificate
                if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)) {
                    SSLCertificateMetadataDetails certMetaData = certificateUtils.getCertificateMetaData(token,
                            sslCertificateRequest.getCertificateName(), sslCertificateRequest.getCertType());

                    if ((Objects.nonNull(certMetaData)) && (Objects.nonNull(certMetaData.getRequestStatus()))
                            && (certMetaData.getRequestStatus().equalsIgnoreCase(SSLCertificateConstants.REQUEST_PENDING_APPROVAL))) {
                        enrollResponse.setSuccess(Boolean.FALSE);
                        enrollResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
                        enrollResponse.setResponse("Given Certificate is waiting for NCLM approval ");
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, String.format("Given Certificate is waiting for NCLM approval  " +
                                        "[%s] = Certificate name = [%s]", enrollResponse.toString(),
                                        sslCertificateRequest.getCertificateName())).
                                build()));
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"" + enrollResponse.getResponse() +
                                "\"]}");
                    }
                }
                //Step-2 Validate targetSystem
                int targetSystemId = getTargetSystem(sslCertificateRequest, certManagerLogin);

                //Step-3:  CreateTargetSystem
                if (targetSystemId == 0) {
                    targetSystemId  =   createTargetSystem(sslCertificateRequest.getTargetSystem(), certManagerLogin,
                            getContainerId(sslCertificateRequest));
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("createTargetSystem Completed Successfully [%s] for " +
                                            "certificate name = [%s]",
                                    targetSystemId,sslCertificateRequest.getCertificateName())).
                            build()));
                    if (targetSystemId == 0){
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                        enrollResponse.setSuccess(Boolean.FALSE);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
                    }
                }

                //Step-4 : Validate the Target System Service
                int targetSystemServiceId = getTargetSystemServiceId(sslCertificateRequest, targetSystemId, certManagerLogin);


                //Step-5: Create Target System  Service
                if (targetSystemServiceId == 0) {
                    TargetSystemServiceRequest targetSystemServiceRequest = prepareTargetSystemServiceRequest(sslCertificateRequest);
                    TargetSystemService targetSystemService = createTargetSystemService(targetSystemServiceRequest, targetSystemId, certManagerLogin);

                    if (Objects.nonNull(targetSystemService)) {
                        targetSystemServiceId = targetSystemService.getTargetSystemServiceId();
                    } else {
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                        enrollResponse.setSuccess(Boolean.FALSE);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
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
                                response.getResponse(),sslCertificateRequest.getCertificateName())).
                        build()));

                ////Only for External -MultiSAN - Update the selectedId
                if ((!StringUtils.isEmpty(sslCertificateRequest.getCertType())) && sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)
                        && sslCertificateRequest.getDnsList().length > 0) {
                    response = prepareEnrollCARequest(response);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("getEnrollCA Completed Successfully [%s] = " +
                                            "For External-MultiSAN-certificate name = [%s]",
                                    response.getResponse(),sslCertificateRequest.getCertificateName())).
                            build()));
                }

                //Step-8 PutEnrollCA
                int updatedSelectedId = putEnrollCA(certManagerLogin, targetSystemServiceId, response);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("PutEnroll CA Successfully Completed[%s] = certificate " +
                                        "name = [%s]",
                                updatedSelectedId,sslCertificateRequest.getCertificateName())).
                        build()));

                //Step-9  GetEnrollTemplates
                CertResponse templateResponse = getEnrollTemplates(certManagerLogin, targetSystemServiceId, updatedSelectedId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("Get Enrollment template  Completed Successfully [%s] = certificate name = [%s]",
                                templateResponse.getResponse(),sslCertificateRequest.getCertificateName())).
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
                                "enrollTemplateId = [%s] = certificate name = [%s]", enrollTemplateId,sslCertificateRequest.getCertificateName())).
                        build()));

                if ((!StringUtils.isEmpty(sslCertificateRequest.getCertType())) && sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL)) {
                //GetTemplateParameters
                    CertResponse getTemplateResponse = getTemplateParametersResponse(certManagerLogin,
                            targetSystemServiceId, enrollTemplateId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("GetTemplateParameters  Successfully Completed = " +
                                    "getTemplateParamterRequest = [%s] = certificate name = [%s] ",
                                    getTemplateResponse.getResponse(),sslCertificateRequest.getCertificateName())).build()));
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
                                    putTemplateParameterResponse.getResponse(),sslCertificateRequest.getCertificateName())).build()));
                }

                //Step-11  GetEnrollKeys
                CertResponse getEnrollKeyResponse = getEnrollKeys(certManagerLogin, targetSystemServiceId, enrollTemplateId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, String.format("getEnrollKeys Completed Successfully [%s] = certificate name = [%s]",
                                getEnrollKeyResponse.getResponse(),sslCertificateRequest.getCertificateName())).
                        build()));

                //Step-12  PutEnrollKeys
                int enrollKeyId = putEnrollKeys(certManagerLogin, targetSystemServiceId, getEnrollKeyResponse, enrollTemplateId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, String.format("putEnrollKeys  Successfully Completed[%s] = certificate name = [%s]",
                                enrollKeyId,sslCertificateRequest.getCertificateName())).
                        build()));

                //Step-13 GetEnrollCSRs
                String updatedRequest = getEnrollCSR(certManagerLogin, targetSystemServiceId, enrollTemplateId, sslCertificateRequest);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, String.format("getEnrollCSRResponse Completed Successfully [%s] = certificate name = [%s]",
                                updatedRequest,sslCertificateRequest.getCertificateName())).
                        build()));

                //In case multiSAN(external/internal)-Need to build request for SubjectAlternateNames
                if (sslCertificateRequest.getDnsList() != null && sslCertificateRequest.getDnsList().length > 0) {
                    //Build Object with DNS names
                    updatedRequest = buildSubjectAlternativeNameRequest(updatedRequest, sslCertificateRequest,
                            targetSystemServiceId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("Build Object with DNS names [%s] = certificate name = [%s]",
                                    updatedRequest,sslCertificateRequest.getCertificateName())).
                            build()));

                } else {
                    //If dnsList is empty ,remove the DNS names object from request
                    updatedRequest = removeSubjectAlternativeNameRequest(updatedRequest);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("Remove  DNS names [%s] = certificate name = [%s]",
                                    updatedRequest,sslCertificateRequest.getCertificateName())).
                            build()));
                }

                //Step-14  PutEnrollCSRs
                CertResponse putEnrollCSRResponse = putEnrollCSR(certManagerLogin, targetSystemServiceId, updatedRequest);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, String.format("PutEnroll CSR  Successfully Completed  = [%s] = certificate name = [%s]",
                                putEnrollCSRResponse,sslCertificateRequest.getCertificateName())).
                        build()));
                  String responseDetails  = validateCSRResponse(putEnrollCSRResponse.getResponse());
                if(!StringUtils.isEmpty(responseDetails)) {
                    enrollResponse.setSuccess(Boolean.FALSE);
                    enrollResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
                    enrollResponse.setResponse(responseDetails);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("Exception While creating certificate  " +
                                            "[%s] = certificate name = [%s]", responseDetails,
                                    sslCertificateRequest.getCertificateName())).
                            build()));
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"" + enrollResponse.getResponse() +
                            "\"]}");
                }

                //Step-15: Enroll Process
                enrollResponse = enrollCertificate(certManagerLogin, targetSystemServiceId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, String.format("Enroll Certificate response Completed Successfully [%s]" +
                                " = certificate name = [%s]", enrollResponse.getResponse(),sslCertificateRequest.getCertificateName())).
                        build()));

                if (enrollResponse.getHttpstatus().equals(HttpStatus.ACCEPTED) && Objects.nonNull(enrollResponse.getResponse())) {
                    int actionId;
                    Map<String, Object> responseMap = ControllerUtil.parseJson(enrollResponse.getResponse());
                    if (!MapUtils.isEmpty(responseMap) && responseMap.get("actionId") != null) {
                        actionId = (Integer) responseMap.get("actionId");
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

					if (userDetails.isAdmin()) {
						isPoliciesCreated = createPolicies(sslCertificateRequest, token);
					} else {
						isPoliciesCreated = createPolicies(sslCertificateRequest, userDetails.getSelfSupportToken());
					}
                    if(isPoliciesCreated) {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("Policies are created for SSL certificate name [%s]",
                                        sslCertificateRequest.getCertificateName())).
                                build()));
                    }

                    String metadataJson = populateSSLCertificateMetadata(sslCertificateRequest, userDetails,
                            certManagerLogin);

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
                                        sslCertificateRequest.getCertificateName())).
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
												sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus,
												isPoliciesCreated))
								.build()));
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
                    } else {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, "generateSSLCertificate").
                                put(LogMessage.MESSAGE, "addSudoPermissionToCertificateOwner- STARTED ").
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                    	return addSudoPermissionToCertificateOwner(sslCertificateRequest, userDetails, enrollResponse
                                , isPoliciesCreated, sslMetaDataCreationStatus,token,"create");
                    }
                }
            } else {
                enrollResponse.setSuccess(Boolean.FALSE);
                enrollResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
                enrollResponse.setResponse("Certificate Already Available in  NCLM with Active Status");
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("Certificate Already Available in  NCLM with Active Status " +
                                "[%s] = certificate name = [%s]", enrollResponse.toString(),
                                sslCertificateRequest.getCertificateName())).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+enrollResponse.getResponse()+
                        "\"]}");
            }
        } catch (TVaultValidationException tex) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Inside  TVaultValidationException " +
                                    "Exception = [%s] =  Message [%s] = certificate name = [%s]",
                            Arrays.toString(tex.getStackTrace()), tex.getMessage(),sslCertificateRequest.getCertificateName())).build()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"" + tex.getMessage() + "\"]}");

        } catch (Exception e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Inside  Exception " +
                                    "Exception = [%s] =  Message [%s] = = certificate name = [%s]", Arrays.toString(e.getStackTrace()),
                            e.getMessage(),sslCertificateRequest.getCertificateName())).build()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body
                    ("{\"errors\":[\"" + SSLCertificateConstants.SSL_CREATE_EXCEPTION + "\"]}");
        }

        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "generateSSLCertificate").
                put(LogMessage.MESSAGE, String.format ("certificate [%s] before sending an email ",
                        sslCertificateRequest.getCertificateName())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\""+SSLCertificateConstants.SSL_CERT_SUCCESS+"\"]}");
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

         if (sslCertificateRequest.getCertType().equalsIgnoreCase("internal")) {
            //Send email for certificate creation
            sendEmail(sslCertificateRequest.getCertType(), sslCertificateRequest.getCertificateName(),
                    userDetails,
                    SSLCertificateConstants.CERT_CREATION_SUBJECT + "-" + sslCertificateRequest.getCertificateName(),
                    "created", token);
        } else {
            sendExternalEmail(sslCertificateRequest.getCertType(), sslCertificateRequest.getCertificateName(),
                    userDetails,
                    SSLCertificateConstants.EX_CERT_CREATION_SUBJECT + "-" + sslCertificateRequest.getCertificateName(),
                    "creation");
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
            if (jsonObject2.get("displayName").getAsString().toString().equals("Entrust CA")) {
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
        if (sslCertificateRequest.getCertType().equalsIgnoreCase("internal")) {
            containerId =getTargetSystemGroupId(SSLCertType.valueOf("PRIVATE_SINGLE_SAN"));
        } else  if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL) && dnsList > 0) {
            containerId =getTargetSystemGroupId(SSLCertType.valueOf("PUBLIC_MULTI_SAN"));
        } else  if (sslCertificateRequest.getCertType().equalsIgnoreCase(SSLCertificateConstants.EXTERNAL) && dnsList == 0) {
            containerId =getTargetSystemGroupId(SSLCertType.valueOf("PUBLIC_SINGLE_SAN"));
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
        String enrollTemplateCA = putTemplateParamUrl.replace("templateId", String.valueOf(templateid)).replace(
                "entityid", String.valueOf(entityId));
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
        String enrollTemplateCA = getTemplateParamUrl.replace("templateId", String.valueOf(templateid)).replace(
                "entityid", String.valueOf(entityId));
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
        String endPoint = approvalUrl.replace("actionId", String.valueOf(actionId));
        ApproveRequest approveRequest = new ApproveRequest();
        approveRequest.setFinalize(true);
        approveRequest.setNote(SSLCertificateConstants.REQUEST_FOR_APPROVAL);
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
		certificateUser.setUsername(sslCertificateRequest.getCertOwnerNtid());
		certificateUser.setAccess(TVaultConstants.SUDO_POLICY);
		certificateUser.setCertificateName(sslCertificateRequest.getCertificateName());
		certificateUser.setCertType(sslCertificateRequest.getCertType());
		
		ResponseEntity<String> addUserresponse = addUserToCertificate(certificateUser, userDetails, true);
		
		if(HttpStatus.OK.equals(addUserresponse.getStatusCode())){
			certificateUser.setAccess(TVaultConstants.WRITE_POLICY);
			ResponseEntity<String> addReadPolicyResponse = addUserToCertificate(certificateUser, userDetails, true);
			if(HttpStatus.OK.equals(addReadPolicyResponse.getStatusCode())){
				enrollResponse.setResponse(SSLCertificateConstants.SSL_CERT_SUCCESS);
				enrollResponse.setSuccess(Boolean.TRUE);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
						.put(LogMessage.ACTION, String.format("Metadata or Policies created for SSL certificate [%s] - metaDataStatus [%s] - policyStatus [%s]", sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus, isPoliciesCreated))
						.build()));
                //Send email only in case of creation
                if(operation.equalsIgnoreCase("create")) {
                    sendCreationEmail(sslCertificateRequest, userDetails, token);
                }
			    return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\""+enrollResponse.getResponse()+"\"]}");
			}else {
				enrollResponse.setResponse(SSLCertificateConstants.SSL_OWNER_PERMISSION_EXCEPTION);
	            enrollResponse.setSuccess(Boolean.FALSE);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			            put(LogMessage.ACTION, "addUserToCertificate").
			            put(LogMessage.MESSAGE, "Adding sudo permission to certificate owner failed").
			            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			            build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
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
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
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
        return directoryUser;
    }

    /**
     * @param certType
     * @param certName
     * @param userDetails
     */
    private void sendDeleteEmail(String token,String certType, String certName, UserDetails userDetails, String subject,
                                 String operation,CertificateData certData) {
        DirectoryUser directoryUser = getUserDetails(userDetails.getUsername());
        if (Objects.nonNull(directoryUser)) {
            String enrollService = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_CERT_ENROLL_STRING :
                    SSLCertificateConstants.EXTERNAL_CERT_ENROLL_STRING);

            String keyUsage = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ? SSLCertificateConstants.INTERNAL_KEY_USAGE :
                    SSLCertificateConstants.EXTERNAL_KEY_USAGE);

                 // set template variables
            Map<String, String> mailTemplateVariables = new Hashtable<>();
            mailTemplateVariables.put("name", directoryUser.getDisplayName());
            mailTemplateVariables.put("certType", StringUtils.capitalize(certType));
            mailTemplateVariables.put("certName", certName);
            mailTemplateVariables.put("contactLink", supportEmail);
            mailTemplateVariables.put("operation", operation);
            mailTemplateVariables.put("enrollService", enrollService);
            mailTemplateVariables.put("keyUsage", keyUsage);
            mailTemplateVariables.put("certStartDate", certData != null ? Objects.requireNonNull(certData).getCreateDate() : null);
            mailTemplateVariables.put("certEndDate", certData != null ? Objects.requireNonNull(certData).getExpiryDate() : null);
            emailUtils.sendHtmlEmalFromTemplateForDelete(supportEmail, directoryUser.getUserEmail(),
                    subject, mailTemplateVariables);
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "sendEmail").
                    put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   " +
                                    "for an user name =  [%s] , do Emails might not send to customer ",
                            userDetails.getUsername())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }
    /**
     * @param certType
     * @param certName
     * @param userDetails
     */
    private void sendEmail(String certType, String certName, UserDetails userDetails, String subject,
                           String operation, String token) {
        DirectoryUser directoryUser = getUserDetails(userDetails.getUsername());
        if (Objects.nonNull(directoryUser)) {
            String enrollService = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_CERT_ENROLL_STRING :
                    SSLCertificateConstants.EXTERNAL_CERT_ENROLL_STRING);

            String keyUsage = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_KEY_USAGE :
                    SSLCertificateConstants.EXTERNAL_KEY_USAGE);
            SSLCertificateMetadataDetails certMetaData = null;

            if (!StringUtils.isEmpty(token)) {
                //Get the DNS names
                certMetaData = certificateUtils.getCertificateMetaData(token, certName, certType);
            }
            // set template variables
            Map<String, String> mailTemplateVariables = new Hashtable<>();
            mailTemplateVariables.put("name", directoryUser.getDisplayName());
            mailTemplateVariables.put("certType", StringUtils.capitalize(certType));
            mailTemplateVariables.put("certName", certName);
            mailTemplateVariables.put("contactLink", supportEmail);
            mailTemplateVariables.put("operation", operation);
            mailTemplateVariables.put("enrollService", enrollService);
            mailTemplateVariables.put("keyUsage", keyUsage);
            mailTemplateVariables.put("certStartDate", certMetaData != null ? Objects.requireNonNull(certMetaData).getCreateDate() : null);
            mailTemplateVariables.put("certEndDate", certMetaData != null ? Objects.requireNonNull(certMetaData).getExpiryDate() : null);

            if(certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
            if (Objects.nonNull(Objects.requireNonNull(certMetaData).getDnsNames())) {
                //Removing first and last char from String
                mailTemplateVariables.put("dnsNames", certMetaData.getDnsNames().toString().
                        substring(1, certMetaData.getDnsNames().toString().length() - 1));
                }
            }else {
                if (Objects.nonNull(Objects.requireNonNull(certMetaData).getDnsNames())) {
                    mailTemplateVariables.put("dnsNames", certMetaData.getDnsNames().toString().
                            substring(3, certMetaData.getDnsNames().toString().length() - 3));
                }
            }

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, String.format("sendEmail for SSL certificate [%s] - certType [%s] - User " +
                                    "email=[%s] - subject = [%s]"
                            ,certName , certType,directoryUser.getUserEmail(),subject))
                    .build()));

            emailUtils.sendHtmlEmalFromTemplateForInternalCert(supportEmail, directoryUser.getUserEmail(),
                    subject, mailTemplateVariables);
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "sendEmail").
                    put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   " +
                                    "for an user name =  [%s] ,  Emails might not send to customer for an certificate = [%s]",
                            userDetails.getUsername(),certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }


    /**
     *
     * @param certType
     * @param certName
     * @param userDetails
     */
    private void sendExternalEmail(String certType, String certName, UserDetails userDetails, String subject,
                                   String operation) {
        DirectoryUser directoryUser = getUserDetails(userDetails.getUsername());
        if (Objects.nonNull(directoryUser)) {
            String enrollService = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_CERT_ENROLL_STRING :
                    SSLCertificateConstants.EXTERNAL_CERT_ENROLL_STRING);

            String keyUsage = (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL) ?
                    SSLCertificateConstants.INTERNAL_KEY_USAGE :
                    SSLCertificateConstants.EXTERNAL_KEY_USAGE);
            // set template variables
            Map<String, String> mailTemplateVariables = new Hashtable<>();
            mailTemplateVariables.put("name", directoryUser.getDisplayName());
            mailTemplateVariables.put("certType", StringUtils.capitalize(certType));
            mailTemplateVariables.put("certName", certName);
            mailTemplateVariables.put("contactLink", supportEmail);
            mailTemplateVariables.put("operation", operation);
            mailTemplateVariables.put("enrollService", enrollService);
            mailTemplateVariables.put("keyUsage", keyUsage);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
                    .put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
                    .put(LogMessage.ACTION, String.format("sendEmail for SSL certificate [%s] - certType [%s] - User " +
                                    "email=[%s] - subject = [%s]"
                            ,certName , certType,directoryUser.getUserEmail(),subject))
                    .build()));
            emailUtils.sendEmailForExternalCert(supportEmail, directoryUser.getUserEmail(),
                    subject, mailTemplateVariables);
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "sendEmail").
                    put(LogMessage.MESSAGE, String.format("Unable to get the Directory User details   " +
                                    "for an user name =  [%s] ,  Emails might not send to customer for an certificate = [%s]",
                            userDetails.getUsername(),certName)).
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
            CertManagerLogin certManagerLogin) throws Exception {
		
		String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase("internal"))?
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

        if(sslCertificateRequest.getCertType().equalsIgnoreCase("internal")) {
        CertificateData certDetails = null;
        //Get Certificate Details
        for (int i = 1; i <= retrycount; i++) {
            Thread.sleep(delayTime);
            certDetails = getCertificate(sslCertificateRequest, certManagerLogin);
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
            sslCertificateMetadataDetails.setCertificateName(certDetails.getCertificateName());
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
                            "certificate = [%s]", sslCertificateRequest.getCertificateName())).
                    build()));
        }
        sslCertificateMetadataDetails.setCertCreatedBy(userDetails.getUsername());
        sslCertificateMetadataDetails.setCertOwnerEmailId(sslCertificateRequest.getCertOwnerEmailId());
        sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
        sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());
        sslCertificateMetadataDetails.setContainerId(containerId);

        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, String.format("MetaData info details = [%s]", sslCertificateMetadataDetails.toString())).
                build()));
        } else {
            sslCertificateMetadataDetails.setCertCreatedBy(userDetails.getUsername());
            sslCertificateMetadataDetails.setCertOwnerEmailId(sslCertificateRequest.getCertOwnerEmailId());
            sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
            sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());
            sslCertificateMetadataDetails.setContainerId(containerId);
            sslCertificateMetadataDetails.setCertificateName(sslCertificateRequest.getCertificateName());
            sslCertificateMetadataDetails.setRequestStatus(SSLCertificateConstants.REQUEST_PENDING_APPROVAL);
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
     * Validate the DNSNames
     * @param sslCertificateRequest
     * @return
     */
    private boolean validateDNSNames(SSLCertificateRequest sslCertificateRequest) {
        String[] dnsNames = sslCertificateRequest.getDnsList();
        Set<String> set = new HashSet<>();
        if(!ArrayUtils.isEmpty(dnsNames)) {
	        for (String dnsName : dnsNames) {
	            if (dnsName.contains(" ") || (!dnsName.matches("^[a-zA-Z0-9.-]+$")) || (dnsName.endsWith(certificateNameTailText)) ||
	                    (dnsName.contains(".-")) || (dnsName.contains("-.")) || (dnsName.contains("..")) || (dnsName.endsWith(".")) ||
	                    (!set.add(dnsName))) {
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
    private boolean validateInputData(SSLCertificateRequest sslCertificateRequest, UserDetails userDetails){
        boolean isValid=true;
        if((!validateCertficateName(sslCertificateRequest.getCertificateName())) || sslCertificateRequest.getAppName().contains(" ") ||
                (!populateCertOwnerEmaild(sslCertificateRequest, userDetails)) ||
                sslCertificateRequest.getCertOwnerEmailId().contains(" ") ||  sslCertificateRequest.getCertType().contains(" ") ||
                sslCertificateRequest.getTargetSystem().getAddress().contains(" ") ||
                (!sslCertificateRequest.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING)) ||
                (!isValidHostName(sslCertificateRequest.getTargetSystemServiceRequest().getHostname()))
                || (!isValidAppName(sslCertificateRequest)) || (!validateDNSNames(sslCertificateRequest))){
            isValid= false;
        }

        return isValid;
    }

	/**
	 * Method to validate the certificate name
	 *
	 * @param certName
	 * @return
	 */
	private boolean validateCertficateName(String certName) {
		boolean isValid = true;
		if (certName.contains(" ") || (certName.endsWith(certificateNameTailText)) || (certName.contains(".-"))
				|| (certName.contains("-.")) || (certName.contains("..")) || (certName.endsWith("."))) {
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Method to append t-mobile.com text to certificate name and dns
	 *
	 * @param sslCertificateRequest
	 */
	private void appendTmobileTextToCertificateName(SSLCertificateRequest sslCertificateRequest) {
		String certName = sslCertificateRequest.getCertificateName() + certificateNameTailText;
		sslCertificateRequest.setCertificateName(certName);

		String[] dnsNames = sslCertificateRequest.getDnsList();

		if (!ArrayUtils.isEmpty(dnsNames)) {
			String[] dnsArray = Arrays.stream(dnsNames).map(value -> value + certificateNameTailText)
					.toArray(String[]::new);
			sslCertificateRequest.setDnsList(dnsArray);
		}
	}

    private boolean isValidAppName(SSLCertificateRequest sslCertificateRequest){
        boolean isValidApp=false;
        ResponseEntity<String> appResponse =
                workloadDetailsService.getWorkloadDetailsByAppName(sslCertificateRequest.getAppName());
        if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
            isValidApp=true;
        }
        return isValidApp;
    }


    /**
     * To Validate the hostname when it's not null/empty
     * @param hostname
     * @return
     */
    private boolean isValidHostName(String hostname){
        if(!StringUtils.isEmpty(hostname)){
            String regex = "^[a-zA-Z0-9.-]+$";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(hostname);
            return m.matches();
        }
        return true;
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
        String certificateName = sslCertificateRequest.getCertificateName();
        
        String metaDataPath = (sslCertificateRequest.getCertType().equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;

		String certPathVal = (sslCertificateRequest.getCertType().equalsIgnoreCase("internal"))?
				SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

        String policyValue=(sslCertificateRequest.getCertType().equalsIgnoreCase("internal"))?
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

	        //Write Policy
	        accessMap.put(certPath , TVaultConstants.WRITE_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX +policyValue+"_" + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response writeResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);

	        //Deny Policy
	        accessMap.put(certPath , TVaultConstants.DENY_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.DENY_CERT_POLICY_PREFIX +policyValue+"_" + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response denyResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);

	        //Owner Policy
	        accessMap.put(certPath , TVaultConstants.SUDO_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX +policyValue+"_" + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response sudoResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);

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
	                    put(LogMessage.MESSAGE, "SSL Certificate Policies Creation Success").
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
	            policiesCreated = true;
	        } else {
	            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, "SSL Certificate policies creation failed").
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
    	sslCertificateMetadataDetails.setCertificateName(sslCertificateRequest.getCertificateName());
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
        String certName = sslCertificateRequest.getCertificateName();
        int containerId = getContainerId(sslCertificateRequest);
        String findCertificateEndpoint = "/certmanager/findCertificate";
        String targetEndpoint = findCertificate.replace("certname", String.valueOf(certName)).replace("cid", String.valueOf(containerId));
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
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(findTargetSystemEndpoint));
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
        String enrollTemplateCA = enrollCSRUrl.replace("templateId", String.valueOf(templateid)).replace("entityid", String.valueOf(entityid));
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
            if (jsonObject2.get(SSLCertificateConstants.TYPENAME).getAsString().toString().equals(SSLCertificateConstants.CN)) {
                JsonArray jsonArray2 = jsonElement.getAsJsonObject().getAsJsonArray(SSLCertificateConstants.VALUE);
                for (int j = 0; j < jsonArray2.size(); j++) {
                    JsonElement jsonElement1 = jsonArray2.get(j);
                    jsonObject2 = jsonElement1.getAsJsonObject();
                    jsonObject2.addProperty(SSLCertificateConstants.VALUE, sslCertificateRequest.getCertificateName());
                    break;
                }
            }
            break;
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
        String enrollTemplateCA = enrollUpdateCSRUrl.replace("entityid", String.valueOf(entityid));
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
        String enrollTemplateCA = enrollKeysUrl.replace("templateId", String.valueOf(templateid)).replace("entityid", String.valueOf(entityid));
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
        String enrollTemplateCA = enrollKeysUrl.replace("templateId", String.valueOf(templateid)).replace("entityid", String.valueOf(entityid));
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
        String enrollTempletEndpoint = enrollTemplateUrl.replace("caid", String.valueOf(caId)).replace("entityid", String.valueOf(entityid));
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
    private CertResponse getEnrollTemplates(CertManagerLogin certManagerLogin, int entityid, int caId) throws Exception {
        String enrollEndPoint = "/certmanager/getEnrollTemplates";
        String enrollTemplateCA = enrollTemplateUrl.replace("caid", String.valueOf(caId)).replace("entityid", String.valueOf(entityid));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
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
        String enrollCA = enrollCAUrl.replace("entityid", String.valueOf(entityid));
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
        String enrollCA = enrollCAUrl.replace("entityid", String.valueOf(entityid));
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
        String targetSystemEndPoint = enrollUrl.replace("entityid", String.valueOf(entityId));
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
                put(LogMessage.ACTION, "createTargetSystem").
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
    public ResponseEntity<String> getServiceCertificates(String token, UserDetails userDetails, String certName, Integer limit, Integer offset, String certType) throws Exception {
    	if(!certType.matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING)){
    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getServiceCertificates")
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
    	}
    	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                  put(LogMessage.ACTION, "getServiceCertificates").
   			      put(LogMessage.MESSAGE, String.format("Trying to get list of Ssl certificatests")).
   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
   			      build()));
        String metaDataPath = (certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;
       	Response response = new Response();
       	String certListStr = "";
       	String tokenValue= (userDetails.isAdmin())? token :userDetails.getSelfSupportToken();

        response = getMetadata(tokenValue, metaDataPath);
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            certListStr = getsslmetadatalist(response.getResponse(),tokenValue,userDetails,certName,limit,offset,metaDataPath);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getServiceCertificates").
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
                  put(LogMessage.MESSAGE, "Failed to get certificate list from metadata").
   			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
   			      build()));

   		return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
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
        JsonObject jsonObject = (JsonObject) jsonParser.parse(certificateResponse);
   		JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
   		List<String> certNames = geMatchCertificates(jsonArray,certName); 		
   		if(limit == null || offset ==null) {
   			limit = certNames.size();
   			offset = 0;
   		}
   		
		if (!userDetails.isAdmin()) {
			responseArray = getMetadataForUser(certNames, userDetails,path,limit,offset);
		} else {
			int maxVal = certNames.size()> (limit+offset)?limit+offset : certNames.size();
			for (int i = offset; i < maxVal; i++) {
				endPoint = certNames.get(i).replaceAll("^\"+|\"+$", "");
				pathStr = path + "/" + endPoint;
				response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);
				if (HttpStatus.OK.equals(response.getHttpstatus())) {
					responseArray.add(((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data"));
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
   		metadataJsonObj.addProperty("offset", offset);
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
						list.add(jsonArray.get(i).toString());
					}
				}
			} else {
				for (int i = 0; i < jsonArray.size(); i++) {
					list.add(jsonArray.get(i).toString());
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
			endPoint = certNames.get(i).replaceAll("^\"+|\"+$", "");
			pathStr = path + "/" + endPoint;
			response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}",
					userDetails.getSelfSupportToken());
			if (HttpStatus.OK.equals(response.getHttpstatus()) && !ObjectUtils.isEmpty(response.getResponse())) {
				JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
				if (userDetails.getUsername().equalsIgnoreCase(
						(object.get("certOwnerNtid") != null ? object.get("certOwnerNtid").getAsString() : ""))) {
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
            return certManagerLogin.getAccess_token();
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
					.put(LogMessage.ACTION, "getTargetSystemList")
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
    	}
        String getTargetSystemEndpoint = "/certmanager/findTargetSystem";
        SSLCertType sslCertType = certType.equalsIgnoreCase("internal")?
                SSLCertType.valueOf("PRIVATE_SINGLE_SAN"): SSLCertType.valueOf("PUBLIC_SINGLE_SAN");
        String findTargetSystemEndpoint = findTargetSystem.replace("tsgid",
                String.valueOf(getTargetSystemGroupId(sslCertType)));

        List<TargetSystemDetails> targetSystemDetails = new ArrayList<>();
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", getNclmToken(),
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
                        put(LogMessage.ACTION, "getTargetSystemList").
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
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getTargetSystemList").
                put(LogMessage.MESSAGE, "Failed to get Target system list from NCLM").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system list from NCLM\"]}");

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
                        put(LogMessage.ACTION, "getTargetSystemServiceList").
                        put(LogMessage.MESSAGE, "Successfully retrieved target system service list from NCLM").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemServiceDetails)+"}");
            }
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getTargetSystemServiceList").
                    put(LogMessage.MESSAGE, "Retrieved empty target system service list from NCLM").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemServiceDetails)+"}");
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getTargetSystemServiceList").
                put(LogMessage.MESSAGE, String.format("Failed to get Target system service list from NCLM for the target system [%s]", targetSystemId)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system service list from NCLM\"]}");

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
					.put(LogMessage.ACTION, "Fetch Revocation Reasons")
					.put(LogMessage.MESSAGE,
							String.format("Trying to fetch Revocation Reasons for [%s]", certificateId))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			String nclmAccessToken = getNclmToken();

			String nclmGetCertificateReasonsEndpoint = getCertifcateReasons.replace("certID", certificateId.toString());
			revocationReasons = reqProcessor.processCert("/certificates/revocationreasons", certificateId,
					nclmAccessToken, getCertmanagerEndPoint(nclmGetCertificateReasonsEndpoint));
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Fetch Revocation Reasons")
					.put(LogMessage.MESSAGE, "Fetch Revocation Reasons")
					.put(LogMessage.STATUS, revocationReasons.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(revocationReasons.getHttpstatus()).body(revocationReasons.getResponse());
		} catch (TVaultValidationException error) {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION,
									String.format(
											"Inside  TVaultValidationException " + "Exception = [%s] =  Message [%s]",
											Arrays.toString(error.getStackTrace()), error.getMessage()))
							.build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"" + "Certificate unavailable in NCLM." + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]", 
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"" + SSLCertificateConstants.SSL_CERTFICATE_REASONS_FAILED + "\"]}");
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
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}

		Map<String, String> metaDataParams = new HashMap<String, String>();

		String endPoint = certificateName;
		String metaDataPath = (certType.equalsIgnoreCase("internal"))?
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
					.body("{\"errors\":[\"" + "Certificate unavailable" + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"errors\":[\"" + "Certificate unavailable" + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);

		if (!userDetails.isAdmin()) {

//			Boolean isPermission = validateOwnerPermissionForNonAdmin(userDetails, certificateName);
			Boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certificateName,certType);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("{\"errors\":[\"" + "Access denied: no permission to revoke certificate" + "\"]}");
			}
		}
		String certID = object.get("certificateId").getAsString();
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

			String nclmAccessToken = getNclmToken();

			String nclmApiIssueRevocationEndpoint = issueRevocationRequest.replace("certID",
					String.valueOf(certificateId));
			revocationResponse = reqProcessor.processCert("/certificates/revocationrequest", revocationRequest,
					nclmAccessToken, getCertmanagerEndPoint(nclmApiIssueRevocationEndpoint));
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Issue Revocation Request")
					.put(LogMessage.MESSAGE, String.format("Issue Revocation Request for [%s] requested by [%s] on [%s]", certificateName,userDetails.getUsername(),LocalDateTime.now()))
					.put(LogMessage.STATUS, revocationResponse.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			boolean sslMetaDataUpdationStatus;
			metaDataParams.put("certificateStatus", "Revoked");
			if (userDetails.isAdmin()) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(metaDataPath, metaDataParams, token);
			} else {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(metaDataPath, metaDataParams,
						userDetails.getSelfSupportToken());
			}
			if (sslMetaDataUpdationStatus) {
			    //Send an email for revoke for internal and external
                sendEmail(certType, certificateName, userDetails, SSLCertificateConstants.CERT_REVOKED_SUBJECT + "-" + certificateName,
                        "revoked", token);
				return ResponseEntity.status(revocationResponse.getHttpstatus())
						.body("{\"messages\":[\"" + "Revocation done successfully" + "\"]}");
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "Revocation Request Failed")
						.put(LogMessage.MESSAGE, "Revocation Request failed for CertificateID")
						.put(LogMessage.STATUS, revocationResponse.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"" + "Revocation failed" + "\"]}");
			}

		} catch (TVaultValidationException error) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  TVaultValidationException  = [%s] =  Message [%s]", 
							Arrays.toString(error.getStackTrace()), error.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"" + error.getMessage() + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"" + e.getMessage() + "\"]}");
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
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
   		}
   		
   		String userName = certificateUser.getUsername().toLowerCase();
   		String certificateName = certificateUser.getCertificateName().toLowerCase();
   		String access = certificateUser.getAccess().toLowerCase();   
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
   			return checkUserDetailsAndAddCertificateToUser(authToken, userName, certificateName, access, certificateType);	
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
			String certificateName, String access, String certificateType) {
		
		String policyPrefix = getCertificatePolicyPrefix(access, certificateType);
		
		String metaDataPath = (certificateType.equalsIgnoreCase("internal"))?
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
		String certPrefix=(certificateType.equalsIgnoreCase("internal"))?
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
		Response userResponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);	
		}
		else {
			userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
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
				currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				if (!(TVaultConstants.USERPASS.equals(vaultAuthMethod))) {
					groups =objMapper.readTree(responseJson).get("data").get("groups").asText();
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
				policiesString, currentpoliciesString);
		
	}

	private String getCertificatePolicyPrefix(String access, String certificateType) {
		String policyPrefix ="";
		String certPrefix=(certificateType.equalsIgnoreCase("internal"))?
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
			String certificatePath, String access, String groups, String policiesString, String currentpoliciesString) {
		Response ldapConfigresponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,policiesString,token);
		}
		else {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
		}

		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
			return updateMetadataForAddUserToCertificate(token, userName, certificatePath, access, groups,
					currentpoliciesString);		
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
			String certificatePath, String access, String groups, String currentpoliciesString) {
		Response ldapConfigresponse;		
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
					put(LogMessage.MESSAGE, String.format ("User is successfully associated with Certificate [%s] - User [%s]", certificatePath, userName)).
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
			else {
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,currentpoliciesString,groups,token);
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
   		if(!ControllerUtil.arecertificateGroupInputsValid(certificateGroup)) {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
	   					put(LogMessage.MESSAGE, "Invalid input values").
	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	   					build()));
   			
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			isAuthorized=isAuthorized(userDetails, certificateGroup.getCertificateName(), certificateGroup.getCertType());
   			if(isAuthorized){
   	   			return addingGroupToCertificate(authToken, certificateGroup);
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
	public ResponseEntity<String> addingGroupToCertificate(String token, CertificateGroup certificateGroup) {
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
		String groupName = certificateGroup.getGroupname().toLowerCase();
		String certificateName = certificateGroup.getCertificateName().toLowerCase();
		String access = certificateGroup.getAccess().toLowerCase();		
		String certType = certificateGroup.getCertType().toLowerCase();
		
		String metaDataPath = (certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH;

		String certPathVal = (certType.equalsIgnoreCase("internal"))?
				SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;

        String policyValue=(certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
        
        String certPath = certPathVal + certificateName;
		
		boolean canAddGroup = ControllerUtil.canAddCertPermission(metaDataPath, certificateName, token);
		String policyPrefix = getCertificatePolicyPrefix(access, certType);
		if(canAddGroup){
			String policy = policyPrefix + certificateName;
			
			String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+policyValue+"_"+certificateName;
			String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+policyValue+"_"+certificateName;
			String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+policyValue+"_"+certificateName;
			
			Response getGrpResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
			String responseJson="";

			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(getGrpResp.getHttpstatus())){
				responseJson = getGrpResp.getResponse();
				try {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
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
			Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,policiesString,token);

			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				return updateMetadataForAddGroupToCertificate(token, groupName, certificateName, access, certPath,
						currentpoliciesString);
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "Group configuration failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Contact Admin \"]}");
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
			String certificateName, String access, String certPath, String currentpoliciesString) {		
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
					put(LogMessage.MESSAGE, "Group configuration Success.").
					put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
		}else{
			return revertPoliciesIfMetadataUpdateFailed(token, groupName, currentpoliciesString, metadataResponse);
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
			String currentpoliciesString, Response metadataResponse) {
		Response ldapConfigresponse;
		ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,currentpoliciesString,token);
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
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
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
        if (approleName.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
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
		
		String metaDataPath = (certType.equalsIgnoreCase("internal"))?
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
		
		String certPrefix=(certType.equalsIgnoreCase("internal"))?
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

		    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Non existing role name. Please configure approle as first step\"]}");
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
					put(LogMessage.MESSAGE, String.format("Approle [%s] successfully associated with Certificate [%s]", approleName, certificatePath)).
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
		String certPrefix=(certType.equalsIgnoreCase("internal"))?
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
        return certificateUtils.getCertificateMetaData(token, certificateName, "internal");
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
        SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token, certName, "internal");
        if (hasDownloadPermission(certificateDownloadRequest.getCertificateName(), userDetails, "internal") && sslCertificateMetadataDetails!= null) {
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

        String nclmToken = getNclmToken();
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
    public ResponseEntity<InputStreamResource> downloadCertificate(String token, UserDetails userDetails, String certificateName, String certificateType) {

        InputStreamResource resource = null;
        SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token, certificateName, "internal");
        if (hasDownloadPermission(certificateName, userDetails, "internal") && sslCertificateMetadataDetails != null) {

            String nclmToken = getNclmToken();
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
                        put(LogMessage.MESSAGE, String.format ("Failed to download certificate")).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
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

		SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token,
				certificateName, certificateType);
		if (sslCertificateMetadataDetails != null) {
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(sslCertificateMetadataDetails));
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body("{\"errors\":[\"Access denied: Unable to read certificate details.\"]}");
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
	public ResponseEntity<String> renewCertificate(String certType, String certificateName, UserDetails userDetails, String token) {

		Map<String, String> metaDataParams = new HashMap<String, String>();
		if (!isValidInputs(certificateName, certType)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "renewCertificate")
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		String endPoint = certificateName;
		String metaDataPath = (certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + endPoint;
		Response response = new Response();
		if (!userDetails.isAdmin()) {
//			Boolean isPermission = validateOwnerPermissionForNonAdmin(userDetails, certificateName);
			Boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certificateName,certType);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("{\"errors\":[\""
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
					.body("{\"messages\":[\"" + "Certficate unavailable" + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"errors\":[\"" + "Certficate unavailable" + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);		
		
		String certID = object.get("certificateId").getAsString();
		int containerId = object.get("containerId").getAsInt();
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

			String nclmAccessToken = getNclmToken();

			String nclmApiRenewEndpoint = renewCertificateEndpoint.replace("certID", String.valueOf(certificateId));
			renewResponse = reqProcessor.processCert("/certificates/renew", "",
					nclmAccessToken, getCertmanagerEndPoint(nclmApiRenewEndpoint));
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
			CertificateData certData = getLatestCertificate(certificateName,nclmAccessToken, containerId);			
			boolean sslMetaDataUpdationStatus=true;		
			if(!ObjectUtils.isEmpty(certData)) {
			metaDataParams.put("certificateId",((Integer)certData.getCertificateId()).toString()!=null?
					((Integer)certData.getCertificateId()).toString():String.valueOf(certificateId));
			metaDataParams.put("createDate", certData.getCreateDate()!=null?certData.getCreateDate():object.get("createDate").getAsString());
			metaDataParams.put("expiryDate", certData.getExpiryDate()!=null?certData.getExpiryDate():object.get("expiryDate").getAsString());			
			metaDataParams.put("certificateStatus", certData.getCertificateStatus()!=null?certData.getCertificateStatus():
				object.get("certificateStatus").getAsString());
			
			if(certType.equalsIgnoreCase("external")) {
				CertManagerLogin certManagerLogin = new CertManagerLogin();
				certManagerLogin.setAccess_token(nclmAccessToken);
				Map<String, Object> responseMap = ControllerUtil.parseJson(renewResponse.getResponse());
                if (!MapUtils.isEmpty(responseMap) && responseMap.get("actionId") != null) {
                    int actionId = (Integer) responseMap.get("actionId");
                if (actionId != 0) {
                	renewResponse = approvalRequest(certManagerLogin, actionId);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, String.format("approvalRequest Completed Successfully [%s]" +
                                    " = certificate name = [%s]", renewResponse.getResponse(),certificateName)).
                            build()));
                }
			}
                metaDataParams.put("certificateStatus", SSLCertificateConstants.RENEW_PENDING); 
			}
			if (userDetails.isAdmin()) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(metaDataPath, metaDataParams, token);
			} else {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(metaDataPath, metaDataParams,
						userDetails.getSelfSupportToken());
			}
			}
			if (sslMetaDataUpdationStatus) {
                //Sending renew email
                sendRenewEmail(certType, certificateName, userDetails, token);
				return ResponseEntity.status(renewResponse.getHttpstatus())
						.body("{\"messages\":[\"" + "Certificate Renewed Successfully" + "\"]}");
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
						.body("{\"errors\":[\"" + "Metadata updation Failed." + "\"]}");
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
						.body("{\"errors\":[\"" + "Certificate Renewal Failed" + "\"]}");
			}

		} catch (TVaultValidationException error) {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, String.format("Inside  TVaultValidationException  = [%s] =  Message [%s]",
									Arrays.toString(error.getStackTrace()), error.getMessage()))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"" + error.getMessage() + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"" + e.getMessage() + "\"]}");
		}
	}

    /**
     * This method will be responsible for sending an email for renew certificate for both internal and external
     * @param certType
     * @param certificateName
     * @param userDetails
     * @param token
     */
    private void sendRenewEmail(String certType, String certificateName, UserDetails userDetails, String token) {
        //Send email for certificate renewed for internal and external
        if (certType.equalsIgnoreCase(SSLCertificateConstants.INTERNAL)) {
            sendEmail(certType, certificateName, userDetails, SSLCertificateConstants.CERT_RENEW_SUBJECT + " - " + certificateName,
                    "renewed", token);
        } else {
            sendExternalEmail(certType, certificateName, userDetails, SSLCertificateConstants.EX_CERT_RENEW_SUBJECT + " - " +
                    certificateName, "renew");
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
        String targetEndpoint = findCertificate.replace("certname", String.valueOf(certName)).replace("cid", String.valueOf(containerId));
        CertResponse response = reqProcessor.processCert(findCertificateEndpoint, "", accessToken, getCertmanagerEndPoint(targetEndpoint));        
        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.CERTIFICATES) != null)) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
                LocalDateTime  createdDate = null ;
                LocalDateTime  certCreatedDate;
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    if(i==0) {
                    createdDate = LocalDateTime.parse(validateString(jsonElement.get("NotBefore")).substring(0, 19));
                    }else if (i>0) {
                    	createdDate = LocalDateTime.parse(validateString(jsonArray.get(i-1).getAsJsonObject().get("NotBefore")).substring(0, 19));
                    }
                    if ((Objects.equals(getCertficateName(jsonElement.get("sortedSubjectName").getAsString()), certName))) {
                    	certCreatedDate = LocalDateTime.parse(validateString(jsonElement.get("NotBefore")).substring(0, 19));
                    	if(!ObjectUtils.isEmpty(createdDate) && (createdDate.isBefore(certCreatedDate) || createdDate.isEqual(certCreatedDate))) {
                        certificateData= new CertificateData();
                        certificateData.setCertificateId(Integer.parseInt(jsonElement.get("certificateId").getAsString()));
                        certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
                        certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));                       
                        certificateData.setCertificateStatus(validateString(jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS)));
                        certificateData.setCertificateName(certName);
                        certificateData.setDeployStatus(getTargetSystemServiceIds(jsonElement.getAsJsonArray("targetSystemServiceIds")));                      
                    	}
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
   					put(LogMessage.MESSAGE, "Invalid user inputs").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
   		}
		
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
			return checkUserPolicyAndRemoveFromCertificate(userName, certificateName, authToken, certificateType);	
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
	 * @param userName
	 * @param certificateName
	 * @param authToken
	 * @return
	 */
	private ResponseEntity<String> checkUserPolicyAndRemoveFromCertificate(String userName, String certificateName,
			String authToken, String certificateType) {
		
		String certPrefix=(certificateType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		
		String metaDataPath = (certificateType.equalsIgnoreCase("internal"))?
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
		
		Response userResponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}", authToken);	
		}
		else {
			userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}", authToken);
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
				currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				if (!(TVaultConstants.USERPASS.equals(vaultAuthMethod))) {
					groups =objMapper.readTree(responseJson).get("data").get("groups").asText();
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
		Response ldapConfigresponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, authToken);
		}
		else {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, authToken);
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
			return updateMetadataForRemoveUserFromCertificate(userName, certificatePath, authToken, groups,
					currentpoliciesString);
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
	 * @param userName
	 * @param certificateName
	 * @param authToken
	 * @param groups
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForRemoveUserFromCertificate(String userName, String certificatePath,
			String authToken, String groups, String currentpoliciesString) {
		Response ldapConfigresponse;
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
					put(LogMessage.MESSAGE, "User is successfully Removed from Certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully removed user from the certificate\"]}");
		} else {
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, currentpoliciesString, authToken);
			}
			else {
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString, groups, authToken);
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
   					put(LogMessage.MESSAGE, "Invalid user inputs").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
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
   			
   			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
   			
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
        	return checkPolicyDetailsAndRemoveGroupFromCertificate(groupName, certificateName, authToken, certificateType);
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
	 * Method to check the group policy and remove the group from certificate
	 * @param groupName
	 * @param certificateName
	 * @param authToken
	 * @return
	 */
	private ResponseEntity<String> checkPolicyDetailsAndRemoveGroupFromCertificate(String groupName,
			String certificateName, String authToken, String certificateType) {
		String certPrefix=(certificateType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		
		String metaDataPath = (certificateType.equalsIgnoreCase("internal"))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		
		String certificatePath = metaDataPath + certificateName;
		
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certPrefix+"_"+certificateName;
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		Response groupResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}", authToken);

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		        put(LogMessage.MESSAGE, String.format ("Group Response status is [%s]", groupResp.getHttpstatus())).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		
		if(HttpStatus.OK.equals(groupResp.getHttpstatus())){
		    responseJson = groupResp.getResponse();
		    try {
		        ObjectMapper objMapper = new ObjectMapper();
		        currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
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
		
		Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, authToken);

		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){

			return updateMetadataForRemoveGroupFromCertificate(groupName, certificatePath, authToken,
					currentpoliciesString);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		            put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		            put(LogMessage.MESSAGE, "Failed to remove the group from the certificate").
		            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		            build()));
		    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the group from the certificate\"]}");
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
			String authToken, String currentpoliciesString) {
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
					metadataResponse);
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
			String currentpoliciesString, Response metadataResponse) {
		Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString, authToken);
		
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
	 * 
	 * @param token
	 * @param certificateType
	 * @return
	 * @throws Exception
	 */
	public ResponseEntity<String> getListOfCertificates(String token, String certificateType) {
		Response response;
		String path = "";
		if(!certificateType.matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING)){
    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getListOfCertificates")
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
    	}
		if (certificateType.equalsIgnoreCase("internal")) {
			path = SSLCertificateConstants.SSL_CERT_PATH_VALUE;
		} else {
			path = SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		}
		response = getMetadata(token, path);

		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getListOfCertificates")
					.put(LogMessage.MESSAGE, "Certificates fetched from metadata")
					.put(LogMessage.STATUS, response.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "getListOfCertificates")
					.put(LogMessage.MESSAGE, "Failed to get certificate list from metadata")
					.put(LogMessage.STATUS, response.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));

			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	
	/**
     * To update the owner of an existing certificate.
     * @param token
     * @param userDetails
     * @return
     * @throws Exception
     */
    public ResponseEntity<String> updateCertOwner(String token, String certType,String certName,String certOwnerEmailId,  UserDetails userDetails) throws Exception {
    	Map<String, String> metaDataParams = new HashMap<String, String>();
    	Map<String, String> dataMetaDataParams = new HashMap<String, String>();
    	SSLCertificateRequest certificateRequest = new SSLCertificateRequest();
    	boolean isValidEmail = true;
    	String certOwnerNtId ="";
    	Object[] users = null;
    	DirectoryUser dirUser = new DirectoryUser();
    	if (!isValidInputs(certName, certType)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "transferCertificate")
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
    	ResponseEntity<DirectoryObjects> userResponse = directoryService.searchByUPN(certOwnerEmailId);
    	if(userResponse.getStatusCode().equals(HttpStatus.OK)) {
    		 users = userResponse.getBody().getData().getValues();
    		 if(!ObjectUtils.isEmpty(users)) {
    		 dirUser = (DirectoryUser) users[0];
    		 certOwnerNtId = dirUser.getUserName();
    		 }
    	}   
    	
    	if(certOwnerNtId==""){
    		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "updateCertOwner")
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
    	}
       
		String endPoint = certName;	
		CertResponse enrollResponse = new CertResponse();		
		String metaDataPath = (certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + endPoint;		
		String permissionMetaDataPath = (certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH_VALUE  + endPoint :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT  + endPoint;
		Response response = new Response();
		Response dataResponse = new Response();
		if (!userDetails.isAdmin()) {
			Boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certName, certType);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("{\"errors\":[\""
								+ "Access denied: No permission to transfer the ownership of this certificate"
								+ "\"]}");
			}
		}
		try {
			if (userDetails.isAdmin()) {
				response = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", token);
				dataResponse = reqProcessor.process("/read", "{\"path\":\"" + permissionMetaDataPath + "\"}", token);
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
					.body("{\"messages\":[\"" + "Certificate unavailable" + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"errors\":[\"" + "Certificate unavailable" + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		ObjectMapper objMapper = new ObjectMapper();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		JsonObject dataObject = ((JsonObject) jsonParser.parse(dataResponse.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);	
		
		if(certOwnerEmailId.equalsIgnoreCase(metaDataParams.get("certOwnerEmailId")))	{
			isValidEmail=false;
		}
				
		if(dataObject!=null) {
		dataMetaDataParams = new Gson().fromJson(dataObject.toString(), Map.class);	
		if(certOwnerEmailId.equalsIgnoreCase(dataMetaDataParams.get("certOwnerEmailId")))	{
			isValidEmail=false;
		}
		dataMetaDataParams.put("certOwnerNtid", certOwnerNtId);
		dataMetaDataParams.put("certOwnerEmailId", certOwnerEmailId);
		}
		
		if(!isValidEmail) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\""
							+ "New owner email id should not be same as owner email id"
							+ "\"]}");
		}		
		
		String certificateUser = metaDataParams.get("certOwnerNtid");
		boolean sslMetaDataUpdationStatus;			
		metaDataParams.put("certOwnerEmailId", certOwnerEmailId);
		metaDataParams.put("certOwnerNtid", certOwnerNtId);
		
		certificateRequest.setCertificateName(certName);
		certificateRequest.setCertType(certType);
		certificateRequest.setCertOwnerEmailId(certOwnerEmailId);
		certificateRequest.setCertOwnerNtid(certOwnerNtId);
		
		try {
		if (userDetails.isAdmin()) {
			sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(metaDataPath, metaDataParams, token);
			if(dataObject!=null) {
			sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(permissionMetaDataPath, dataMetaDataParams, token);
			}
		} else {
			sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(metaDataPath, metaDataParams,
					userDetails.getSelfSupportToken());	
			if(dataObject!=null) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(permissionMetaDataPath, dataMetaDataParams, userDetails.getSelfSupportToken());
				}
		}
		if (sslMetaDataUpdationStatus) {
			boolean isPoliciesCreated=true;	
			removeSudoPermissionForPreviousOwner( certificateUser.toLowerCase(), certName,userDetails,certType);
			addSudoPermissionToCertificateOwner(certificateRequest, userDetails, enrollResponse, isPoliciesCreated, true,token,"transfer");		
			
			return ResponseEntity.status(HttpStatus.OK)
					.body("{\"messages\":[\"" + "Certificate owner Transferred Successfully" + "\"]}");
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "updateCertOwner")
					.put(LogMessage.MESSAGE, "Certificate owner Transfer failed for CertificateID")
					.put(LogMessage.STATUS, HttpStatus.INTERNAL_SERVER_ERROR.toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"" + "Certificate owner Transfer failed" + "\"]}");
		}
	
	} catch (Exception e) {
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
				.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
						Arrays.toString(e.getStackTrace()), e.getMessage()))
				.build()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"" + e.getMessage() + "\"]}");
	}

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
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		String metaDataPath = (certType.equalsIgnoreCase("internal")) ? SSLCertificateConstants.SSL_CERT_PATH
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
				return getCertificateDetailsAndProcessMetadata(certificatePath, authToken, certificateMetaData);
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
	 * @param certificatePath
	 * @param authToken
	 * @param certificateMetaData
	 * @return
	 */
	private ResponseEntity<String> getCertificateDetailsAndProcessMetadata(String certificatePath, String authToken,
			SSLCertificateMetadataDetails certificateMetaData) {
		try {
			CertificateData certificateData = getExternalCertificate(certificateMetaData);
			if(!ObjectUtils.isEmpty(certificateData)) {
				return processCertificateDataAndUpdateMetadata(certificatePath, authToken, certificateMetaData,
						certificateData);
			}else {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG).
						put(LogMessage.MESSAGE, "Certificate may not be approved or rejected from NCLM").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.NO_CONTENT).body("{\"messages\":[\"Certificate may not be approved or rejected from NCLM \"]}");
			}
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.VALIDATE_CERTIFICATE_DETAILS_MSG).
					put(LogMessage.MESSAGE, "Certificate may not be approved or rejected from NCLM").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Certificate may not be approved or rejected from NCLM\"]}");
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
				metaDataParams.put("requestStatus", "Approved");
			}
		}else {
			metaDataParams.put("requestStatus", "Approved");
		}
		metaDataParams.put("authority", certificateData.getAuthority()!=null ? certificateData.getAuthority(): object.get("authority").getAsString());
		metaDataParams.put("certificateId",((Integer)certificateData.getCertificateId()).toString()!=null ?
				((Integer)certificateData.getCertificateId()).toString() : object.get("certificateId").getAsString());
		metaDataParams.put("createDate", certificateData.getCreateDate()!=null ? certificateData.getCreateDate() : object.get("createDate").getAsString());
		metaDataParams.put("expiryDate", certificateData.getExpiryDate()!=null ? certificateData.getExpiryDate() : object.get("expiryDate").getAsString());
		metaDataParams.put("certificateStatus", certificateData.getCertificateStatus()!=null ?
				certificateData.getCertificateStatus(): object.get("certificateStatus").getAsString());
		metaDataParams.put("containerName", certificateData.getContainerName()!=null ?
				certificateData.getContainerName() : object.get("containerName").getAsString());
        if(Objects.nonNull(certificateData.getDnsNames())) {
            metaDataParams.put("dnsNames", certificateData.getDnsNames().toString());
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
		boolean sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(certificatePath, metaDataParams, authToken);
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
		String certName = certificateMetaData.getCertificateName();
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
		        certificateData.setCertificateId(Integer.parseInt(jsonElement.get("certificateId").getAsString()));
		        certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
		        certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));
		        certificateData.setContainerName(validateString(jsonElement.get("containerName")));
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
		String authToken = ""; 
		String certPrefix=(certificateType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.INTERNAL_POLICY_NAME :SSLCertificateConstants.EXTERNAL_POLICY_NAME;
		
		String metaDataPath = (certificateType.equalsIgnoreCase("internal"))?
	            SSLCertificateConstants.SSL_CERT_PATH_VALUE :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		if (userDetails.isAdmin()) {
				authToken = userDetails.getClientToken();   	            
	        }else {
	        	authToken = userDetails.getSelfSupportToken();
	        }
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
		
		Response userResponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}", authToken);	
		}
		else {
			userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}", authToken);
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
				currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				if (!(TVaultConstants.USERPASS.equals(vaultAuthMethod))) {
					groups =objMapper.readTree(responseJson).get("data").get("groups").asText();
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
			
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		Response ldapConfigresponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, authToken);
		}
		else {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, authToken);
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
			return updateMetadataForRemoveUserFromCertificate(userName, certificatePath, authToken, groups,
					policiesString);
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
					.put(LogMessage.ACTION, "deleteCertificate")
					.put(LogMessage.MESSAGE, "Invalid user inputs")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}
		Map<String, String> metaDataParams = new HashMap<String, String>();
		String endPoint =certificateName;	
		String metaDataPath = (certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint :SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + endPoint;
		String permissionMetaDataPath = (certType.equalsIgnoreCase("internal"))?
                SSLCertificateConstants.SSL_CERT_PATH_VALUE + "/" + endPoint :SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT + "/" + endPoint;
		
		Response response = new Response();
		Response metadataResponse = new Response();
		CertResponse unAssignResponse = new CertResponse();
		if (!userDetails.isAdmin()) {
//			Boolean isPermission = validateOwnerPermissionForNonAdmin(userDetails, certificateName);
			Boolean isPermission = validateCertOwnerPermissionForNonAdmin(userDetails, certificateName,certType);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("{\"errors\":[\""
								+ "Access denied: No permission to delete certificate"
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
					.body("{\"messages\":[\"" + "Certificate unavailable" + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"errors\":[\"" + "Certificate unavailable" + "\"]}");
		}
		
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");				
		
		int certID = object.get("certificateId").getAsInt();	
		int containerId = object.get("containerId").getAsInt();
		try {
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);	
		String certificateUserId = metaDataParams.get("certOwnerNtid");
		
		//remove user permissions
		CertificateUser certificateUser = new CertificateUser();
		Map<String, String> userParams = new HashMap<String, String>();
		JsonObject userObj = ((JsonObject) jsonParser.parse(object.get("users").toString()));
		userParams = new Gson().fromJson(userObj.toString(), Map.class);
		if(!userParams.isEmpty()) {
		for (Map.Entry<String, String> entry : userParams.entrySet()) {
			certificateUser.setCertificateName(certificateName);
			certificateUser.setCertType(certType);
			certificateUser.setUsername(entry.getKey());
			certificateUser.setAccess(entry.getValue());
			removeUserFromCertificate( certificateUser,  userDetails);
		 }
		}		
		
			//remove group permissions
				CertificateGroup certificateGroup = new CertificateGroup();
				Map<String, String> groupParams = new HashMap<String, String>();
				JsonObject groupObj = ((JsonObject) jsonParser.parse(object.get("groups").toString()));
				groupParams = new Gson().fromJson(groupObj.toString(), Map.class);
				if(!groupParams.isEmpty()) {
				for (Map.Entry<String, String> entry : groupParams.entrySet()) {
					certificateGroup.setCertificateName(certificateName);
					certificateGroup.setCertType(certType);
					certificateGroup.setGroupname(entry.getKey());
					certificateGroup.setAccess(entry.getValue());
					removeGroupFromCertificate( certificateGroup,  userDetails);
				 }
				}
		
			removeSudoPermissionForPreviousOwner( certificateUserId.toLowerCase(), certificateName,userDetails,certType);
			String nclmAccessToken = getNclmToken();
			
			//find certificates
			CertificateData certData = getLatestCertificate(certificateName,nclmAccessToken, containerId);		
			if(certData!=null) {
			//Unassign certificate from target system
			JsonObject jo = new JsonObject();
	        jo.add("targetSystemServiceIds", new GsonBuilder().create().toJsonTree(certData.getDeployStatus()));
			String nclmApiAssignEndpoint = unassignCertificateEndpoint.replace("certID", String.valueOf(certID));
			unAssignResponse = reqProcessor.processCert("/certificates/services/assigned",  jo,
					nclmAccessToken, getCertmanagerEndPoint(nclmApiAssignEndpoint));	
			if (unAssignResponse!=null && HttpStatus.OK.equals(unAssignResponse.getHttpstatus())) {
				//delete the certiicate
				String nclmApiDeleteEndpoint = deleteCertificateEndpoint.replace("certID", String.valueOf(certID));
				unAssignResponse = reqProcessor.processCert("/certificates", "",
						nclmAccessToken, getCertmanagerEndPoint(nclmApiDeleteEndpoint));	
			}
			
			if (unAssignResponse!=null && (HttpStatus.OK.equals(unAssignResponse.getHttpstatus())|| (HttpStatus.NO_CONTENT.equals(unAssignResponse.getHttpstatus())))) {
				try {
					if (userDetails.isAdmin()) {
						response = reqProcessor.process("/delete", "{\"path\":\"" + metaDataPath + "\"}", token);
						metadataResponse=reqProcessor.process("/delete", "{\"path\":\"" + permissionMetaDataPath + "\"}", token);
					} else {
						response = reqProcessor.process("/delete", "{\"path\":\"" + metaDataPath + "\"}",
								userDetails.getSelfSupportToken());
						metadataResponse = reqProcessor.process("/delete", "{\"path\":\"" + permissionMetaDataPath + "\"}",
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
							.body("{\"messages\":[\"" + "Certificate metadata deletion failed" + "\"]}");
				}


				//Send an email for delete in case of internal and external
                sendDeleteEmail(token,certType, certificateName, userDetails,
                        SSLCertificateConstants.CERT_DELETE_SUBJECT + " - " + certificateName,
                        "deleted",certData);
				return ResponseEntity.status(HttpStatus.OK)
						.body("{\"messages\":[\"" + "Certificate deleted  Successfully" + "\"]}");
			}
			else {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION, "Delete certificate Failed")
										.put(LogMessage.MESSAGE, "Delete Request failed for CertificateID")
										.put(LogMessage.STATUS, unAssignResponse.getHttpstatus().toString())
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
										.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"" + "Certificate Deletion Failed" + "\"]}");
			}	
			}else {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION, "Delete certificate Failed")
										.put(LogMessage.MESSAGE, "Delete Request failed for CertificateID")
										.put(LogMessage.STATUS, unAssignResponse.getHttpstatus().toString())
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
										.build()));
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"" + "Certificate unavailable in NCLM." + "\"]}");
			}
			
	} catch (Exception e) {
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
				.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
						Arrays.toString(e.getStackTrace()), e.getMessage()))
				.build()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("{\"errors\":[\"" + e.getMessage() + "\"]}");
	}
	}
	
	/**
     * Validate input data
     * @param sslCertificateRequest
     * @return
     */
	
    private boolean validateTransferData(SSLCertificateMetadataDetails sslCertificateRequest){
        boolean isValid=true;
        if(sslCertificateRequest.getCertificateName().contains(" ") || 
                sslCertificateRequest.getCertOwnerEmailId().contains(" ") ||  sslCertificateRequest.getCertType().contains(" ") ||
                (!sslCertificateRequest.getCertificateName().endsWith(certificateNameTailText)) ||                
                (sslCertificateRequest.getCertificateName().contains(".-")) ||
                (sslCertificateRequest.getCertificateName().contains("-.")) ||
                (!sslCertificateRequest.getCertType().matches(SSLCertificateConstants.CERT_TYPE_MATCH_STRING))){
            isValid= false;
        }       
        return isValid;
    }
    
    /**
	 * Validate Permission for Non-admin User.
	 * 
	 * @param userDetails
	 * @param certificateName
	 * @return
	 */
	public Boolean validateCertOwnerPermissionForNonAdmin(UserDetails userDetails, String certificateName, String certType) {
		String ownerPermissionCertName = (certType.equalsIgnoreCase("internal"))?
				SSLCertificateConstants.OWNER_PERMISSION_CERTIFICATE + certificateName :SSLCertificateConstants.OWNER_PERMISSION_EXT_CERTIFICATE + certificateName;
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
                put(LogMessage.ACTION, "getAllCertificates").
                put(LogMessage.MESSAGE, "Trying to get all certificates").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        String path = SSLCertificateConstants.SSL_CERT_PATH ;        
        String extPath = SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH ;        

        Response response;
        String certListStr = "";

        response = getMetadata(token, path);        
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            String pathStr= "";
            String endPoint = "";
            Response metadataResponse = new Response();
            JsonParser jsonParser = new JsonParser();
            JsonArray responseArray = new JsonArray();
            JsonObject metadataJsonObj=new JsonObject();            
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
            List<String> certNames = geMatchCertificates(jsonArray,certName);            
            
            response = getMetadata(token, extPath);
            JsonObject jsonObjectExt = (JsonObject) jsonParser.parse(response.getResponse());
            JsonArray jsonArrayExt = jsonObjectExt.getAsJsonObject("data").getAsJsonArray("keys");
            List<String> certNamesExt = geMatchCertificates(jsonArrayExt,certName);            
            certNames.addAll(certNamesExt);
            Collections.sort(certNames);
            
            if(limit == null) {
                limit = certNames.size();
            }
            if (offset ==null) {
                offset = 0;
            }

            int maxVal = certNames.size()> (limit+offset)?limit+offset : certNames.size();
            for (int i = offset; i < maxVal; i++) {            	
                endPoint = certNames.get(i).replaceAll("^\"+|\"+$", "");
                if(certNamesExt.contains(certNames.get(i))) {  
                	pathStr = extPath + TVaultConstants.PATH_DELIMITER + endPoint;                
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
        else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getAllCertificates").
                    put(LogMessage.MESSAGE, "Retrieved empty certificate list from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body(certListStr);
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
    
}

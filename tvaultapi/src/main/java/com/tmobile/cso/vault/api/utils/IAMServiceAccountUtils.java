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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.IAMServiceAccountConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.model.IAMSecretsMetadata;
import com.tmobile.cso.vault.api.model.IAMServiceAccountRotateRequest;
import com.tmobile.cso.vault.api.model.IAMServiceAccountSecret;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Component
public class IAMServiceAccountUtils {
    private Logger log = LogManager.getLogger(IAMServiceAccountUtils.class);

    @Value("${iamPortal.domain}")
    private String iamPortalDomain;
    @Value("${iamPortal.secret.endpoint}")
    private String iamPortalSecretEndpoint;
    @Value("${iamPortal.secret.endpoint}")
    private String iamPortalrotateSecretEndpoint;
    @Value("${iamPortal.auth.endpoint}")
    private String iamPortalAuthEndpoint;

    @Autowired
    HttpUtils httpUtils;

    @Autowired
    private RequestProcessor requestProcessor;

    /**
     * To get approle token fro IAM Portal approle.
     * @return
     */
    public String getIAMApproleToken() {
        String authIAMAuthApi = iamPortalAuthEndpoint;

        AppRoleIdSecretId appRoleIdSecretId = new AppRoleIdSecretId();
        if (ControllerUtil.getSscred() != null) {
            appRoleIdSecretId.setRole_id(new String(Base64.getDecoder().decode(ControllerUtil.getIamUsername())));
            appRoleIdSecretId.setSecret_id(new String(Base64.getDecoder().decode(ControllerUtil.getIamPassword())));
        }
        else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getIAMApproleToken").
                    put(LogMessage.MESSAGE, "Failed to get IAM portal credentials").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        HttpClient httpClient = httpUtils.getHttpClient();
        if (httpClient == null) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getIAMApproleToken").
                    put(LogMessage.MESSAGE, "Failed to initialize httpClient").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        HttpPost httpPost = new HttpPost(authIAMAuthApi);

        List<NameValuePair> params = new ArrayList<>();
        String inputJson = JSONUtil.getJSON(appRoleIdSecretId);
        StringEntity entity;
        try {
            entity = new StringEntity(inputJson);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setEntity(entity);

        } catch (UnsupportedEncodingException e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getIAMApproleToken").
                    put(LogMessage.MESSAGE, "Failed to build StringEntity").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        String output = "";
        StringBuffer jsonResponse = new StringBuffer();

        try {
            HttpResponse apiResponse = httpClient.execute(httpPost);
            if (apiResponse.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
            while ((output = br.readLine()) != null) {
                jsonResponse.append(output);
            }
            String iamPortalToken = null;
            JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
            if (!responseJson.isJsonNull()) {
                if (responseJson.has("auth")) {
                    JsonObject authJson = responseJson.get("auth").getAsJsonObject();
                    if (authJson.has("client_token")) {
                        iamPortalToken = authJson.get("client_token").getAsString();
                    }
                }
            }
            return iamPortalToken;
        } catch (IOException e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getIAMApproleToken").
                    put(LogMessage.MESSAGE, "Failed to parse Approle login response").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
        }
        return null;
    }
    /**
     * To get response from Workload endpoint
     *
     * @return
     */
    public IAMServiceAccountSecret rotateIAMSecret(IAMServiceAccountRotateRequest iamServiceAccountRotateRequest)  {
        String iamApproleToken = getIAMApproleToken();
        if (StringUtils.isEmpty(iamApproleToken)) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "rotateIAMSecret").
                    put(LogMessage.MESSAGE, "Invalid IAM Portal approle token").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        String api = iamPortalDomain + iamPortalrotateSecretEndpoint;
        if (StringUtils.isEmpty(iamPortalDomain) || StringUtils.isEmpty(iamPortalrotateSecretEndpoint)) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "rotateIAMSecret").
                    put(LogMessage.MESSAGE, "Invalid IAM portal endpoint").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        HttpClient httpClient = httpUtils.getHttpClient();
        if (httpClient == null) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "rotateIAMSecret").
                    put(LogMessage.MESSAGE, "Failed to initialize httpClient").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        HttpPut httpPut = new HttpPut(api);

        String inputJson = JSONUtil.getJSON(iamServiceAccountRotateRequest);
        StringEntity entity;
        String iamAuthToken = IAMServiceAccountConstants.IAM_AUTH_TOKEN_PREFIX + " " + Base64.getEncoder().encodeToString(iamApproleToken.getBytes());

        try {
            entity = new StringEntity(inputJson);
            httpPut.setEntity(entity);
            httpPut.setHeader("Authorization", iamAuthToken);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            httpPut.setEntity(entity);

        } catch (UnsupportedEncodingException e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "rotateIAMSecret").
                    put(LogMessage.MESSAGE, "Failed to build StringEntity").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        String output = "";
        StringBuffer jsonResponse = new StringBuffer();

        try {
            HttpResponse apiResponse = httpClient.execute(httpPut);
            if (apiResponse.getStatusLine().getStatusCode() != 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(apiResponse.getEntity().getContent()));
                StringBuilder total = new StringBuilder();
                String line = null;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
                r.close();
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "rotateIAMSecret").
                        put(LogMessage.MESSAGE, "Failed to build StringEntity:"+total.toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
            while ((output = br.readLine()) != null) {
                jsonResponse.append(output);
            }
            IAMServiceAccountSecret iamServiceAccountSecret = new IAMServiceAccountSecret();
            JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
            if (!responseJson.isJsonNull()) {
                if (responseJson.has("accessKeyId")) {
                    iamServiceAccountSecret.setAccessKeyId(responseJson.get("accessKeyId").getAsString());
                }
                if (responseJson.has("userName")) {
                    iamServiceAccountSecret.setUserName(responseJson.get("userName").getAsString());
                }
                if (responseJson.has("accessKeySecret")) {
                    iamServiceAccountSecret.setAccessKeySecret(responseJson.get("accessKeySecret").getAsString());
                }
                if (responseJson.has("expiryDateEpoch")) {
                    iamServiceAccountSecret.setExpiryDateEpoch(responseJson.get("expiryDateEpoch").getAsLong());
                }
                iamServiceAccountSecret.setAwsAccountId(iamServiceAccountRotateRequest.getAccountId());
            }
            return iamServiceAccountSecret;
        } catch (IOException e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "rotateIAMSecret").
                    put(LogMessage.MESSAGE, "Failed to parse IAM Secret response").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
        }
        return null;
    }

    /**
     * To save IAM Service Account Secret for a single AccesKeyId.
     * @param iamServiceAccountSecret
     */
    public boolean writeIAMSvcAccSecret(String token, String path, String iamServiceAccountName, IAMServiceAccountSecret iamServiceAccountSecret) {
        boolean isSecretUpdated = false;
        ObjectMapper objMapper = new ObjectMapper();
        String secretJson = null;
        try {
            secretJson = objMapper.writeValueAsString(iamServiceAccountSecret);
        } catch (JsonProcessingException e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "writeIAMSvcAccSecret").
                    put(LogMessage.MESSAGE, "Failed to write IAMServiceAccountSecret as string json").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return isSecretUpdated;
        }
        String writeJson =  "{\"path\":\""+path+"\",\"data\":"+ secretJson +"}";
        Response response = requestProcessor.process("/write", writeJson, token);


        if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
            isSecretUpdated = true;
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "writeIAMSvcAccSecret").
                    put(LogMessage.MESSAGE, String.format("Successfully saved credentials for IAM Service Account " +
                                    "[%s] for access key : [%s]", iamServiceAccountName,
                            iamServiceAccountSecret.getAccessKeyId())).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "writeIAMSvcAccSecret").
                    put(LogMessage.MESSAGE, "Failed to save IAM Service Account Secret in T-Vault").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
        return isSecretUpdated;
    }

    /**
     * Update metadata for the IAM service account on activation.
     * @param token
     * @param iamServiceAccountName
     * @param awsAccountId
     * @return
     */
    public Response updateActivatedStatusInMetadata(String token, String iamServiceAccountName, String awsAccountId){
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "updateActivatedStatusInMetadata").
                put(LogMessage.MESSAGE, String.format ("Trying to update metadata on IAM Service account activation [%s] in aws account [%s]", iamServiceAccountName, awsAccountId)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String uniqueIAMSvcaccName = awsAccountId + "_" + iamServiceAccountName;
        String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(uniqueIAMSvcaccName).toString();
        Map<String,String> isActivatedParams = new Hashtable<>();
        isActivatedParams.put("type", "isActivated");
        isActivatedParams.put("path",path);
        isActivatedParams.put("value","true");

        String typeIsActivated = isActivatedParams.get("type");
        path = "metadata/"+path;

        ObjectMapper objMapper = new ObjectMapper();
        String pathjson ="{\"path\":\""+path+"\"}";
        // Read info for the path
        Response metadataResponse = requestProcessor.process("/read",pathjson,token);
        Map<String,Object> _metadataMap = null;
        if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
            try {
                _metadataMap = objMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
            } catch (IOException e) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "updateActivatedStatusInMetadata").
                        put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type [%s] and path [%s] message [%s]", typeIsActivated, path, e.getMessage())).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }

            @SuppressWarnings("unchecked")
            Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");

            @SuppressWarnings("unchecked")
            boolean isActivated = (boolean) metadataMap.get(typeIsActivated);
            if(StringUtils.isEmpty(isActivated) || !isActivated) {
                metadataMap.put(typeIsActivated, true);
                String metadataJson = "";
                try {
                    metadataJson = objMapper.writeValueAsString(metadataMap);
                } catch (JsonProcessingException e) {
                    log.error(e);
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateActivatedStatusInMetadata").
                            put(LogMessage.MESSAGE, String.format ("Error in creating metadataJson for type [%s] and path [%s] with message [%s]", typeIsActivated, path, e.getMessage())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                }

                String writeJson =  "{\"path\":\""+path+"\",\"data\":"+ metadataJson +"}";
                metadataResponse = requestProcessor.process("/write",writeJson,token);
                return metadataResponse;
            }
            return metadataResponse;
        }
        return null;
    }

    /**
     * To udpated Access key details in metadata.
     * @param token
     * @param awsAccountId
     * @param iamServiceAccountName
     * @param accessKeyId
     * @param iamServiceAccountSecret
     * @return
     */
    public Response updateIAMSvcAccNewAccessKeyIdInMetadata(String token, String awsAccountId, String iamServiceAccountName, String accessKeyId, IAMServiceAccountSecret iamServiceAccountSecret){
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "updateIAMSvcAccNewAccessKeyIdInMetadata").
                put(LogMessage.MESSAGE, String.format ("Trying to update the metadata with new accessKeyId for [%s] for IAM service account [%s] in aws account [%s]", accessKeyId, iamServiceAccountName, awsAccountId)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String uniqueIAMSvcaccName = awsAccountId + "_" + iamServiceAccountName;
        String path = new StringBuffer(IAMServiceAccountConstants.IAM_SVCC_ACC_PATH).append(uniqueIAMSvcaccName).toString();

        List<IAMSecretsMetadata> secretData = new ArrayList<>();


        String typeSecret = "secret";
        path = "metadata/"+path;

        ObjectMapper objMapper = new ObjectMapper();
        String pathjson ="{\"path\":\""+path+"\"}";
        // Read info for the path
        Response metadataResponse = requestProcessor.process("/read",pathjson,token);
        Map<String,Object> _metadataMap = null;
        if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
            try {
                _metadataMap = objMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
            } catch (IOException e) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "updateIAMSvcAccNewAccessKeyIdInMetadata").
                        put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type [%s] and path [%s] message [%s]", typeSecret, path, e.getMessage())).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }

            @SuppressWarnings("unchecked")
            Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");

            @SuppressWarnings("unchecked")
            //List<IAMSecretsMetadata> currentSecretData = (List<IAMSecretsMetadata>) metadataMap.get(typeSecret);

            ObjectMapper objectMapper = new ObjectMapper();
            List<IAMSecretsMetadata> currentSecretData = objectMapper.convertValue((List<IAMSecretsMetadata>) metadataMap.get(typeSecret), new TypeReference<List<IAMSecretsMetadata>>() { });
            if(null != currentSecretData) {
                List<IAMSecretsMetadata> newSecretData = new ArrayList<>();
                for (int i=0;i<currentSecretData.size();i++) {
                    IAMSecretsMetadata iamSecretsMetadata = currentSecretData.get(i);
                    if (accessKeyId.equals(iamSecretsMetadata.getAccessKeyId())) {
                        iamSecretsMetadata.setAccessKeyId(iamServiceAccountSecret.getAccessKeyId());
                        iamSecretsMetadata.setExpiryDuration(iamServiceAccountSecret.getExpiryDateEpoch());
                    }
                    newSecretData.add(iamSecretsMetadata);
                }

                metadataMap.put(typeSecret, newSecretData);
                String metadataJson = "";
                try {
                    metadataJson = objMapper.writeValueAsString(metadataMap);
                } catch (JsonProcessingException e) {
                    log.error(e);
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateIAMSvcAccNewAccessKeyIdInMetadata").
                            put(LogMessage.MESSAGE, String.format ("Error in creating metadataJson for type [%s] and path [%s] with message [%s]", typeSecret, path, e.getMessage())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                }

                String writeJson =  "{\"path\":\""+path+"\",\"data\":"+ metadataJson +"}";
                metadataResponse = requestProcessor.process("/write",writeJson,token);
                return metadataResponse;
            }
            return metadataResponse;
        }
        return null;
    }
}
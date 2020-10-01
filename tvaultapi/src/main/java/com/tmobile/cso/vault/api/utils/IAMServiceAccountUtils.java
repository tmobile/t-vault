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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.IAMServiceAccountRotateRequest;
import com.tmobile.cso.vault.api.model.IAMServiceAccountSecret;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import java.util.ArrayList;
import java.util.List;

@Component
public class IAMServiceAccountUtils {
    private Logger log = LogManager.getLogger(IAMServiceAccountUtils.class);

    @Value("${iamPortal.endpoint}")
    private String iamPortalEndpoint;
    @Value("${iamPortal.endpoint.token}")
    private String iamPortalEndpointToken;
    @Value("${iamPortal.rotateSecret.endpoint}")
    private String iamPortalrotateSecretEndpoint;

    @Autowired
    HttpUtils httpUtils;

    @Autowired
    private RequestProcessor requestProcessor;

    /**
     * To get response from Workload endpoint
     *
     * @return
     */
    public IAMServiceAccountSecret rotateIAMSecret(IAMServiceAccountRotateRequest iamServiceAccountRotateRequest)  {
        String api = iamPortalrotateSecretEndpoint;
        if (StringUtils.isEmpty(iamPortalEndpointToken)) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "rotateIAMSecret").
                    put(LogMessage.MESSAGE, String.format ("Invalid IAM portal token")).
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

        List<NameValuePair> params = new ArrayList<>();
        String inputJson = JSONUtil.getJSON(iamServiceAccountRotateRequest);
        StringEntity entity;
        try {
            entity = new StringEntity(inputJson);
            httpPut.setEntity(entity);
            httpPut.setHeader("Authorization", iamPortalEndpointToken);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            httpPut.setEntity(new UrlEncodedFormEntity(params));

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
                    iamServiceAccountSecret.setAccessKeyId(responseJson.get("userName").getAsString());
                }
                if (responseJson.has("accessKeySecret")) {
                    iamServiceAccountSecret.setAccessKeyId(responseJson.get("accessKeySecret").getAsString());
                }
                if (responseJson.has("expiryDateEpoch")) {
                    iamServiceAccountSecret.setAccessKeyId(responseJson.get("expiryDateEpoch").getAsString());
                }
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
}
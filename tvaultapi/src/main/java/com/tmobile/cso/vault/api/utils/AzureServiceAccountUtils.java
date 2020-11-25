package com.tmobile.cso.vault.api.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.AzureServiceAccountConstants;
import com.tmobile.cso.vault.api.common.IAMServiceAccountConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

@Component
public class AzureServiceAccountUtils {

    @Value("${iamPortal.domain}")
    private String iamPortalDomain;
    @Value("${iamPortal.auth.endpoint}")
    private String iamPortalAuthEndpoint;
    @Value("${azurePortal.secret.endpoint}")
    private String azurePortalrotateSecretEndpoint;

    private Logger log = LogManager.getLogger(AzureServiceAccountUtils.class);

    @Autowired
    HttpUtils httpUtils;

    @Autowired
    IAMServiceAccountUtils iamServiceAccountUtils;

    @Autowired
    private RequestProcessor requestProcessor;

    /**
     * Convenient method to get policies as list from token lookup.
     * @param objMapper
     * @param policyJson
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     */
    public List<String> getTokenPoliciesAsListFromTokenLookupJson(ObjectMapper objMapper, String policyJson) throws IOException{
        List<String> currentpolicies = new ArrayList<>();
        JsonNode policiesNode = objMapper.readTree(policyJson).get("policies");
        if (null != policiesNode ) {
            if (policiesNode.isContainerNode()) {
                Iterator<JsonNode> elementsIterator = policiesNode.elements();
                while (elementsIterator.hasNext()) {
                    JsonNode element = elementsIterator.next();
                    currentpolicies.add(element.asText());
                }
            }
            else {
                currentpolicies.add(policiesNode.asText());
            }
        }
        return currentpolicies;
    }


    /**
     * To get response from rotate api.
     *
     * @return
     */
    public AzureServiceAccountSecret rotateAzureServicePrincipalSecret(AzureServicePrinicipalRotateRequest azureServicePrinicipalRotateRequest)  {
        String iamApproleToken = iamServiceAccountUtils.getIAMApproleToken();
        if (StringUtils.isEmpty(iamApproleToken)) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, AzureServiceAccountConstants.GET_AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Invalid IAM Portal approle token").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        String api = iamPortalDomain + azurePortalrotateSecretEndpoint;
        if (StringUtils.isEmpty(iamPortalDomain) || StringUtils.isEmpty(azurePortalrotateSecretEndpoint)) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, AzureServiceAccountConstants.GET_AZURE_SP_ROTATE_SECRET_ACTION).
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
                    put(LogMessage.ACTION, AzureServiceAccountConstants.GET_AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Failed to initialize httpClient").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        HttpPut httpPut = new HttpPut(api);

        String inputJson = JSONUtil.getJSON(azureServicePrinicipalRotateRequest);
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
                    put(LogMessage.ACTION, AzureServiceAccountConstants.GET_AZURE_SP_ROTATE_SECRET_ACTION).
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
                        put(LogMessage.ACTION, AzureServiceAccountConstants.GET_AZURE_SP_ROTATE_SECRET_ACTION).
                        put(LogMessage.MESSAGE, "Failed to build StringEntity:"+total.toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
            while ((output = br.readLine()) != null) {
                jsonResponse.append(output);
            }
            AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret();
            JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
            if (!responseJson.isJsonNull()) {
                if (responseJson.has("servicePrinicipalId")) {
                    azureServiceAccountSecret.setServicePrinicipalId(responseJson.get("servicePrinicipalId").getAsString());
                }
                if (responseJson.has("tenantId")) {
                    azureServiceAccountSecret.setTenantId(responseJson.get("tenantId").getAsString());
                }
                if (responseJson.has("secretKeyId")) {
                    azureServiceAccountSecret.setSecretKeyId(responseJson.get("secretKeyId").getAsString());
                }
                if (responseJson.has("secretText")) {
                    azureServiceAccountSecret.setSecretText(responseJson.get("secretText").getAsString());
                }
                if (responseJson.has("expiryDateEpoch")) {
                    azureServiceAccountSecret.setExpiryDateEpoch(responseJson.get("expiryDateEpoch").getAsLong());
                }
            }
            return azureServiceAccountSecret;
        } catch (IOException e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, AzureServiceAccountConstants.GET_AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Failed to parse Azure Service Principal Secret response").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
        return null;
    }
}

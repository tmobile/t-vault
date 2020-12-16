package com.tmobile.cso.vault.api.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.AzureServiceAccountConstants;
import com.tmobile.cso.vault.api.common.IAMServiceAccountConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
     * To mock rotate Azure secret API.
     * @param azureServicePrincipalRotateRequest
     * @return
     */
    public AzureServiceAccountSecret rotateAzureServicePrincipalSecretMOCK(AzureServicePrincipalRotateRequest azureServicePrincipalRotateRequest) {
        AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret();
        azureServiceAccountSecret.setServicePrincipalId(azureServicePrincipalRotateRequest.getServicePrincipalId());
        azureServiceAccountSecret.setTenantId(azureServicePrincipalRotateRequest.getTenantId());
        azureServiceAccountSecret.setSecretKeyId(azureServicePrincipalRotateRequest.getSecretKeyId());
        azureServiceAccountSecret.setSecretText("mocksecrettext_"+ new Date().getTime());
        azureServiceAccountSecret.setExpiryDateEpoch(604800000L);
        azureServiceAccountSecret.setExpiryDate(new Date(604800000L).toString());
        return azureServiceAccountSecret;
    }
    /**
     * To get response from rotate api.
     *
     * @return
     * @throws IOException 
     */
    public AzureServiceAccountSecret rotateAzureServicePrincipalSecret(AzureServicePrincipalRotateRequest azureServicePrincipalRotateRequest) throws IOException  {
        String iamApproleToken = iamServiceAccountUtils.getIAMApproleToken();
        if (StringUtils.isEmpty(iamApproleToken)) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Invalid IAM Portal approle token").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        String api = iamPortalDomain + azurePortalrotateSecretEndpoint;
        if (StringUtils.isEmpty(iamPortalDomain) || StringUtils.isEmpty(azurePortalrotateSecretEndpoint)) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Invalid Azure service principal endpoint").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        HttpClient httpClient = httpUtils.getHttpClient();
        if (httpClient == null) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Failed to initialize httpClient").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }

        HttpPut httpPut = new HttpPut(api);

        String inputJson = JSONUtil.getJSON(azureServicePrincipalRotateRequest);
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
                    put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Failed to build StringEntity").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return null;
        }


        StringBuilder jsonResponse = new StringBuilder();        
        try {
            HttpResponse apiResponse = httpClient.execute(httpPut);
            if (apiResponse.getStatusLine().getStatusCode() != 200) {

				StringBuilder total = new StringBuilder();
				readFailedResponseContent(apiResponse, total);
                return null;
            }

            readResponseContent(jsonResponse, apiResponse, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION);
            AzureServiceAccountSecret azureServiceAccountSecret = new AzureServiceAccountSecret();
            JsonObject responseJson = (JsonObject) jsonParser.parse(jsonResponse.toString());
            if (!responseJson.isJsonNull()) {
                if (responseJson.has("servicePrincipalId")) {
                    azureServiceAccountSecret.setServicePrincipalId(responseJson.get("servicePrincipalId").getAsString());
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
                    put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
                    put(LogMessage.MESSAGE, "Failed to parse Azure Service Principal Secret response").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
        return null;
    }

	/**
	 * Method to read response
	 * @param apiResponse
	 * @param total
	 */
	private void readFailedResponseContent(HttpResponse apiResponse, StringBuilder total) {		
		try(BufferedReader r = new BufferedReader(new InputStreamReader(apiResponse.getEntity().getContent()))) {
			String line = null;
			while ((line = r.readLine()) != null) {
			    total.append(line);
			}

			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			        put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
			        put(LogMessage.MESSAGE, "Failed to build StringEntity:"+total.toString()).
			        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			        build()));
		}catch(IOException ex) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			        put(LogMessage.ACTION, AzureServiceAccountConstants.AZURE_SP_ROTATE_SECRET_ACTION).
			        put(LogMessage.MESSAGE, "Failed to read response").
			        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			        build()));
		}
	}

    /**
     * To save Azure Service Principal Secret for a single SecretKeyId.
     * @param token
     * @param path
     * @param servicePrincipalName
     * @param azureServiceAccountSecret
     * @return
     */
    public boolean writeAzureSPSecret(String token, String path, String servicePrincipalName, AzureServiceAccountSecret azureServiceAccountSecret) {
        boolean isSecretUpdated = false;
        ObjectMapper objMapper = new ObjectMapper();
        String secretJson = null;
        try {
            secretJson = objMapper.writeValueAsString(azureServiceAccountSecret);
        } catch (JsonProcessingException e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "writeAzureSPSecret").
                    put(LogMessage.MESSAGE, "Failed to write Azure Service Principal secret request as string json").
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
                    put(LogMessage.ACTION, "writeAzureSPSecret").
                    put(LogMessage.MESSAGE, String.format("Successfully saved secrets for Azure Service Principal " +
                                    "[%s] for secret key id: [%s]", servicePrincipalName,
                            azureServiceAccountSecret.getSecretKeyId())).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "writeAzureSPSecret").
                    put(LogMessage.MESSAGE, "Failed to save Azure Service Principal Secret in T-Vault").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
        return isSecretUpdated;
    }

    /**
     * To update Secret key details in metadata.
     * @param token
     * @param servicePrincipalName
     * @param secretKeyId
     * @param azureServiceAccountSecret
     * @return
     */
    public Response updateAzureSPSecretKeyInfoInMetadata(String token, String servicePrincipalName, String secretKeyId, AzureServiceAccountSecret azureServiceAccountSecret){
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "updateAzureSPSecretKeyInfoInMetadata").
                put(LogMessage.MESSAGE, String.format ("Trying to update the metadata with secretKeyId [%s] for Azure Service Principal [%s]", secretKeyId, servicePrincipalName)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String path = new StringBuffer(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(servicePrincipalName).toString();

        List<AzureSvccAccMetadata> secretData = new ArrayList<>();


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
                        put(LogMessage.ACTION, "updateAzureSPSecretKeyInfoInMetadata").
                        put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for type [%s] and path [%s] message [%s]", typeSecret, path, e.getMessage())).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }

            @SuppressWarnings("unchecked")
            Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");

            ObjectMapper objectMapper = new ObjectMapper();
            List<AzureSecretsMetadata> currentSecretData = objectMapper.convertValue((List<AzureSecretsMetadata>) metadataMap.get(typeSecret), new TypeReference<List<AzureSecretsMetadata>>() { });
            if(null != currentSecretData) {
                List<AzureSecretsMetadata> newSecretData = new ArrayList<>();
                for (int i=0;i<currentSecretData.size();i++) {
                    AzureSecretsMetadata azureSecretsMetadata = currentSecretData.get(i);
                    if (secretKeyId.equals(azureSecretsMetadata.getSecretKeyId())) {
                        azureSecretsMetadata.setSecretKeyId(azureServiceAccountSecret.getSecretKeyId());
                        azureSecretsMetadata.setExpiryDuration(azureServiceAccountSecret.getExpiryDateEpoch());
                    }
                    newSecretData.add(azureSecretsMetadata);
                }

                metadataMap.put(typeSecret, newSecretData);
                String metadataJson = "";
                try {
                    metadataJson = objMapper.writeValueAsString(metadataMap);
                } catch (JsonProcessingException e) {
                    log.error(e);
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateAzureSPSecretKeyInfoInMetadata").
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

    /**
     * Update metadata for the Azure Service Principal on activation.
     * @param token
     * @param servicePrincipalName
     * @return
     */
    public Response updateActivatedStatusInMetadata(String token, String servicePrincipalName){
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "updateActivatedStatusInMetadata").
                put(LogMessage.MESSAGE, String.format ("Trying to update metadata on Azure Service Principal activation for [%s]", servicePrincipalName)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String path = new StringBuffer(AzureServiceAccountConstants.AZURE_SVCC_ACC_PATH).append(servicePrincipalName).toString();
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
	 * Method to read the response content
	 * @param jsonResponse
	 * @param apiResponse
	 */
	private void readResponseContent(StringBuilder jsonResponse, HttpResponse apiResponse, String actionMsg) {
		String output = "";
		try(BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())))) {
			while ((output = br.readLine()) != null) {
				jsonResponse.append(output);
			}
		}catch(Exception ex) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		            put(LogMessage.ACTION, actionMsg).
		            put(LogMessage.MESSAGE, "Failed to read the response").
		            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		            build()));
		}
	}
}

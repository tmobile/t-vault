package com.tmobile.cso.vault.api.utils;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.AzureServiceAccountConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
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
}

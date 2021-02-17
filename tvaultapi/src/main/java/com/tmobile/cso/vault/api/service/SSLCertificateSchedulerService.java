// =========================================================================
// Copyright 2021 T-Mobile, US
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class SSLCertificateSchedulerService {

    @Value("${SSL.metadatarefresh.enable}")
    private boolean isSSLMetadataRefreshEnabled;

    @Autowired
    private RequestProcessor reqProcessor;

    @Autowired
    private WorkloadDetailsService workloadDetailsService;

    @Autowired
    private SSLCertificateService sslCertificateService;

    @Autowired
    private TokenUtils tokenUtils;

    private static Logger log = LogManager.getLogger(SSLCertificateSchedulerService.class);

    @Scheduled(cron = "${SSLExternalCertificate.schedule.crontime}")
    public void checkApplicationMetaDataChanges() {
        ThreadLocalContext.getCurrentMap().put(LogMessage.APIURL, "ssl application change Scheduler");
        ThreadLocalContext.getCurrentMap().put(LogMessage.USER, "SSLCertificateSchedulerService");
        if (isSSLMetadataRefreshEnabled) {
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                    put(LogMessage.MESSAGE, String.format("Starting Certificate metadata refresh scheduler at [%s]", new Date().toString())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            String token = tokenUtils.getSelfServiceTokenWithAppRole();

            List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM = workloadDetailsService.getAllApplicationDetailsFromCLM();
            if (!tmoAppMetadataListFromCLM.isEmpty()) {
                log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                        put(LogMessage.MESSAGE, "Received application details from CLM").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                // Get all application metadata from metadata/tmo-applications
                List<TMOAppMetadataDetails> tmoAppMetadataList = workloadDetailsService.getAllAppMetadata(token);
                if (!tmoAppMetadataList.isEmpty()) {
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                            put(LogMessage.MESSAGE, "Received all application metadata details").
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    // Get list of applications with outdated metadata
                    List<TMOAppMetadataDetails> modifiedApplicationList = getModifiedApplicationList(tmoAppMetadataListFromCLM, tmoAppMetadataList);
                    modifiedApplicationList.addAll(getAppsFailedToUpdateMetadataLastTime(tmoAppMetadataList));
                    modifiedApplicationList = modifiedApplicationList.stream().distinct().collect(Collectors.toList());
                    if (modifiedApplicationList.size() > 0) {
                        // Update all certificates of applications in modifiedApplicationList with new application details
                        boolean updateStatus = updateCertMetadataForModifiedApps(token, modifiedApplicationList, tmoAppMetadataListFromCLM);
                        if (updateStatus) {
                            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                    put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                                    put(LogMessage.MESSAGE, "Successfully updated all certificates with latest application metadata").
                                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                    build()));
                        }
                        else {
                            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                    put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                                    put(LogMessage.MESSAGE, "Failed to update one or more application certificates with latest application metadata").
                                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                    build()));
                        }
                    }
                    else {
                        log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                                put(LogMessage.MESSAGE, "Metadata for all applications are already up-to-date").
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                    }
                }
                else {
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                            put(LogMessage.MESSAGE, "Failed to get all application metadata from metadata/tmo-applications").
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                }
            }
            else {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                        put(LogMessage.MESSAGE, "Failed to get all application details from CLM").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
        }
        else {
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, TVaultConstants.SCHEDULED_ACTION_APP_METADATA_CHECK).
                    put(LogMessage.MESSAGE, "Scheduler not enabled. Skipping now.").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }

    /**
     * To get applications which failed to update metadata for certificates last time.
     * @param tmoAppMetadataList
     * @return
     */
    private List<TMOAppMetadataDetails> getAppsFailedToUpdateMetadataLastTime(List<TMOAppMetadataDetails> tmoAppMetadataList) {
        return tmoAppMetadataList.stream().filter(a -> !a.isUpdateFlag()).collect(Collectors.toList());
    }

    /**
     * To udpate outdated application metadata details for all certificates.
     * @param token
     * @param modifiedApplications
     * @param tmoAppMetadataListFromCLM
     * @return
     */
    private boolean updateCertMetadataForModifiedApps(String token, List<TMOAppMetadataDetails> modifiedApplications, List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM) {

        int updateCount = 0;
        for (TMOAppMetadataDetails app: modifiedApplications) {
            TMOAppMetadataDetails clmApp = tmoAppMetadataListFromCLM.stream().filter(clmAppData ->
                            clmAppData.getApplicationName().equals(app.getApplicationName())).findFirst().orElse(null);
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "updateCertMetadataForModifiedApps").
                    put(LogMessage.MESSAGE, String.format("Trying to update certificate metadata for application [%s]", app.getApplicationName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            if (clmApp != null) {
                // Update metadata for all internal certificates in this application
                List<String> internalCertList = app.getInternalCertificateList();
                int noOfInternalCertsToUpdate = (internalCertList != null)?internalCertList.size():0;
                int internalUpdateCount = 0;
                if (internalCertList != null) {
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateCertMetadataForModifiedApps").
                            put(LogMessage.MESSAGE, String.format("Trying to update metadata for all internal certificates in application [%s]", app.getApplicationName())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    internalUpdateCount = updateCertMetadata(token, clmApp, internalCertList, SSLCertificateConstants.SSL_CERT_PATH);
                }

                // Update metadata for all internal certificates in this application
                List<String> externalCertList = app.getExternalCertificateList();
                int noOfExternalCertsToUpdate = (externalCertList != null)?externalCertList.size():0;
                int externalUpdateCount = 0;
                if (externalCertList != null) {
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateCertMetadataForModifiedApps").
                            put(LogMessage.MESSAGE, String.format("Trying to update metadata for all external certificates in application [%s]", app.getApplicationName())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    externalUpdateCount = updateCertMetadata(token, clmApp, externalCertList, SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH);
                }

                if (internalUpdateCount == noOfInternalCertsToUpdate && externalUpdateCount == noOfExternalCertsToUpdate) {
                    updateCount++;
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateCertMetadataForModifiedApps").
                            put(LogMessage.MESSAGE, String.format("All certificates are updated with latest metadata application [%s]", app.getApplicationName())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    // Update application metadata with latest application details
                    updateApplciationMetadta(token, app, clmApp);
                }
                else {
                    log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateCertMetadataForModifiedApps").
                            put(LogMessage.MESSAGE, String.format("Failed to update application metadata for one or more certificates in the application [%s]", app.getApplicationName())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    // Update application metadata update flag to false on failure
                    app.setUpdateFlag(false);
                    updateApplciationMetadta(token, app, null);
                }
            }
        }
        return updateCount == modifiedApplications.size();
    }

    /**
     * Update application metadata with new application details or update flag
     * @param token
     * @param app
     * @param clmApp
     */
    private void updateApplciationMetadta(String token, TMOAppMetadataDetails app, TMOAppMetadataDetails clmApp) {
        Response response = workloadDetailsService.udpateApplicationMetadata(token, app, clmApp);
        if (response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "updateApplciationMetadta").
                    put(LogMessage.MESSAGE, String.format("Successfully updaetd application metadata updated for [%s]", app.getApplicationName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
        else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "updateApplciationMetadta").
                    put(LogMessage.MESSAGE, String.format("Failed to update application metadata for [%s]", app.getApplicationName())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
    }

    /**
     * Update metadata for list of certificates with new application details.
     * @param token
     * @param clmApp
     * @param certList
     * @param path
     * @return
     */
    private int updateCertMetadata(String token, TMOAppMetadataDetails clmApp, List<String> certList, String path) {
        int successfulUpdateCount = 0;
        // iterate each internal certificate in application to update metadata if required
        if (certList != null) {
            for (String certificateName: certList) {
                boolean isUpdaterequired = false;
                String certPath = path + "/" + certificateName;

                String pathjson = "{\"path\":\""+certPath+"\"}";
                Response metadataResponse = reqProcessor.process("/read",pathjson,token);
                Map<String,Object> _metadataMap = null;
                if(HttpStatus.OK.equals(metadataResponse.getHttpstatus())){
                    ObjectMapper objectMapper = new ObjectMapper();
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateCertMetadata").
                            put(LogMessage.MESSAGE, String.format ("Trying to read metadata for certificate [%s]", certificateName)).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    try {
                        _metadataMap = objectMapper.readValue(metadataResponse.getResponse(), new TypeReference<Map<String,Object>>() {});
                    } catch (IOException e) {
                        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, "updateCertMetadata").
                                put(LogMessage.MESSAGE, String.format ("Error creating _metadataMap for certificate [%s]", certificateName)).
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                    }

                    if (_metadataMap != null) {
                        @SuppressWarnings("unchecked")
                        Map<String,Object> metadataMap = (Map<String,Object>) _metadataMap.get("data");

                        @SuppressWarnings("unchecked")
                        String notificationEmails = (String) metadataMap.get("notificationEmails");

                        @SuppressWarnings("unchecked")
                        String applicationOwnerEmailId = (String) metadataMap.get("applicationOwnerEmailId");

                        @SuppressWarnings("unchecked")
                        String projectLeadEmailId = (String) metadataMap.get("projectLeadEmailId");

                        if (!clmApp.getApplicationOwnerEmailId().equals(applicationOwnerEmailId)) {
                            isUpdaterequired = true;
                            metadataMap.put("applicationOwnerEmailId", clmApp.getApplicationOwnerEmailId());
                            notificationEmails = updateNotificationEmailIds(notificationEmails, clmApp.getApplicationOwnerEmailId());
                        }
                        if (!clmApp.getProjectLeadEmailId().equals(projectLeadEmailId)) {
                            isUpdaterequired = true;
                            metadataMap.put("projectLeadEmailId", clmApp.getProjectLeadEmailId());
                            notificationEmails = updateNotificationEmailIds(notificationEmails, clmApp.getProjectLeadEmailId());
                        }

                        String metadataJson = "";
                        if (isUpdaterequired) {
                            if (!StringUtils.isEmpty(notificationEmails)) {
                                metadataMap.put("notificationEmails", notificationEmails);
                            }
                            try {
                                metadataJson = objectMapper.writeValueAsString(metadataMap);
                            } catch (JsonProcessingException e) {
                                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                        put(LogMessage.ACTION, "updateCertMetadata").
                                        put(LogMessage.MESSAGE, String.format ("Error in creating metadataJson for certificate [%s]", certificateName)).
                                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                        build()));
                            }
                            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                    put(LogMessage.ACTION, "updateOutdatedMetadataForCert").
                                    put(LogMessage.MESSAGE, String.format("Trying to update metadata for certificate [%s] in application [%s]", certificateName, clmApp.getApplicationName())).
                                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                    build()));
                            String writeJson =  "{\"path\":\""+certPath+"\",\"data\":"+ metadataJson +"}";
                            Response response = reqProcessor.process("/write",writeJson,token);
                            if (response.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
                                log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                        put(LogMessage.ACTION, "updateOutdatedMetadataForCert").
                                        put(LogMessage.MESSAGE, String.format("Successfully updated cert metadata with updates from CLM for [%s] in application [%s]", certPath, clmApp.getApplicationName())).
                                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                        build()));
                                successfulUpdateCount++;
                            }
                            else {
                                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                        put(LogMessage.ACTION, "updateOutdatedMetadataForCert").
                                        put(LogMessage.MESSAGE, String.format("Failed to update cert metadata with updates from CLM for [%s]", certPath)).
                                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                        build()));
                            }
                        }
                        else {
                            log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                    put(LogMessage.ACTION, "updateOutdatedMetadataForCert").
                                    put(LogMessage.MESSAGE, String.format("Either certificate not found or Certificate metadata update is not required for [%s] for application [%s]", certificateName, clmApp.getApplicationName())).
                                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                    build()));
                            successfulUpdateCount++;
                        }
                    }
                    else {
                        log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                                put(LogMessage.ACTION, "updateOutdatedMetadataForCert").
                                put(LogMessage.MESSAGE, String.format("Failed to parse metadata for [%s] for application [%s]", certificateName, clmApp.getApplicationName())).
                                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                                build()));
                    }
                }
                else {
                    log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                            put(LogMessage.ACTION, "updateOutdatedMetadataForCert").
                            put(LogMessage.MESSAGE, String.format("Either certificate not found or failed to read metadata for [%s] for application [%s]", certificateName, clmApp.getApplicationName())).
                            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                            build()));
                    successfulUpdateCount++;
                }
            }
        }
        return successfulUpdateCount;
    }

    /**
     * To udpate notification email string
     * @param notificationEmails
     * @param email
     * @return
     */
    private String updateNotificationEmailIds(String notificationEmails, String email) {
        if (!StringUtils.isEmpty(notificationEmails)) {
            if (!notificationEmails.contains(email.toLowerCase())) {
                notificationEmails = notificationEmails + "," + email;
            }
        }
        else {
            notificationEmails = email;
        }
        return notificationEmails;
    }

    /**
     * To get the list of app names having outdated metadata.
     * @param tmoAppMetadataListFromCLM
     * @param tmoAppMetadataList
     * @return
     */
    private List<TMOAppMetadataDetails> getModifiedApplicationList(List<TMOAppMetadataDetails> tmoAppMetadataListFromCLM, List<TMOAppMetadataDetails> tmoAppMetadataList) {
        List<TMOAppMetadataDetails> modifiedAppNames = new ArrayList<>();
        for (TMOAppMetadataDetails tmoAppMetadata: tmoAppMetadataList) {
            TMOAppMetadataDetails tmoAppMetadataInCLM = tmoAppMetadataListFromCLM.stream().filter(app ->
                    app.getApplicationName().equals(tmoAppMetadata.getApplicationName())).findFirst().orElse(null);

            // Check if owner email and project lead email is changed
            if (tmoAppMetadataInCLM != null) {
                if (tmoAppMetadataInCLM.getApplicationOwnerEmailId() != null && !tmoAppMetadataInCLM.getApplicationOwnerEmailId().equals(tmoAppMetadata.getApplicationOwnerEmailId())) {
                    modifiedAppNames.add(tmoAppMetadata);
                }
                else if (tmoAppMetadataInCLM.getProjectLeadEmailId() != null && !tmoAppMetadataInCLM.getProjectLeadEmailId().equals(tmoAppMetadata.getProjectLeadEmailId())) {
                    modifiedAppNames.add(tmoAppMetadata);
                }
            }
        }
        log.info(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getModifiedApplicationList").
                put(LogMessage.MESSAGE, String.format("Found [%d] outdated certificate metadata", modifiedAppNames.size())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return modifiedAppNames;
    }
}

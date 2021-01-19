package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.SSLCertificateMetadataDetails;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Component
public class SSLExternalCertificateScheduler {

    @Autowired
    private SSLCertificateService sslCertificateService;

    @Autowired
    private TokenUtils tokenUtils;

    @Value("${vault.port}")
    private String vaultPort;

    @Value("${SSLExternalCertificate.schedule.enabled}")
    private boolean isSSLExtProcessScheduleEnabled;

    @Autowired
    private RequestProcessor reqProcessor;

    private static Logger log = LogManager.getLogger(SSLCertificateService.class);

    private static final String MESSAGES = "{\"messages\":[\"";
    private static final String ERRORS = "{\"errors\":[\"";

    /**
     * This method to set the ThreadLocalContext and return user details
     *
     * @return
     */
    private UserDetails getUserDetailsForScheduler() {
        ThreadLocalContext.getCurrentMap().put(LogMessage.APIURL, "processApprovedExternalCertificates");
        ThreadLocalContext.getCurrentMap().put(LogMessage.USER, "");
        String token = tokenUtils.getSelfServiceTokenWithAppRole();
        UserDetails userDetails = new UserDetails();
        userDetails.setAdmin(false);
        userDetails.setSelfSupportToken(token);
        return userDetails;
    }


    /**
     * This method will start as per cronschedule and process the pending external certificates
     *
     * @throws Exception
     */
     @Scheduled(cron = "${SSLExternalCertificate.process.crontime.value}")
    public void processApprovedExternalCertificates() throws Exception {
        UserDetails userDetails = getUserDetailsForScheduler();
        if (isSSLExtProcessScheduleEnabled) {
            String token = userDetails.getSelfSupportToken();
            Date startDate = new Date();
            printLogMessage(SSLCertificateConstants.PROCESS_APPROVED_CERTIFICATES,
                    String.format("External Certificates Process has been started at =  [%s] ", startDate));

            Response response = getMetadata(token, SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH);
            if (Objects.nonNull(response)) {
                ResponseEntity<String> responseEntity = filterExternalCertsAndProcess(response, token, userDetails);
                if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                    Date endDate = new Date();
                    printLogMessage(SSLCertificateConstants.PROCESS_APPROVED_CERTIFICATES,
                            String.format("External Certificates Process Completed  =  [%s]  and " +
                                            "time taken to complete process = [%s]",
                                    endDate, (endDate.getTime() - startDate.getTime())));

                } else {
                    printLogMessage(SSLCertificateConstants.PROCESS_APPROVED_CERTIFICATES,
                            "External Certificates Process Completed with Errors  ");
                }
            } else {
                printLogMessage(SSLCertificateConstants.PROCESS_APPROVED_CERTIFICATES,
                        "NO External Certificates avaiable  ");
            }
        } else {
            printLogMessage(SSLCertificateConstants.PROCESS_APPROVED_CERTIFICATES, String.format("External " +
                    "certificate process was not enabled"));
        }
    }

    /**
     * This method will be to filter the pending approval certificates and vaidate the same with NCLM
     *
     * @param response
     * @param token
     * @param userDetails
     * @return
     * @throws Exception
     */
    private ResponseEntity<String> filterExternalCertsAndProcess(Response response, String token,
                                                                 UserDetails userDetails) throws Exception {
        printLogMessage("filterExternalCertsAndProcess",
                "Filter the External Certificates from Metadata path process started ");
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
        JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
        //Iterate the certificate list and validate the status
        for (int i = 0; i < jsonArray.size(); i++) {
            String metaDataPath =
                    SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + jsonArray.get(i).getAsString();
            Response extCertMetaData = reqProcessor.process("/read", "{\"path\":\"" + metaDataPath + "\"}", token);
            getPendApprovalCertsAndProcess(extCertMetaData, userDetails);
        }
        return ResponseEntity.status(HttpStatus.OK).body(MESSAGES + "External Certificates Process completed " +
                "Successfully" + "\"]}");
    }


    /**
     * This method will be used to validate the pending certificats with NCLM
     *
     * @param response
     * @param userDetails
     * @return
     * @throws Exception
     */
    private ResponseEntity<String> getPendApprovalCertsAndProcess(Response response, UserDetails userDetails) throws Exception {

        JsonParser jsonParser = new JsonParser();
        JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
        Map<String, String> metaDataParams = new Gson().fromJson(object.toString(), Map.class);
        String certName = metaDataParams.get(SSLCertificateConstants.CERTIFICATE_NAME);
        printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS, String.format("External Certificate" +
                " Process Started for Certificate ==  [%s]  and Metadata = [%s]", certName, metaDataParams));
        try {
            String requestStatus = (!StringUtils.isEmpty(metaDataParams.get(SSLCertificateConstants.REQUEST_STATUS))) ?
                    metaDataParams.get(SSLCertificateConstants.REQUEST_STATUS) : "";

            String certStatus = (!StringUtils.isEmpty(metaDataParams.get(SSLCertificateConstants.CERTIFICATE_STATUS))) ?
                    metaDataParams.get(SSLCertificateConstants.CERTIFICATE_STATUS) : "";

            if ((!StringUtils.isEmpty(requestStatus)) && (requestStatus.
                    equalsIgnoreCase(SSLCertificateConstants.REQUEST_PENDING_APPROVAL) &&
                    (!certStatus.equalsIgnoreCase(StringUtils.capitalize(SSLCertificateConstants.STATUS_REJECTED))))) {

                printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS, String.format("Certificate ==  " +
                        "[%s] ==  is in Pending Approval ", certName));
                int actionId = (int) Float.valueOf(object.get(SSLCertificateConstants.ACTION_ID).getAsString()).floatValue();
                if (actionId > 0) {
                    SSLCertificateMetadataDetails metadataDetails = new SSLCertificateMetadataDetails();
                    metadataDetails.setActionId(actionId);
                    metadataDetails.setCertificateName(certName);
                    String approvalStatus = sslCertificateService.getExternalCertReqStatus(metadataDetails);
                    if ((!StringUtils.isEmpty(approvalStatus)) && (SSLCertificateConstants.APPROVED.equalsIgnoreCase(approvalStatus))) {
                        ResponseEntity<String> responseEntity =
                                sslCertificateService.validateApprovalStatusAndGetCertificateDetails(certName,
                                        SSLCertificateConstants.EXTERNAL, userDetails);
                        return validateApprovalResponse(responseEntity, certName);
                    } else {
                        printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS,
                                String.format(" Certificate ==  [%s] == NOT Approved AND  NCLM  STATUS[%s]  and Metadata " +
                                        "= [%s]", certName, approvalStatus, metaDataParams));
                        updateMetadataWithStatus(metaDataParams, userDetails, approvalStatus);
                        return ResponseEntity.status(HttpStatus.OK).body(MESSAGES + "Certificate is not in Approval Status" +
                                " " + certName + " ==Metadata Details= " + metaDataParams + " NCLM  STATUS =" + approvalStatus + "  \"]}");
                    }
                } else {
                    printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS,
                            String.format(" Certificate ==  [%s] == action id is Zero and metadata =[%s] ", certName,  metaDataParams));
                }
            } else {
                printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS,
                        String.format(" Certificate ==  [%s] == not in pending approval or rejected and Metadata =  " +
                                "= [%s]", certName,  metaDataParams));
            }
            return ResponseEntity.status(HttpStatus.OK).body(MESSAGES + "External Certificate May not be pending approval" +
                    " status to process or Rejected in NCLM " + certName + " ==Metadata = " + metaDataParams + " \"]}");
        } catch (Exception e) {
            printErrorLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS,
                    String.format(" Error while Processing External Certificate ==  [%s]  and Metadata " +
                            "= [%s]", certName, metaDataParams));

            return ResponseEntity.status(HttpStatus.OK).body(MESSAGES + "Exception while processing external " +
                    "certificate status to process or Rejected in NCLM " + certName + " ==Metadata = " + metaDataParams + " \"]}");
        }
    }


    /**
     * Validate the approval Response
     *
     * @param responseEntity
     * @param certName
     * @return
     */
    private ResponseEntity<String> validateApprovalResponse(ResponseEntity responseEntity, String certName) {
        if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS, String.format(" " +
                    "External Process Completed for Certificate = [%s]  ", certName));
            return ResponseEntity.status(HttpStatus.OK).body(MESSAGES + "Successfully updated the details" +
                    " for external certificate" + certName + "\"]}");
        } else {
            printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS,
                    String.format(" External Process Process failed  for Certificate [%s]  ", certName));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERRORS + "Failed to " +
                    "update details for external certificate" + certName + "\"]}");
        }
    }

    /**
     * Update the metadata data details
     *
     * @param metaDataParams
     * @param userDetails
     * @param approvalStatus
     * @throws JsonProcessingException
     */
    private void updateMetadataWithStatus(Map<String, String> metaDataParams, UserDetails userDetails,
                                          String approvalStatus) throws JsonProcessingException {
        String certName = metaDataParams.get(SSLCertificateConstants.CERTIFICATE_NAME);
        String certificatePath = SSLCertificateConstants.SSL_EXTERNAL_CERT_PATH + "/" + certName;
        metaDataParams.put(SSLCertificateConstants.CERTIFICATE_STATUS, StringUtils.capitalize(approvalStatus));
        boolean sslMetaDataUpdateStatus = ControllerUtil.updateMetaDataOnPath(certificatePath,
                metaDataParams, userDetails.getSelfSupportToken());
        if (sslMetaDataUpdateStatus) {
            printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS,
                    String.format(" Metadata Details updated successfully for Certificate ==  [%s]  and details= [%s] ",
                            certName, metaDataParams));
        } else {
            printLogMessage(SSLCertificateConstants.GET_PENDING_APPROVAL_CERT_PROCESS,
                    String.format(" Error While updating Metadata Details  for Certificate == [%s]  ",
                            certName));
        }
    }


    /**
     * Thi method display log message with given formate
     *
     * @param action
     * @param message
     */
    private void printLogMessage(String action, String message) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, action).
                put(LogMessage.MESSAGE, message).build()));
    }

    /**
     * This method  to print the error log message
     *
     * @param action
     * @param message
     */
    private void printErrorLogMessage(String action, String message) {
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, action).
                put(LogMessage.MESSAGE, message).build()));
    }


    /**
     * This method will be used get the metadata
     *
     * @param token
     * @param path
     * @return
     */
    private Response getMetadata(String token, String path) {
        String pathStr = path + "?list=true";
        return reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);
    }

}

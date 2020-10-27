package com.tmobile.cso.vault.api.utils;

import com.tmobile.cso.vault.api.model.CertManagerLogin;
import com.tmobile.cso.vault.api.model.CertificateData;
import com.tmobile.cso.vault.api.model.SSLCertificateRequest;
import com.tmobile.cso.vault.api.process.CertResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

@Component
public class NCLMMockUtil {
    public NCLMMockUtil() {
    }

    /**
     * Get the revocation mock response
     * @return
     */
    public CertResponse getRevocationMockResponse(){
        CertResponse CertResponse = new CertResponse();
        CertResponse.setHttpstatus(HttpStatus.OK);
        CertResponse.setSuccess(Boolean.TRUE);
        return CertResponse;
    }

    //get the certificate during renewal
    public CertificateData  getRenewCertificateMockData(){
        //Assign from and to-dates
        DateTimeFormatter formatter= DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX");
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("-07:00"));
        String fromDate = now.format(formatter);
        int endYear = Integer.parseInt(fromDate.substring(0,4))+1;

        CertificateData certificateData = new CertificateData();
        certificateData.setCertificateId(1234);
        certificateData.setCreateDate(now.format(formatter));
        certificateData.setExpiryDate(endYear +fromDate.substring(4));
        certificateData.setCertificateStatus("Active");
        return certificateData;
    }

    /**
     * Get the revocation mock response
     * @return
     */
    public CertResponse getRenewMockResponse(){
        CertResponse CertResponse = new CertResponse();
        CertResponse.setHttpstatus(HttpStatus.OK);
        CertResponse.setSuccess(Boolean.TRUE);
        return CertResponse;
    }

    /**
     * Get the delete mock response
     * @return
     */
    public CertResponse getDeleteMockResponse(){
        CertResponse CertResponse = new CertResponse();
        CertResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        CertResponse.setSuccess(Boolean.TRUE);
        return CertResponse;
    }


    /**
     * Get the delete mock response
     * @return
     */
    public CertificateData getDeleteCertMockResponse(Map<String, String>  metaDataParams){
        CertificateData certificateData = new CertificateData();
        certificateData.setCertificateId(1234);
        certificateData.setExpiryDate(metaDataParams.get("expiryDate"));
        certificateData.setCreateDate(metaDataParams.get("createDate"));
        return certificateData;
    }


    /**
     * Return login mock details
     * @return
     */
    public CertManagerLogin getMockLoginDetails(){
        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setAccess_token("dummytoken");
        certManagerLogin.setToken_type("Bearer");
        return certManagerLogin;
    }

    /**
     * Get Enroll mock Response
     * @return
     */
    public CertResponse getEnrollMockResponse(){
        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setSuccess(Boolean.TRUE);
        return certResponse;
    }

    /**
     * This return mock certificate data
     * @param sslCertificateRequest
     * @return
     */
    public CertificateData getMockCertificateData(SSLCertificateRequest sslCertificateRequest){

        //Assign from and to-dates
        DateTimeFormatter formatter= DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX");
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("-07:00"));
        String fromDate = now.format(formatter);
        int endYear = Integer.parseInt(fromDate.substring(0,4))+1;

        CertificateData certificateData = new CertificateData();
        certificateData.setCertificateName(sslCertificateRequest.getCertificateName());
        certificateData.setCertificateId(11);
        certificateData.setCertificateStatus("Active");
        certificateData.setCreateDate(now.format(formatter));
        certificateData.setExpiryDate(endYear +fromDate.substring(4));
        certificateData.setContainerName("T-Vault-Test");
        certificateData.setAuthority("T-Mobile Issuing CA 01 - SHA2");
        certificateData.setDnsNames(Arrays.asList(sslCertificateRequest.getDnsList()));
        return certificateData;
    }


    /**
     * Get the Revocation Reasons
     * @return
     */
    public CertResponse getMockRevocationReasons() {
        CertResponse certResponse = new CertResponse();
        String revocationReason = "{\"time_enabled\":false,\"details_enabled\":false," +
                "\"reasons\":[{\"reason\":\"unspecified\",\"displayName\":\"Unspecified\"},{\"reason\":\"keyCompromise\",\"displayName\":\"Key compromise\"},{\"reason\":\"cACompromise\",\"displayName\":\"CA compromise\"},{\"reason\":\"affiliationChanged\",\"displayName\":\"Affiliation changed\"},{\"reason\":\"superseded\",\"displayName\":\"Superseded\"},{\"reason\":\"cessationOfOperation\",\"displayName\":\"Cessation of operation\"},{\"reason\":\"certificateHold\",\"displayName\":\"Certificate hold\"}]}";
        certResponse.setSuccess(Boolean.TRUE);
        certResponse.setResponse(revocationReason);
        certResponse.setHttpstatus(HttpStatus.OK);
        return certResponse;
    }


    public CertificateData getMockDataForRevoked(){
        CertificateData certificateData = new CertificateData();
        certificateData.setCertificateId(11);
        certificateData.setCertificateStatus("Revoked");
        return certificateData;
    }

}

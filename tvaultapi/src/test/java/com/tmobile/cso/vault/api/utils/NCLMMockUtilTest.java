package com.tmobile.cso.vault.api.utils;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.controller.OIDCUtil;
import com.tmobile.cso.vault.api.model.CertManagerLogin;
import com.tmobile.cso.vault.api.model.CertificateData;
import com.tmobile.cso.vault.api.model.SSLCertificateRequest;
import com.tmobile.cso.vault.api.process.CertResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.ComponentScan;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class, EntityUtils.class, HttpClientBuilder.class, OIDCUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class NCLMMockUtilTest {

    @InjectMocks
    NCLMMockUtil nclmMockUtil;

    @Test
    public void getRevocationMockResponse_Test() {
        CertResponse certResponse = nclmMockUtil.getRevocationMockResponse();
        assertNotNull(certResponse);
    }

    @Test
    public void getRenewCertificateMockData_Test() {
        CertificateData certificateData = nclmMockUtil.getRenewCertificateMockData();
        assertNotNull(certificateData);
    }

    @Test
    public void getRenewMockResponse_Test() {
        CertResponse certResponse = nclmMockUtil.getRenewMockResponse();
        assertNotNull(certResponse);
    }

    @Test
    public void getDeleteMockResponse_Test() {
        CertResponse certResponse = nclmMockUtil.getDeleteMockResponse();
        assertNotNull(certResponse);
    }

    @Test
    public void getDeleteCertMockResponse_Test() {
        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("expiryDate", "1");
        currentMap.put("createDate", "2");
        CertificateData certificateData1 = nclmMockUtil.getDeleteCertMockResponse(currentMap);
        assertNotNull(certificateData1);
    }

    @Test
    public void getMockLoginDetails_Test() {
        CertManagerLogin certManagerLogin = nclmMockUtil.getMockLoginDetails();
        assertNotNull(certManagerLogin);
    }

    @Test
    public void getEnrollMockResponse_Test() {
        CertResponse certResponse = nclmMockUtil.getEnrollMockResponse();
        assertNotNull(certResponse);
    }

    @Test
    public void getMockCertificateData_Test() {
        SSLCertificateRequest sslCertificateRequest = new SSLCertificateRequest();
        sslCertificateRequest.setCertificateName("test1");
        sslCertificateRequest.setDnsList(new String[] { "A", "B", "C", "D" });
        CertificateData certificateData = nclmMockUtil.getMockCertificateData(sslCertificateRequest);
        assertNotNull(certificateData);
    }

    @Test
    public void getMockRevocationReasons_Test() {
        CertResponse certResponse = nclmMockUtil.getMockRevocationReasons();
        assertNotNull(certResponse);
    }

    @Test
    public void getMockDataForRevoked_Test() {
        CertificateData certificateData = nclmMockUtil.getMockDataForRevoked();
        assertNotNull(certificateData);
    }
}

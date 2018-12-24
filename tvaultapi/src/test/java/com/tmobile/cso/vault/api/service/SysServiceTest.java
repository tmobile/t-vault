package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.Unseal;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SysServiceTest {

    @InjectMocks
    SysService sysService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }

    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (expectedBody!="") {
            response.setResponse(expectedBody);
        }
        return response;
    }

    @Test
    public void test_checkVaultHealth_successfully_https() {

        Response response = getMockResponse(HttpStatus.OK, true, "{  \"messages\": [    \"Healthy.All OK\"  ]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Healthy.All OK\"]}");
        when( reqProcessor.process("/health","{}","")).thenReturn(response);

        ResponseEntity<String> responseEntity = sysService.checkVaultHealth();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_checkVaultHealth_successfully_http() {

        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response response = getMockResponse(HttpStatus.OK, true, "{  \"messages\": [    \"Healthy.All OK\"  ]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Healthy.All OK\"]}");
        when( reqProcessor.process("/health","{}","")).thenReturn(responseNotFound);

        when(reqProcessor.process("/v2/health","{}","")).thenReturn(response);
        ResponseEntity<String> responseEntity = sysService.checkVaultHealth();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_checkVaultHealth_failure() {

        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"messages\":[\"Not OK \"]}");
        when( reqProcessor.process("/health","{}","")).thenReturn(responseNotFound);

        when(reqProcessor.process("/v2/health","{}","")).thenReturn(responseNotFound);
        ResponseEntity<String> responseEntity = sysService.checkVaultHealth();
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_unseal_successfully_https() {

        String jsonStr = "{  \"serverip\": \"localhost\",  \"port\": 8200,  \"key\": \"LwRw+j0I7L4ff4no50fduaF5OeTZJEOQFwdjWZcee0s=\",  \"reset\": true}";
        String jsonStr1 = "{  \"serverip\": \"localhost\",  \"port\": 8200,  \"key\": \"LwRw+j0I7L4ff4no50fduaF5OeTZJEOQFwdjWZcee0s=\",  \"reset\": true,\"port\":\"null\"}";
        Unseal unseal = new Unseal("localhost", "8200", "LwRw+j0I7L4ff4no50fduaF5OeTZJEOQFwdjWZcee0s=", true);
        Response response = getMockResponse(HttpStatus.OK, true, "{\"messages\": [ \"vault is unsealed\" ]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\": [ \"vault is unsealed\" ]}");

        when(JSONUtil.getJSON(unseal)).thenReturn(jsonStr);
        when(reqProcessor.process("/unseal",jsonStr1,"")).thenReturn(response);

        ResponseEntity<String> responseEntity = sysService.unseal(unseal);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_unseal_successfully_http() {

        String jsonStr = "{  \"serverip\": \"localhost\",  \"port\": 8200,  \"key\": \"LwRw+j0I7L4ff4no50fduaF5OeTZJEOQFwdjWZcee0s=\",  \"reset\": true}";
        String jsonStr1 = "{  \"serverip\": \"localhost\",  \"port\": 8200,  \"key\": \"LwRw+j0I7L4ff4no50fduaF5OeTZJEOQFwdjWZcee0s=\",  \"reset\": true,\"port\":\"null\"}";
        Unseal unseal = new Unseal("localhost", "8200", "LwRw+j0I7L4ff4no50fduaF5OeTZJEOQFwdjWZcee0s=", true);
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\": [ \"vault is unsealed\" ]}");
        Response responseOk = getMockResponse(HttpStatus.OK, true, "{\"messages\": [ \"vault is unsealed\" ]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\": [ \"vault is unsealed\" ]}");

        when(JSONUtil.getJSON(unseal)).thenReturn(jsonStr);
        when(reqProcessor.process("/unseal",jsonStr1,"")).thenReturn(response);
        when(reqProcessor.process("/v2/unseal",jsonStr1,"")).thenReturn(responseOk);
        ResponseEntity<String> responseEntity = sysService.unseal(unseal);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_unsealProgress_successfully_https() {
        Response response = getMockResponse(HttpStatus.OK, true, "{  \"sealed\": false,  \"progress\": 0}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{  \"sealed\": false,  \"progress\": 0}");

        when(reqProcessor.process("/unseal-progress","{\"serverip\":\"localhost\",\"port\":\"null\"}","")).thenReturn(response);

        ResponseEntity<String> responseEntity = sysService.unsealProgress("localhost");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_unsealProgress_successfully_http() {
        Response responseOk = getMockResponse(HttpStatus.OK, true, "{  \"sealed\": false,  \"progress\": 0}");
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\": [ \"vault is unsealed\" ]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{  \"sealed\": false,  \"progress\": 0}");

        when(reqProcessor.process("/unseal-progress","{\"serverip\":\"localhost\",\"port\":\"null\"}","")).thenReturn(response);
        when(reqProcessor.process("/v2/unseal-progress","{\"serverip\":\"localhost\",\"port\":\"null\"}","")).thenReturn(responseOk);

        ResponseEntity<String> responseEntity = sysService.unsealProgress("localhost");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_checkVaultSealStatus_successfully_https() {

        Response response = getMockResponse(HttpStatus.OK, true, "{\"type\": \"shamir\",\"initialized\": true,\"sealed\": false,\"progress\": 0,\"version\": \"0.11.3\",\"migration\": null,\"cluster_name\": \"vault-cluster-9f404bc2\", \"cluster_id\": \"a26bf9f9-84e2-de1a-a4d3-cffec0ddf10b\", \"recovery_seal\": false}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":{\"type\": \"shamir\",\"initialized\": true,\"sealed\": false,\"progress\": 0,\"version\": \"0.11.3\",\"migration\": null,\"cluster_name\": \"vault-cluster-9f404bc2\", \"cluster_id\": \"a26bf9f9-84e2-de1a-a4d3-cffec0ddf10b\", \"recovery_seal\": false}}");
        when( reqProcessor.process("/seal-status","{}","")).thenReturn(response);

        ResponseEntity<String> responseEntity = sysService.checkVaultSealStatus();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_checkVaultSealStatus_successfully_http() {

        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        Response response = getMockResponse(HttpStatus.OK, true, "{\"type\": \"shamir\",\"initialized\": true,\"sealed\": false,\"progress\": 0,\"version\": \"0.11.3\",\"migration\": null,\"cluster_name\": \"vault-cluster-9f404bc2\", \"cluster_id\": \"a26bf9f9-84e2-de1a-a4d3-cffec0ddf10b\", \"recovery_seal\": false}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":{\"type\": \"shamir\",\"initialized\": true,\"sealed\": false,\"progress\": 0,\"version\": \"0.11.3\",\"migration\": null,\"cluster_name\": \"vault-cluster-9f404bc2\", \"cluster_id\": \"a26bf9f9-84e2-de1a-a4d3-cffec0ddf10b\", \"recovery_seal\": false}}");
        when( reqProcessor.process("/seal-status","{}","")).thenReturn(responseNotFound);

        when(reqProcessor.process("/v2/seal-status","{}","")).thenReturn(response);
        ResponseEntity<String> responseEntity = sysService.checkVaultSealStatus();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_checkVaultSealStatus_failure() {

        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"Error\":[\"Unable to get Seal-Status information\"]}");
        when( reqProcessor.process("/seal-status","{}","")).thenReturn(responseNotFound);

        when(reqProcessor.process("/v2/seal-status","{}","")).thenReturn(responseNotFound);
        ResponseEntity<String> responseEntity = sysService.checkVaultSealStatus();
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
}

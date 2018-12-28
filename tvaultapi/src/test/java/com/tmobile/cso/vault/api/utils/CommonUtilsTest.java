package com.tmobile.cso.vault.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.ComponentScan;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"javax.management.*"})
public class CommonUtilsTest {

    @InjectMocks
    CommonUtils commonUtils;

	@Test
    public void test_getPoliciesAsArray() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = "{\"id\":\"1JRRXfKHYuN2zbl1gkRM12zA\",\"last_renewal_time\":null,\"renewable\":false,\"policies\":[\"approle_normal_user\",\"default\"],\"creation_ttl\":1800000,\"username\":\"normaluser\"}";
        String[] expectedPolicies = {"approle_normal_user", "default"};
        String[] policies = commonUtils.getPoliciesAsArray(objMapper, jsonStr);

    }

    @Test
    public void test_getPoliciesAsArray_string() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = "{\"id\":\"1JRRXfKHYuN2zbl1gkRM12zA\",\"last_renewal_time\":null,\"renewable\":false,\"policies\":\"approle_normal_user\",\"creation_ttl\":1800000,\"username\":\"normaluser\"}";
        String[] expectedPolicies = {"approle_normal_user"};
        String[] policies = commonUtils.getPoliciesAsArray(objMapper, jsonStr);

    }

}

// =========================================================================
// Copyright 2019 T-Mobile, US
//
// Licensed under the Apache License, Version 2.0 (the "License");
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
        String jsonStr = "{\"id\":\"1JRRXfKHYuN2zbl1gkRM12zA\",\"last_renewal_time\":null,\"renewable\":false,\"policies\":[\"normal_user\",\"default\"], \"identity_policies\":[\"default2\"],\"creation_ttl\":1800000,\"username\":\"normaluser\"}";
        String[] expectedPolicies = {"approle_normal_user", "default"};
        String[] policies = commonUtils.getPoliciesAsArray(objMapper, jsonStr);

    }

    @Test
    public void test_getPoliciesAsArray_string() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = "{\"id\":\"1JRRXfKHYuN2zbl1gkRM12zA\",\"last_renewal_time\":null,\"renewable\":false,\"policies\":\"approle_normal_user\", \"identity_policies\":[\"default2\"], \"creation_ttl\":1800000,\"username\":\"normaluser\"}";
        String[] expectedPolicies = {"approle_normal_user"};
        String[] policies = commonUtils.getPoliciesAsArray(objMapper, jsonStr);

    }

}

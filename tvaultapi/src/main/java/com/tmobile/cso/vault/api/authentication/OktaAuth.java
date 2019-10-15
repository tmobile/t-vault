package com.tmobile.cso.vault.api.authentication;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.springframework.stereotype.Component;

@Component
public class OktaAuth extends VaultAuth {

    /**
     * Okta login
     * @param jsonStr
     * @return
     */
    @Override
    public Response login(String jsonStr) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/okta/login", jsonStr,"");
    }

    /**
     * Read user for okta backend
     * @param jsonStr
     * @param token
     * @return
     */
    @Override
    public Response readUser(String jsonStr, String token) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/okta/users", jsonStr,token);
    }

    /**
     * Configure user with policies for okta auth backend
     * @param userName
     * @param policiesString
     * @param groups
     * @param token
     * @return
     */
    @Override
    public Response configureUser(String userName, String policiesString, String groups, String token) {
        return ControllerUtil.configureOktaUser(userName,policiesString,groups,token);
    }

    /**
     * Read group details
     * @param jsonStr
     * @param token
     * @return
     */
    @Override
    public Response readGroup(String jsonStr, String token) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/okta/groups",jsonStr,token);
    }

    /**
     * Configure group with policies
     * @param groupName
     * @param policiesString
     * @param token
     * @return
     */
    public Response configureGroup(String groupName, String policiesString, String token) {
        return ControllerUtil.configureOktaGroup(groupName, policiesString, token);
    }
}

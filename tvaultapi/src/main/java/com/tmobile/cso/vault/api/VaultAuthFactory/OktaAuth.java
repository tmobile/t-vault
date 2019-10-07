package com.tmobile.cso.vault.api.VaultAuthFactory;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.springframework.stereotype.Component;

@Component
public class OktaAuth extends VaultAuth {

    @Override
    public Response vaultLogin(String jsonStr) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/okta/login", jsonStr,"");
    }

    @Override
    public Response readUser(String jsonStr, String token) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/okta/users", jsonStr,token);
    }

    @Override
    public Response configureUser(String userName, String policiesString, String groups, String token) {
        return ControllerUtil.configureOktaUser(userName,policiesString,groups,token);
    }
}

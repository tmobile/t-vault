package com.tmobile.cso.vault.api.authentication;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.springframework.stereotype.Component;

@Component
public class LdapAuth extends VaultAuth {

    /**
     * Ldap login
     * @param jsonStr
     * @return
     */
    @Override
    public Response login(String jsonStr) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/ldap/login",jsonStr,"");
    }

    /**
     * Read user for ldap backend
     * @param jsonStr
     * @param token
     * @return
     */
    @Override
    public Response readUser(String jsonStr, String token) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/ldap/users", jsonStr, token);
    }

    /**
     * Configure user with policies for ldap auth backend
     * @param userName
     * @param policiesString
     * @param groups
     * @param token
     * @return
     */
    @Override
    public Response configureUser(String userName, String policiesString, String groups, String token) {
        return ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
    }
}

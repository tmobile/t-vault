package com.tmobile.cso.vault.api.authentication;

import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class VaultAuth {

    @Autowired
    private RequestProcessor reqProcessor;

    public RequestProcessor getReqProcessor() {
        return reqProcessor;
    }

    /**
     * Vault login
     * @param jsonStr
     * @return
     */
    public abstract Response login(String jsonStr);

    /**
     * Read user details
     * @param jsonStr
     * @param token
     * @return
     */
    public abstract Response readUser(String jsonStr, String token);

    /**
     * Configure user with policies
     * @param userName
     * @param policiesString
     * @param groups
     * @param token
     * @return
     */
    public abstract Response configureUser(String userName, String policiesString, String groups, String token);
}

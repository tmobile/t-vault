package com.tmobile.cso.vault.api.authentication;

import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UserPassAuth extends VaultAuth {


    /**
     * Userpass login
     * @param jsonStr
     * @return
     */
    @Override
    public Response login(String jsonStr) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/userpass/login",jsonStr,"");
    }

    /**
     * Read user for userpass auth backend
     * @param jsonStr
     * @param token
     * @return
     */
    @Override
    public Response readUser(String jsonStr, String token) {
        RequestProcessor requestProcessor = getReqProcessor();
        return requestProcessor.process("/auth/userpass/users", jsonStr, token);
    }

    /**
     * Configure user with policies for userpass auth backend
     * @param userName
     * @param policiesString
     * @param groups
     * @param token
     * @return
     */
    @Override
    public Response configureUser(String userName, String policiesString, String groups, String token) {
        return ControllerUtil.configureUserpassUser(userName,policiesString,token);
    }

    /**
     * Read group details
     * @param jsonStr
     * @param token
     * @return
     */
    @Override
    public Response readGroup(String jsonStr, String token) {
        Response response = new Response();
        response.setHttpstatus(HttpStatus.BAD_REQUEST);
        response.setResponse("{\"errors\":[\"This operation is not supported for Userpass authentication.\"]}");
        return response;
    }

    /**
     * Configure group with policies
     * @param groupName
     * @param policiesString
     * @param token
     * @return
     */
    public Response configureGroup(String groupName, String policiesString, String token) {
        Response response = new Response();
        response.setHttpstatus(HttpStatus.BAD_REQUEST);
        response.setResponse("{\"errors\":[\"This operation is not supported for Userpass authentication.\"]}");
        return response;
    }
}

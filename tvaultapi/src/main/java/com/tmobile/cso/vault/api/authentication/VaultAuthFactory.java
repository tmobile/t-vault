package com.tmobile.cso.vault.api.authentication;

import com.tmobile.cso.vault.api.process.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VaultAuthFactory {

    @Value("${vault.auth.method}")
    private String vaultAuthMethod;

    @Autowired
    LdapAuth ldapAuth;

    @Autowired
    UserPassAuth userPassAuth;

    @Autowired
    OktaAuth oktaAuth;

    /**
     * Get vault auth method
     * @return
     */
    private VaultAuth getAuth() {
        VaultAuth vaultAuth;
        switch (vaultAuthMethod) {
            case "ldap":
                vaultAuth = ldapAuth;
                break;
            case "okta":
                vaultAuth = oktaAuth;
                break;
            default:
                vaultAuth = userPassAuth;
                break;
        }
        return vaultAuth;
    }

    /**
     * Perform login for all auth backend
     * @param jsonStr
     * @return
     */
    public Response login(String jsonStr) {
        return getAuth().login(jsonStr);
    }

    /**
     * Read user details
     * @param userName
     * @param token
     * @return
     */
    public Response readUser(String userName, String token) {
        String jsonStr = "{\"username\":\""+userName+"\"}";
        return getAuth().readUser(jsonStr, token);
    }

    /**
     * Configure user with policies
     * @param userName
     * @param policiesString
     * @param groups
     * @param token
     * @return
     */
    public Response configureUser(String userName, String policiesString, String groups, String token) {
        return getAuth().configureUser(userName,policiesString,groups,token);
    }

    /**
     * Read group details
     * @param groupName
     * @param token
     * @return
     */
    public Response readGroup(String groupName, String token) {
        String jsonStr = "{\"groupname\":\""+groupName+"\"}";
        return getAuth().readGroup(jsonStr, token);
    }

    public Response configureGroup(String groupName, String policiesString, String token) {
        return getAuth().configureGroup(groupName, policiesString, token);
    }
}

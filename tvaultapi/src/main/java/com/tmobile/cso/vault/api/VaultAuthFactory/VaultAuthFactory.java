package com.tmobile.cso.vault.api.VaultAuthFactory;

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

    public Response login(String jsonStr) {
        return getAuth().vaultLogin(jsonStr);
    }

    public Response readUser(String userName, String token) {
        String jsonStr = "{\"username\":\""+userName+"\"}";
        return getAuth().readUser(jsonStr, token);
    }

    public Response configureUser(String userName, String policiesString, String groups, String token) {
        return getAuth().configureUser(userName,policiesString,groups,token);
    }
}

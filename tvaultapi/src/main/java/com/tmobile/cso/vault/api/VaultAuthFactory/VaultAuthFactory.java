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

    public Response login(String jsonStr) {
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
        return vaultAuth.vaultLogin(jsonStr);
    }
}

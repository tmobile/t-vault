package com.tmobile.cso.vault.api.VaultAuthFactory;

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

    public abstract Response vaultLogin(String jsonStr);
}

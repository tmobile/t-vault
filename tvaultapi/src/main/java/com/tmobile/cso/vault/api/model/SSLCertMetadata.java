package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class SSLCertMetadata  implements Serializable {

    private String path;

    @JsonProperty("data")
    private SSLCertificateMetadataDetails sslCertificateMetadataDetails;

    public SSLCertMetadata(String path, SSLCertificateMetadataDetails sslMetadataDetails) {
        super();
        this.path = path;
        this.sslCertificateMetadataDetails = sslMetadataDetails;
    }

}

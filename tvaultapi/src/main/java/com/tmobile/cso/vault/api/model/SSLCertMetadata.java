package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class SSLCertMetadata  implements Serializable {

    private String path;

    @JsonProperty("data")
    private SSLCertificateMetadataDetails sslCertificateMetadataDetails;
    public SSLCertMetadata() {
        super();
    }
    public SSLCertMetadata(String path, SSLCertificateMetadataDetails sslMetadataDetails) {
        super();
        this.path = path;
        this.sslCertificateMetadataDetails = sslMetadataDetails;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public SSLCertificateMetadataDetails getSslCertificateMetadataDetails() {
        return sslCertificateMetadataDetails;
    }

    public void setSslCertificateMetadataDetails(SSLCertificateMetadataDetails sslCertificateMetadataDetails) {
        this.sslCertificateMetadataDetails = sslCertificateMetadataDetails;
    }
}

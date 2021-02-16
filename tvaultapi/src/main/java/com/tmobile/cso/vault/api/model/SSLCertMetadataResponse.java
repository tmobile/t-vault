package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class SSLCertMetadataResponse implements Serializable {


    private static final long serialVersionUID = -3967795703312263611L;

    @JsonProperty("data")
    private SSLCertificateMetadataDetails sslCertificateMetadataDetails;

    public SSLCertMetadataResponse() {
        super();
    }

    public SSLCertMetadataResponse(SSLCertificateMetadataDetails sslCertificateMetadataDetails) {
        this.sslCertificateMetadataDetails = sslCertificateMetadataDetails;
    }

    public SSLCertificateMetadataDetails getSslCertificateMetadataDetails() {
        return sslCertificateMetadataDetails;
    }

    public void setSslCertificateMetadataDetails(SSLCertificateMetadataDetails sslCertificateMetadataDetails) {
        this.sslCertificateMetadataDetails = sslCertificateMetadataDetails;
    }

    @Override
    public String toString() {
        return "SSLCertMetadataResponse{" +
                "sslCertificateMetadataDetails=" + sslCertificateMetadataDetails +
                '}';
    }
}

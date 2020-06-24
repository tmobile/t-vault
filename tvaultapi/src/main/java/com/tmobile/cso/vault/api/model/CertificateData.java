package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateData {

    private int certificateId;
    private String certificateStatus;
    private String expiryDate;
    private String createDate;
    private String containerName;
    private String certificateName;
    private String authority;
    public String getAuthority() {
        return authority;
    }
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public int getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(int certificateId) {
        this.certificateId = certificateId;
    }

    public String getCertificateStatus() {
        return certificateStatus;
    }

    public void setCertificateStatus(String certificateStatus) {
        this.certificateStatus = certificateStatus;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }

    @Override
    public String toString() {
        return "CertificateData{" +
                "certificateId=" + certificateId +
                ", certificateStatus='" + certificateStatus + '\'' +
                ", expiryData='" + expiryDate + '\'' +
                ", createDate='" + createDate + '\'' +
                ", containerName='" + containerName + '\'' +
                ", certificateName='" + certificateName + '\'' +
                '}';
    }
}

package com.tmobile.cso.vault.api.model;

public class CertificateData {

    private int certificateId;
    private String certificateStatus;
    private String expiryData;
    private String createDate;
    private String containerName;
    private String certificateName;

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

    public String getExpiryData() {
        return expiryData;
    }

    public void setExpiryData(String expiryData) {
        this.expiryData = expiryData;
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
                ", expiryData='" + expiryData + '\'' +
                ", createDate='" + createDate + '\'' +
                ", containerName='" + containerName + '\'' +
                ", certificateName='" + certificateName + '\'' +
                '}';
    }
}

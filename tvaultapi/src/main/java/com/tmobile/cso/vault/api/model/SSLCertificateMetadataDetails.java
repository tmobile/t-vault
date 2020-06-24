package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SSLCertificateMetadataDetails implements Serializable {

    private int certificateId;
    private String certificateStatus;
    private String expiryDate;
    private String createDate;
    private String containerName;
    private String certificateName;
    private String authority;
    private String applicationTag;
    private String applicationName;
    private String projectLeadEmailId;
    private String applicationOwnerEmailId;
    private String akmid;
    private String certType;
    private String certCreatedBy;
    private String certOwnerEmailId;
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
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getApplicationTag() {
        return applicationTag;
    }
    public void setApplicationTag(String applicationTag) {
        this.applicationTag = applicationTag;
    }
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getProjectLeadEmailId() {
        return projectLeadEmailId;
    }

    public void setProjectLeadEmailId(String projectLeadEmailId) {
        this.projectLeadEmailId = projectLeadEmailId;
    }

    public String getApplicationOwnerEmailId() {
        return applicationOwnerEmailId;
    }

    public void setApplicationOwnerEmailId(String applicationOwnerEmailId) {
        this.applicationOwnerEmailId = applicationOwnerEmailId;
    }

    public String getAkmid() {
        return akmid;
    }
    public void setAkmid(String akmid) {
        this.akmid = akmid;
    }
    public String getCertType() {
        return certType;
    }

    public void setCertType(String certType) {
        this.certType = certType;
    }

    public String getCertCreatedBy() {
        return certCreatedBy;
    }
    public void setCertCreatedBy(String certCreatedBy) {
        this.certCreatedBy = certCreatedBy;
    }
    public String getCertOwnerEmailId() {
        return certOwnerEmailId;
    }
    public void setCertOwnerEmailId(String certOwnerEmailId) {
        this.certOwnerEmailId = certOwnerEmailId;
    }
}

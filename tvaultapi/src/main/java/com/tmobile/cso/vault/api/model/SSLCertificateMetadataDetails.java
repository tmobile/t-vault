package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
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
    private String certOwnerNtid;
    private int containerId;
    private String requestStatus;
    private List<String> dnsNames;
    private int actionId;
    private String notificationEmails;
    private boolean onboardFlag;
    private String keyUsageValue;

    public String getKeyUsageValue() {
        return keyUsageValue;
    }

    public void setKeyUsageValue(String keyUsageValue) {
        this.keyUsageValue = keyUsageValue;
    }

    public boolean isOnboardFlag() {
        return onboardFlag;
    }
    
    public void setOnboardFlag(boolean onboardFlag) {
        this.onboardFlag = onboardFlag;
    }
	public String getNotificationEmails() {
		return notificationEmails;
	}
	public void setNotificationEmails(String notificationEmails) {
		this.notificationEmails = notificationEmails;
	}
	public int getActionId() {
        return actionId;
    }
    public void setActionId(int actionId) {
        this.actionId = actionId;
    }

    public List<String> getDnsNames() {
        return dnsNames;
    }

    public void setDnsNames(List<String> dnsNames) {
        this.dnsNames = dnsNames;
    }

    public String getRequestStatus() {
        return requestStatus;
    }
    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    public String getCertOwnerNtid() {
        return certOwnerNtid;
    }

    public void setCertOwnerNtid(String certOwnerNtid) {
        this.certOwnerNtid = certOwnerNtid;
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

    @Override
	public String toString() {
		return "SSLCertificateMetadataDetails [certificateId=" + certificateId + ", certificateStatus="
				+ certificateStatus + ", expiryDate=" + expiryDate + ", createDate=" + createDate + ", containerName="
				+ containerName + ", certificateName=" + certificateName + ", authority=" + authority
				+ ", applicationTag=" + applicationTag + ", applicationName=" + applicationName
				+ ", projectLeadEmailId=" + projectLeadEmailId + ", applicationOwnerEmailId=" + applicationOwnerEmailId
				+ ", akmid=" + akmid + ", certType=" + certType + ", certCreatedBy=" + certCreatedBy
				+ ", certOwnerEmailId=" + certOwnerEmailId + ", certOwnerNtid=" + certOwnerNtid + ", containerId="
				+ containerId + ", requestStatus=" + requestStatus + ", dnsNames=" + dnsNames + ", actionId=" + actionId
				+ ", notificationEmail=" + notificationEmails + "]";
	}
}

package com.tmobile.cso.vault.api.common;

/**
 * SSLCertificate Constants
 */
public final class SSLCertificateConstants {
	
	private SSLCertificateConstants() {}

    public static final String ACCESS_TOKEN = "access_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String CERTIFICATES="certificates";
    public static final String ACTIVE="ACTIVE";
    public static final String CERTIFICATE_STATUS="certificateStatus";
    public static final String TARGETSYSTEM_SERVICES="targetsystemservices";
    public static final String NAME="name";
    public static final String TARGETSYSTEM_SERVICE_ID="targetSystemServiceId";
    public static final String TARGETSYSTEMS="targetSystems";
    public static final String TARGETSYSTEM_ID="targetSystemID";
    public static final String SUBJECT="subject";
    public static final String TYPENAME="typeName";
    public static final String CN="cn";
    public static final String VALUE="value";
    public static final String ITEMS="items";
    public static final String SELECTED_ID="selectedId";
    public static final String SSL_CERT_PATH = "metadata/sslcerts";
    public static final String SSL_CREATE_EXCEPTION="SSL Certificate creation failed";
    public static final String SSL_CERT_SUCCESS="Certificate Created Successfully In NCLM";
    public static final String CUSTOMER_LOGIN="CertManager Login";
    public static final String ACCESS_ID="accessid";
    public static final String ADDRESS="address";
    public static final String DESCRIPTION="description";
    public static final String HOSTNAME="hostname";
    public static final String MONITORINGENABLED="monitoringEnabled";
    public static final String MULTIIPMONITORINGENABLED="multiIpMonitoringEnabled";
    public static final String PORT="port";
    public static final String SSL_CERTFICATE_REASONS_FAILED="SSL Get Certificate reasons failed";
    public static final String SSL_CERTFICATE_WRITE_PERMISSION="write";
    public static final String SSL_CERT_PATH_VALUE = "sslcerts/";
    public static final String ERROR_INVALID_ACCESS_POLICY_MSG = "Invalid access policy";
    public static final String ADD_USER_TO_CERT_MSG = "Add User to Certificate";
    public static final String SSL_OWNER_PERMISSION_EXCEPTION="Add sudo permission to certificate owner failed";
    public static final String ADD_APPROLE_TO_CERT_MSG = "Add Approle to Certificate";
    public static final String OWNER_PERMISSION_CERTIFICATE = "o_cert_";
    public static final String CERT_DOWNLOAD_TYPE_PKCS12DERR = "pkcs12der";
    public static final String CERT_DOWNLOAD_TYPE_PEMBUNDLE = "pembundle";
    public static final String CERT_DOWNLOAD_TYPE_PKCS12PEM = "pkcs12pem";
    public static final String REMOVE_USER_FROM_CERT_MSG = "Remove User from Certificate";
    public static final String REMOVE_GROUP_FROM_CERT_MSG = "Remove Group from Certificate";
}

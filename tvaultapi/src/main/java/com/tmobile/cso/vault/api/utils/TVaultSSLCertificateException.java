package com.tmobile.cso.vault.api.utils;

import org.springframework.http.HttpStatus;

public class TVaultSSLCertificateException extends Throwable {

    private HttpStatus errorCode;
    private String errorMessage;

    public TVaultSSLCertificateException() {
        super();
    }


    public TVaultSSLCertificateException(HttpStatus errorCodeStatus, String errorMessageDetails) {
        this.errorCode = errorCodeStatus;
        this.errorMessage = errorMessageDetails;
    }


    public HttpStatus getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(HttpStatus errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "TVaultSSLCertificateException{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

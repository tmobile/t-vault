package com.tmobile.cso.vault.api.utils;

import org.springframework.http.HttpStatus;

public class GenericRestException extends RuntimeException {

    private HttpStatus errorCode;
    private String errorMessage;

    public GenericRestException() {
        super();
    }


    public GenericRestException(String message) {
        super(message);
    }

    public GenericRestException(HttpStatus mbcErrorCode, String mbcErrorMessage) {
        super(mbcErrorCode + "," + mbcErrorMessage);
        this.errorCode = mbcErrorCode;
        this.errorMessage = mbcErrorMessage;
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
        return "GenericRestException{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

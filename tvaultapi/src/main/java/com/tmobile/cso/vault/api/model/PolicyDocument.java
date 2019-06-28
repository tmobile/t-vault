package com.tmobile.cso.vault.api.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

public class PolicyDocument implements Serializable {
    private static final long serialVersionUID = 8449432373729941477L;
    private String version;
    private Statement statement;

    public PolicyDocument() {
    }

    public PolicyDocument(String version, Statement policyStatement) {
        this.version = version;
        this.statement = policyStatement;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement policyStatement) {
        this.statement = policyStatement;
    }

    @Override
    public String toString() {
        return "{\"Version\":\"" + version + "\"" +
                ", \"Statement\":[" + statement +
                "]}";
    }
}

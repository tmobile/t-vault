package com.tmobile.cso.vault.api.model;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

public class Statement implements Serializable {
    private static final long serialVersionUID = 6128378805676143280L;
    private String effect;
    private String[] action;
    private String[] resource;

    public Statement() {
    }

    public Statement(String effect, String[] action, String[] resource) {
        this.effect = effect;
        this.action = action;
        this.resource = resource;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public String[] getAction() {
        return action;
    }

    public void setAction(String[] action) {
        this.action = action;
    }

    public String[] getResource() {
        return resource;
    }

    public void setResource(String[] resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        return "{\"Effect\":\"" + effect + "\"" +
                ", \"Action\":" + "[\"" + StringUtils.join(action,"\",\"") + "\"]" +
                ", \"Resource\":" + "[\"" + StringUtils.join(resource,"\",\"") + "\"]" +
                "}";
    }
}

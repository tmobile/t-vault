package com.tmobile.cso.vault.api.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

public class AWSDynamicRoleRequest implements Serializable {
    private static final long serialVersionUID = -755730651319640417L;
    private String name;
    private String permisisons;
    private String resources;


    public AWSDynamicRoleRequest() {
    }

    public AWSDynamicRoleRequest(String name, String permisisons, String resources) {
        this.name = name;
        this.permisisons = permisisons;
        this.resources = resources;
    }

    @ApiModelProperty(example="role1", position=8)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @ApiModelProperty(example="s3:CreateBucket,s3:ListAllMyBuckets,s3:DeleteBucket", position=8)
    public String getPermisisons() {
        return permisisons;
    }

    public void setPermisisons(String permisisons) {
        this.permisisons = permisisons;
    }
    @ApiModelProperty(example="arn:aws:s3:::vault*,arn:aws:s3:::tvault*", position=8)
    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }
}

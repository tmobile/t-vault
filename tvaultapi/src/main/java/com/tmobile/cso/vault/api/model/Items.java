package com.tmobile.cso.vault.api.model;

import java.util.ArrayList;
import java.util.List;

public class Items {
    private String typeName;
    private DenyMore denyMore;
    private Required required;
    private boolean removable;


    private List<Value> value = new ArrayList<>();

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public DenyMore getDenyMore() {
        return denyMore;
    }

    public void setDenyMore(DenyMore denyMore) {
        this.denyMore = denyMore;
    }

    public Required getRequired() {
        return required;
    }

    public void setRequired(Required required) {
        this.required = required;
    }

    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public List<Value> getValue() {
        return value;
    }

    public void setValue(List<Value> value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Items{" +
                "typeName='" + typeName + '\'' +
                ", denyMore=" + denyMore +
                ", required=" + required +
                ", removable=" + removable +
                ", value=" + value +
                '}';
    }
}

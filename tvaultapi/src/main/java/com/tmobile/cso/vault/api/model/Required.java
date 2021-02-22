package com.tmobile.cso.vault.api.model;

public class Required {
    private boolean value;
    private boolean disabled;
    private Owner owner;

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Required{" +
                "value='" + value + '\'' +
                ", disabled=" + disabled +
                ", owner=" + owner +
                '}';
    }
}

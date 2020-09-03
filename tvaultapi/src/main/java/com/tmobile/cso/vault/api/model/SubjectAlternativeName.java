package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SubjectAlternativeName {
    @JsonProperty("items")
    private List<Items> items = new ArrayList<>();

    public List<Items> getItems() {
        return items;
    }

    public void setItems(List<Items> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "subjectAlternativeName{" +
                "items=" + items +
                '}';
    }
}

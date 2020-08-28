package com.tmobile.cso.vault.api.model;

public class ApproveRequest {
    private boolean finalize;
    private String note;

    public boolean isFinalize() {
        return finalize;
    }

    public void setFinalize(boolean finalize) {
        this.finalize = finalize;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

package com.peopleapp.enums;

public enum ReferralStatus {

    PENDING("PENDING"),
    COMPLETED("COMPLETED");

    private String value;

    ReferralStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}

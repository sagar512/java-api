package com.peopleapp.enums;

public enum ContactPriority {

    PRIORITY_LABEL_MOBILE ("PL.00.00"),
    PRIORITY_LABEL_IPHONE ("PL.00.03"),
    PRIORITY_LABEL_MAIN ("PL.00.04"),
    PRIORITY_LABEL_HOME  ("PL.00.01"),
    PRIORITY_LABEL_WORK  ("PL.00.02");

    private String value;

    private ContactPriority(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

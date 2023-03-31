package com.peopleapp.enums;

public enum APIPrivacyType {

    PRIVATE("PRIVATE"),
    TEMP_PRIVATE("TEMP_PRIVATE"),
    PUBLIC("PUBLIC");

    private String value;

    APIPrivacyType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

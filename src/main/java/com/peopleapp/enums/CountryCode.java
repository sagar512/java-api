package com.peopleapp.enums;

public enum CountryCode {

    US_CANADA_COUNTRY_CODE("+1");
    private String value;

    CountryCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}

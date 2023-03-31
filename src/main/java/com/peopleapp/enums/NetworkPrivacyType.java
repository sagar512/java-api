package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum NetworkPrivacyType {

    DIRECT_JOIN("OPEN"),
    JOIN_BY_REQUEST("PUBLIC"),
    PRIVATE("PRIVATE");

    private static final Map<String, NetworkPrivacyType> CATEGORY_MAP = new HashMap<>();

    static {
        for (NetworkPrivacyType myEnum : values()) {
            CATEGORY_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private NetworkPrivacyType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NetworkPrivacyType getByValue(String value) {
        return CATEGORY_MAP.get(value);
    }

    @Override
    public String toString() {
        return value;
    }

}

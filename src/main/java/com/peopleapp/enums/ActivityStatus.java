package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum ActivityStatus {

    PENDING("PENDING"),
    EXPIRED("EXPIRED"),
    INACTIVE("INACTIVE"),
    ACTIVE("ACTIVE"),
    INFORMATIVE("INFORMATIVE"),
    NA("NA");

    private static final Map<String, ActivityStatus> ACTIVITY_STATUS_MAP = new HashMap<>();

    static {
        for (ActivityStatus myEnum : values()) {
            ACTIVITY_STATUS_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private ActivityStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActivityStatus getByValue(String value) {
        return ACTIVITY_STATUS_MAP.get(value);
    }
}

package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum UserStatus {

    ACTIVE("ACTIVE"),
    DEACTIVATED("DEACTIVATED"),
    DELETED("DELETED");

    private static final Map<String, UserStatus> USER_STATUS_MAP = new HashMap<>();

    static {
        for (UserStatus myEnum : values()) {
            USER_STATUS_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserStatus getByValue(String value) {
        return USER_STATUS_MAP.get(value);
    }

}

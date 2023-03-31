package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum NetworkStatus {

    ACTIVE("ACTIVE"),
    DELETED("DELETED");

    private static final Map<String, NetworkStatus> NETWORK_STATUS = new HashMap<>();

    static {
        for (NetworkStatus myEnum : values()) {
            NETWORK_STATUS.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private NetworkStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NetworkStatus getByValue(String value) {
        return NETWORK_STATUS.get(value);
    }

}

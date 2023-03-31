package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum ConnectionStatus {

    CONNECTED("CONNECTED"),
    NOT_CONNECTED("NOT_CONNECTED"),
    PENDING("PENDING"),
    DELETED("DELETED");

    private static final Map<String, ConnectionStatus> CONNECTION_STATUS_MAP = new HashMap<>();

    static {
        for (ConnectionStatus myEnum : values()) {
            CONNECTION_STATUS_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private ConnectionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConnectionStatus getByValue(String value) {
        return CONNECTION_STATUS_MAP.get(value);
    }

}

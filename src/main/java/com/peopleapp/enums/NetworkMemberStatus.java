package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum NetworkMemberStatus {

    ACTIVE("ACTIVE"),
    REMOVED("REMOVED"),
    UNSUBSCRIBED("UNSUBSCRIBED"),
    DELETED("DELETED");

    private static final Map<String, NetworkMemberStatus> NETWORK_MEMBER_STATUS_MAP = new HashMap<>();

    static {
        for (NetworkMemberStatus myEnum : values()) {
            NETWORK_MEMBER_STATUS_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private NetworkMemberStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NetworkMemberStatus getByValue(String value) {
        return NETWORK_MEMBER_STATUS_MAP.get(value);
    }

}

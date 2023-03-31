package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum RequestType {

    CONNECTION_REQUEST("CONNECTION_REQUEST"),
    MORE_INFO_REQUEST("MORE_INFO_REQUEST"),
    INTRODUCTION_REQUEST("INTRODUCTION_REQUEST"),
    NETWORK_JOIN_REQUEST("NETWORK_JOIN_REQUEST"),
    NETWORK_ADMIN_PROMOTION("NETWORK_ADMIN_PROMOTION"),
    NETWORK_OWNERSHIP_TRANSFER("NETWORK_OWNERSHIP_TRANSFER"),
    NETWORK_SHARE("NETWORK_SHARE"),
    NETWORK_MEMBER_INVITE("NETWORK_MEMBER_INVITE"),
    SHARE_CONTACT_ACTIVITY("SHARE_CONTACT_ACTIVITY"),
    SHARE_LOCATION_ACTIVITY("SHARE_LOCATION_ACTIVITY"),
    NETWORK_MESSAGE_BROADCAST("NETWORK_MESSAGE_BROADCAST"),
    NETWORK_JOIN_REQUEST_ACCEPTED("NETWORK_JOIN_REQUEST_ACCEPTED"),
    UPDATE_CONTACT_ACTIVITY("UPDATE_CONTACT_ACTIVITY"),
    REMOVE_NETWORK_MEMBER("REMOVE_NETWORK_MEMBER"),
    STOP_LOCATION_SHARE("STOP_LOCATION_SHARE"),
    CONNECTION_REQUEST_ACCEPTED("CONNECTION_REQUEST_ACCEPTED");


    private static final Map<String, RequestType> REQUEST_TYPE_MAP = new HashMap<>();

    static {
        for (RequestType myEnum : values()) {
            REQUEST_TYPE_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private RequestType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RequestType getByValue(String value) {
        return REQUEST_TYPE_MAP.get(value);
    }

}

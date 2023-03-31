package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum NetworkCommunicationMode {

    CONTACT_NUMBER("PhoneNumber"),
    EMAIL_ADDRESS("Email"),
    SOCIAL_PROFILE("SocialProfile");

    private static final Map<String, NetworkCommunicationMode> CATEGORY_MAP = new HashMap<>();

    static {
        for (NetworkCommunicationMode myEnum : values()) {
            CATEGORY_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private NetworkCommunicationMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NetworkCommunicationMode getByValue(String value) {
        return CATEGORY_MAP.get(value);
    }

    @Override
    public String toString() {
        return value;
    }


}

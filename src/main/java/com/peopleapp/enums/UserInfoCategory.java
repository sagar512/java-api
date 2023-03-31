package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum UserInfoCategory {

    CONTACT_NUMBER("PhoneNumber"),
    EMAIL_ADDRESS("Email"),
    SOCIAL_PROFILE("SocialProfile"),
    ADDRESS("Address"),
    DATE("Event"),
    WEBSITE("Website"),
    RELATED_NAME("Relationship"),
    OTHER("Other"),
    INSTANT_MESSAGING("InstantMessaging");

    private static final Map<String, UserInfoCategory> CATEGORY_MAP = new HashMap<>();

    static {
        for (UserInfoCategory myEnum : values()) {
            CATEGORY_MAP.put(myEnum.getValue().toUpperCase(), myEnum);
        }
    }

    private String value;

    private UserInfoCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value.toUpperCase();
    }

    public static UserInfoCategory getByValue(String value) {
        return CATEGORY_MAP.get(value.toUpperCase());
    }

    @Override
    public String toString() {
        return value;
    }

}

package com.peopleapp.enums;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public enum UserInformationVerification {

    VERIFIED("VERIFIED"),
    NOT_VERIFIED("NOT_VERIFIED"),
    PENDING("PENDING"),
    NOT_REQUIRED("NA");

    private static final Map<String, UserInformationVerification> VERIFICATION_MAP = new HashMap<>();

    static {
        for (UserInformationVerification myEnum : values()) {
            VERIFICATION_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private UserInformationVerification(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserInformationVerification getByValue(String value) {
        return VERIFICATION_MAP.get(value);
    }


}

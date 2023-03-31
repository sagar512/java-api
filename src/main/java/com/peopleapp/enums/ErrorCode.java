package com.peopleapp.enums;

import java.util.HashMap;
import java.util.Map;

public enum ErrorCode {

    BAD_REQUEST("400"),
    UNAUTHORIZED("401");

    private static final Map<String, ErrorCode> ERROR_CODE_MAP = new HashMap<>();

    static {
        for (ErrorCode myEnum : values()) {
            ERROR_CODE_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private ErrorCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ErrorCode getByValue(String value) {
        return ERROR_CODE_MAP.get(value);
    }

}

package com.peopleapp.enums;


import java.util.HashMap;
import java.util.Map;

public enum Action {

    INITIATED("INITIATED"),
    IGNORED("IGNORED"),
    REJECTED("REJECTED"),
    ACCEPTED("ACCEPTED"),
    CANCELLED("CANCELLED"),
    PROMOTED("PROMOTED"),
    TRANSFERRED("TRANSFERRED"),
    DELETED("DELETED");

    private static final Map<String, Action> ACTION_MAP = new HashMap<>();

    static {
        for (Action myEnum : values()) {
            ACTION_MAP.put(myEnum.getValue(), myEnum);
        }
    }

    private String value;

    private Action(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Action getByValue(String value) {
        return ACTION_MAP.get(value);
    }

}

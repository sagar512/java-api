package com.peopleapp.enums;

public enum NetworkMemberRole {

    OWNER("Owner"),
    ADMIN("Admin"),
    MEMBER("Member"),
    GUEST("Guest");

    NetworkMemberRole(String value) {
        this.value = value;
    }

    private String value;

    public String getValue(){
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}

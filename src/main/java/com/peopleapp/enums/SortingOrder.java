package com.peopleapp.enums;

public enum SortingOrder {

    ASCENDING_DEFAULT(0),
    ASCENDING_ORDER(1),
    DESCENDING_ORDER(-1);

    private Integer value;

    SortingOrder(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}

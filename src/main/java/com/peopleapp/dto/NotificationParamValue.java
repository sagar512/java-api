package com.peopleapp.dto;

import lombok.Data;

@Data
public class NotificationParamValue {

    private String collection;

    private String uniqueKey;

    private String field;
}

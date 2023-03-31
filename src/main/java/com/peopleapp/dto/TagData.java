package com.peopleapp.dto;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Transient;

@Data
public class TagData {

    @Transient
    private String tagName;

    private Boolean isProfileTag;

    private DateTime createdOn;

    private DateTime lastUpdatedOn;

}

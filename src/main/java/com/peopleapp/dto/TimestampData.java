package com.peopleapp.dto;

import lombok.Data;
import org.joda.time.DateTime;

@Data
public class TimestampData {

    private String value;

    private DateTime lastUpdatedOn;

}

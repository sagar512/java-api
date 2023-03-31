package com.peopleapp.dto;

import lombok.Data;
import org.joda.time.DateTime;

@Data
public class SharedLocationWithOtherDetails {

    private String activityId;

    private String sharedWithUserId;

    private int sharedForTimeInMinutes;

    private DateTime locationSharedOn;

    private DateTime currentTime;
}

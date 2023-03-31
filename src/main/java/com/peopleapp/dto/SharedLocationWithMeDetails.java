package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.joda.time.DateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharedLocationWithMeDetails {

    private String activityId;

    private String sharedByUserId;

    private Coordinates userLocation;

    private int sharedForTimeInMinutes;

    private DateTime locationSharedOn;

    private DateTime currentTime;

}

package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.joda.time.DateTime;

import javax.validation.constraints.NotEmpty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityDetails {

    @NotEmpty
    private String activityId;

    private String activityById;

    private String activityForId;

    private String requestId;

    @NotEmpty
    private ActivityType activityType;

    private InitiateDetails initiateDetails;

    private String activityMessage;

    private String activityStatus;

    private Boolean isActivityRead;

    private Boolean isCleared ;

    private String activityDescription;

    private String networkId;

    private String connectionId;

    private DateTime createdOn;

    private DateTime lastUpdatedOn;

}

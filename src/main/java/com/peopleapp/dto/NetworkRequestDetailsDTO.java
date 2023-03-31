package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.model.UserActivity;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkRequestDetailsDTO {

    private String networkId;

    private List<UserActivity> requestDetailsList;

    private List<ActivityDetails> activityDetails;
}

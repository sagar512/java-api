package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.peopleapp.dto.ActivityDetails;
import com.peopleapp.dto.UserContactData;
import lombok.Data;

import java.util.List;

@Data
public class SendConnectionRequestResponse {

    private List<ActivityDetails> activityDetailsList;

    private UserContactData contactDetails;

    @JsonIgnore
    private String message;

}

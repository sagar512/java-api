package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserActivityDataNew {

    // Activity details
    private ActivityDetails activityDetails;

    // Only applicable for 'share-contact' and 'introduction'
    private Integer numberOfRecords;

    // Activity read status. default false and will
    // marked true once activity details calls happen with
    // particular activity id.
    private Boolean isRead;

    // Initiator detail
    private UserContactData initiatorDetail;

    // Optional - Will be present if number of contact involved
    // in 'share-contact' or 'introduction' is only one
    private UserContactData contactInvolvedInActivity;

    private String message;

}

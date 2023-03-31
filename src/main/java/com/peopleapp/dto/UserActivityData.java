package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserActivityData {

    private ActivityDetails activityDetails;

    private Integer numberOfContactShared;

    private Integer numberOfRecords;

    // It will be used for preparing initiator information
    // in "/activities" API
    private UserContactData initiatorDetails;

    // It will be used for preparing receiver information
    // in "/sent-request" API
    private UserContactData receiverDetails;

    /* public data of initiator */
    private UserInformationDTO publicProfileData;

    private UserInformationDTO introducedUserDetail;

    private List<SharedContactDetails> sharedContactDetailsList;

    private SharedLocationWithMeDetails sharedLocationDetails;

}

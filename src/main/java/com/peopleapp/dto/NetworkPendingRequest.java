package com.peopleapp.dto;

import lombok.Data;

@Data
public class NetworkPendingRequest {

    private ActivityDetails pendingRequestDetails;

    private UserInformationDTO publicProfileData;

}

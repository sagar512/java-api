package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyOTPResponseDTO {

    private String userId;

    private Boolean isExistingUser;

    private String sessionToken;

    private Boolean isPhoneNumberVerified;

    private Boolean isPushNotificationEnabled = Boolean.FALSE;

    private String referralCode;

    private String bluetoothToken;
    
    private VerifyAllDetailsResponse verifyAllDetails;

}
package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class VerifyOTPRequestDTO {

    @NotEmpty
    private String otp;

    private String referralCode;

    private String bluetoothToken;
    
    private String deviceToken;
}

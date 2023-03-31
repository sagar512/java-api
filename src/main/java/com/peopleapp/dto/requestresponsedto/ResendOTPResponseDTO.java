package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResendOTPResponseDTO {

    private Boolean isPhoneNumberVerified;

    private String tempToken;

}
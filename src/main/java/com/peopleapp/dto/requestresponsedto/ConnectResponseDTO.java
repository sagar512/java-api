package com.peopleapp.dto.requestresponsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectResponseDTO {

    private String tempToken;

    private Boolean isExistingUser = Boolean.TRUE;

    private Boolean isPhoneNumberVerified;
}

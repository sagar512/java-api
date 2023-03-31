package com.peopleapp.dto.requestresponsedto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.dto.SharedProfileInformationData;
import com.peopleapp.dto.UserInformationDTO;

import lombok.Data;


import javax.validation.constraints.NotNull;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InviteByNumberRequest {

    @NotNull
    private UserInformationDTO inviteeContactInformation;

    private SharedProfileInformationData sharedPrivacyProfileKey;

}

package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharedContactDetails {

    private String userId;

    private String sharedContactId;

    private UserInformationDTO staticProfileData;

    private UserInformationDTO sharedProfileData;
}

package com.peopleapp.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class ContactStaticData {

    @NotEmpty
    private String connectionId;

    @NotNull
    private UserInformationDTO staticProfileData;
}

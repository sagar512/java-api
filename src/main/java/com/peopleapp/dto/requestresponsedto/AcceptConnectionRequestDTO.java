package com.peopleapp.dto.requestresponsedto;

import com.peopleapp.dto.SharedProfileInformationData;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class AcceptConnectionRequestDTO {

    @NotEmpty
    private String activityId;

    @Valid
    @NotNull
    private SharedProfileInformationData sharedPrivacyProfileKey;
}

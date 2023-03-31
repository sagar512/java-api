package com.peopleapp.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class AcceptIntroductionActivity {

    @NotNull
    private String activityId;

    @Valid
    @NotNull
    private ContactNumberDTO contactNumber;
}

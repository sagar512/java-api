package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class SetDefaultProfileRequestDTO {

    @NotEmpty
    private String profileId;
}
